/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.devp2p.v5

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.ExpiringMap
import org.apache.tuweni.concurrent.coroutines.asyncCompletion
import org.apache.tuweni.concurrent.coroutines.asyncResult
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.encrypt.SessionKey
import org.apache.tuweni.devp2p.v5.topic.TopicTable
import org.apache.tuweni.io.Base64URLSafe
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * A creator of discovery service objects.
 */
object DiscoveryService {

  /**
   * Creates a new discovery service, generating the node ENR and configuring the UDP connector.
   * @param keyPair the key pair identifying the node running the service.
   * @param bindAddress the address to bind the node to.
   * @param enrSeq the sequence of the ENR of the node
   * @param bootstrapENRList the list of other nodes to connect to on bootstrap.
   * @param enrStorage the permanent storage of ENRs. Defaults to an in-memory store.
   * @param coroutineContext the coroutine context associated with the store.
   */
  @JvmStatic
  @JvmOverloads
  fun open(
    keyPair: SECP256K1.KeyPair,
    localPort: Int,
    bindAddress: InetSocketAddress = InetSocketAddress(InetAddress.getLoopbackAddress(), localPort),
    enrSeq: Long = Instant.now().toEpochMilli(),
    bootstrapENRList: List<String> = emptyList(),
    enrStorage: ENRStorage = DefaultENRStorage(),
    coroutineContext: CoroutineContext = Dispatchers.Default
  ): DiscoveryV5Service {
    val selfENR = EthereumNodeRecord.create(
      keyPair,
      enrSeq,
      emptyMap(),
      emptyMap(),
      bindAddress.address,
      bindAddress.port, // TODO allow override
      bindAddress.port
    )
    // val connector = UdpConnector(bindAddress, keyPair, selfENR, enrStorage)
    return DefaultDiscoveryV5Service(
      bindAddress,
      bootstrapENRList,
      enrStorage,
      keyPair,
      selfENR,
      coroutineContext = coroutineContext
    )
  }
}

/**
 * Service executes network discovery, according to discv5 specification
 * (https://github.com/ethereum/devp2p/blob/master/discv5/discv5.md)
 */
interface DiscoveryV5Service : CoroutineScope {

  /**
   * Starts the node discovery service.
   */
  suspend fun start(): AsyncCompletion

  /**
   * Stops the node discovery service.
   */
  suspend fun terminate()

  /**
   * Starts the discovery service, providing a handle to the completion of the start operation.
   */
  fun startAsync() = asyncCompletion { start() }

  /**
   * Stops the node discovery service, providing a handle to the completion of the shutdown operation.
   */
  fun terminateAsync() = asyncCompletion { terminate() }

  /**
   * Provides the ENR identifying the service.
   */
  fun enr(): EthereumNodeRecord

  /**
   * Adds a peer to the routing table.
   *
   * @param rlpENR the RLP representation of the peer ENR.
   */
  suspend fun addPeer(rlpENR: Bytes): AsyncCompletion {
    val enr: EthereumNodeRecord = EthereumNodeRecord.fromRLP(rlpENR)
    return addPeer(enr)
  }

  /**
   * Adds a peer to the routing table.
   *
   * @param enr the peer Ethereum Node Record
   * @param address optionally, the UDP address to call for this peer.
   */
  suspend fun addPeer(
    enr: EthereumNodeRecord,
    address: InetSocketAddress = InetSocketAddress(enr.ip(), enr.udp()!!)
  ): AsyncCompletion

  /**
   * Requests nodes from all connected peers.
   */
  suspend fun requestNodes(distance: Int = 1): AsyncResult<List<EthereumNodeRecord>>
}

internal class DefaultDiscoveryV5Service(
  private val bindAddress: InetSocketAddress,
  private val bootstrapENRList: List<String>,
  private val enrStorage: ENRStorage,
  private val keyPair: SECP256K1.KeyPair,
  private val selfEnr: EthereumNodeRecord,
  private val routingTable: RoutingTable = RoutingTable(selfEnr),
  private val topicTable: TopicTable = TopicTable(),
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : DiscoveryV5Service {

  companion object {

    private val logger = LoggerFactory.getLogger(DefaultDiscoveryV5Service::class.java)
  }

  private val channel = CoroutineDatagramChannel.open()
  private val handshakes = ExpiringMap<InetSocketAddress, HandshakeSession>()
  private val sessions = ConcurrentHashMap<InetSocketAddress, Session>()
  private val started = AtomicBoolean(false)
  private val nodeId = EthereumNodeRecord.nodeId(keyPair.publicKey())
  private val whoAreYouHeader = Hash.sha2_256(Bytes.concatenate(nodeId, Bytes.wrap("WHOAREYOU".toByteArray())))

  private lateinit var receiveJob: Job

  @ObsoleteCoroutinesApi
  override suspend fun start(): AsyncCompletion {
    channel.bind(bindAddress)

    receiveJob = launch { receiveDatagram() }
    return bootstrap()
  }

  override suspend fun terminate() {
    if (started.compareAndSet(true, false)) {
      receiveJob.cancel()
      channel.close()
    }
  }

  override fun enr(): EthereumNodeRecord = selfEnr

  override suspend fun addPeer(enr: EthereumNodeRecord, address: InetSocketAddress): AsyncCompletion {
    val session = sessions[address]
    if (session == null) {
      logger.trace("Creating new session for peer {}", enr)
      val handshakeSession = handshakes.computeIfAbsent(address) { addr -> createHandshake(addr, enr.publicKey(), enr) }
      return asyncCompletion {
        logger.trace("Handshake connection start {}", enr)
        handshakeSession.connect().await()
        logger.trace("Handshake connection done {}", enr)
      }
    } else {
      logger.trace("Session found for peer {}", enr)
      return AsyncCompletion.completed()
    }
  }

  private fun send(addr: InetSocketAddress, message: Bytes) {
    launch {
      val buffer = ByteBuffer.allocate(message.size())
      buffer.put(message.toArrayUnsafe())
      buffer.flip()
      channel.send(buffer, addr)
    }
  }

  private suspend fun bootstrap(): AsyncCompletion = AsyncCompletion.allOf(bootstrapENRList.map {
    logger.trace("Connecting to bootstrap peer {}", it)
    var encodedEnr = it
    if (it.startsWith("enr:")) {
      encodedEnr = it.substringAfter("enr:")
    }
    val rlpENR = Base64URLSafe.decode(encodedEnr)
    addPeer(rlpENR)
  })

  private suspend fun receiveDatagram() {
    while (channel.isOpen) {
      val datagram = ByteBuffer.allocate(Message.MAX_UDP_MESSAGE_SIZE)
      val address = channel.receive(datagram) as InetSocketAddress

      datagram.flip()

      var session = sessions.get(address)
      try {
        val message = Bytes.wrapByteBuffer(datagram)
        if (message.slice(0, 32) == whoAreYouHeader && session != null) {
          sessions.remove(address)
          session = null
        }
        if (session == null) {
          val handshakeSession = handshakes.computeIfAbsent(address) { createHandshake(it) }
          handshakeSession.processMessage(message)
        } else {
          session.processMessage(Bytes.wrapByteBuffer(datagram))
        }
      } catch (e: ClosedChannelException) {
        break
      }
    }
  }

  private fun createHandshake(
    address: InetSocketAddress,
    publicKey: SECP256K1.PublicKey? = null,
    receivedEnr: EthereumNodeRecord? = null
  ): HandshakeSession {
    logger.trace("Creating new handshake with {}", address)
    val newSession = HandshakeSession(keyPair, address, publicKey, this::send, this::enr, coroutineContext)
    newSession.awaitConnection().thenAccept {
      val peerEnr = receivedEnr ?: newSession.receivedEnr!!
      logger.trace("Handshake connection done {}", peerEnr)
      val session = createSession(newSession, address, it, peerEnr)
      newSession.requestId?.let { requestId ->
        session.activeFindNodes[requestId] = AsyncResult.incomplete()
      }
    }.exceptionally { logger.error("Error during connection", it) }
    return newSession
  }

  private fun createSession(
    newSession: HandshakeSession,
    address: InetSocketAddress,
    sessionKey: SessionKey,
    receivedEnr: EthereumNodeRecord
  ): Session {
    val session = Session(
      keyPair,
      newSession.nodeId,
      newSession.tag(),
      sessionKey,
      address,
      this::send,
      this::enr,
      routingTable,
      topicTable,
      { missedPings ->
        missedPings > 5
      },
      coroutineContext
    )
    logger.trace("Adding ENR discovered by connecting to peer")
    enrStorage.set(receivedEnr)
    sessions[address] = session
    return session
  }

  override suspend fun requestNodes(distance: Int): AsyncResult<List<EthereumNodeRecord>> =
    asyncResult {
      val results = ArrayList<EthereumNodeRecord>()
      logger.debug("Requesting from ${sessions.size} sessions with distance $distance")
      sessions.values.map { session ->
        async {
          try {
            val oneResult = session.sendFindNodes(distance).get(10, TimeUnit.SECONDS)
            logger.debug("Received ${oneResult!!.size} results")
            results.addAll(oneResult)
          } catch (e: Exception) {
            logger.debug("Timeout waiting for nodes")
          }
        }
      }.awaitAll()
      results
    }
}
