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

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.datagram.DatagramPacket
import io.vertx.core.net.SocketAddress
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import org.apache.tuweni.devp2p.Packet
import org.apache.tuweni.devp2p.v5.encrypt.SessionKey
import org.apache.tuweni.devp2p.v5.topic.TopicTable
import org.apache.tuweni.io.Base64URLSafe
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
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
   * @param vertx Vert.x instance
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
    vertx: Vertx,
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
      vertx,
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
    address: SocketAddress = SocketAddress.inetSocketAddress(enr.udp()!!, enr.ip().hostAddress)
  ): AsyncCompletion

  /**
   * Requests nodes from all connected peers.
   *
   * @param distance the distance between the node and the peer. Helps pick a Kademlia bucket.
   * @param maxSecondsToWait number of seconds to wait for a response.
   */
  suspend fun requestNodes(
    distance: Int = 1,
    maxSecondsToWait: Long = 10
  ): AsyncResult<Map<EthereumNodeRecord, List<EthereumNodeRecord>>>
}

internal class DefaultDiscoveryV5Service(
  private val vertx: Vertx,
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

  private val server = vertx.createDatagramSocket()
  private val handshakes = ExpiringMap<SocketAddress, HandshakeSession>()
  private val sessions = ConcurrentHashMap<SocketAddress, Session>()
  private val started = AtomicBoolean(false)
  private val nodeId = EthereumNodeRecord.nodeId(keyPair.publicKey())
  private val whoAreYouHeader = Hash.sha2_256(Bytes.concatenate(nodeId, Bytes.wrap("WHOAREYOU".toByteArray())))

  private lateinit var receiveJob: Job

  override suspend fun start(): AsyncCompletion {
    server.handler(this::receiveDatagram).listen(bindAddress.port, bindAddress.hostString).await()
    return bootstrap()
  }

  override suspend fun terminate() {
    if (started.compareAndSet(true, false)) {
      receiveJob.cancel()
      server.close().await()
    }
  }

  override fun enr(): EthereumNodeRecord = selfEnr

  override suspend fun addPeer(enr: EthereumNodeRecord, address: SocketAddress): AsyncCompletion {
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

  private fun send(addr: SocketAddress, message: Bytes) {
    launch {
      server.send(Buffer.buffer(message.toArrayUnsafe()), addr.port(), addr.host()).await()
    }
  }

  private suspend fun bootstrap(): AsyncCompletion = AsyncCompletion.allOf(
    bootstrapENRList.map {
      logger.trace("Connecting to bootstrap peer {}", it)
      var encodedEnr = it
      if (it.startsWith("enr:")) {
        encodedEnr = it.substringAfter("enr:")
      }
      val rlpENR = Base64URLSafe.decode(encodedEnr)
      addPeer(rlpENR)
    }
  )

  private fun receiveDatagram(packet: DatagramPacket) {
    var session = sessions.get(packet.sender())
    val size = Math.min(Packet.MAX_SIZE, packet.data().length())
    val buffer = ByteBuffer.allocate(size)
    packet.data().byteBuf.readBytes(buffer)
    buffer.flip()
    val message = Bytes.wrapByteBuffer(buffer)
    if (message.slice(0, 32) == whoAreYouHeader && session != null) {
      sessions.remove(packet.sender())
      session = null
    }
    if (session == null) {
      val handshakeSession =
        handshakes.computeIfAbsent(packet.sender()) { createHandshake(it) }
      launch {
        handshakeSession.processMessage(message)
      }
    } else {
      launch {
        session.processMessage(message)
      }
    }
  }

  private fun createHandshake(
    address: SocketAddress,
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
    address: SocketAddress,
    sessionKey: SessionKey,
    receivedEnr: EthereumNodeRecord
  ): Session {
    val session = Session(
      receivedEnr,
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

  override suspend fun requestNodes(
    distance: Int,
    maxSecondsToWait: Long
  ): AsyncResult<Map<EthereumNodeRecord, List<EthereumNodeRecord>>> =
    asyncResult {
      val results = ConcurrentHashMap<EthereumNodeRecord, List<EthereumNodeRecord>>()
      logger.debug("Requesting from ${sessions.size} sessions with distance $distance")
      sessions.values.map { session ->
        async {
          try {
            val oneResult = session.sendFindNodes(distance).get(maxSecondsToWait, TimeUnit.SECONDS)
            logger.debug("Received ${oneResult!!.size} results")
            results.put(session.enr, oneResult)
          } catch (e: Exception) {
            logger.debug("Timeout waiting for nodes")
          }
        }
      }.awaitAll()
      results
    }
}
