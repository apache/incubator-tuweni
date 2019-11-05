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
package org.apache.tuweni.devp2p.v5.internal

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.ENRStorage
import org.apache.tuweni.devp2p.v5.MessageHandler
import org.apache.tuweni.devp2p.v5.MessageObserver
import org.apache.tuweni.devp2p.v5.PacketCodec
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.storage.RoutingTable
import org.apache.tuweni.devp2p.v5.internal.handler.FindNodeMessageHandler
import org.apache.tuweni.devp2p.v5.internal.handler.NodesMessageHandler
import org.apache.tuweni.devp2p.v5.internal.handler.PingMessageHandler
import org.apache.tuweni.devp2p.v5.internal.handler.PongMessageHandler
import org.apache.tuweni.devp2p.v5.internal.handler.RandomMessageHandler
import org.apache.tuweni.devp2p.v5.internal.handler.WhoAreYouMessageHandler
import org.apache.tuweni.devp2p.v5.packet.FindNodeMessage
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import org.apache.tuweni.devp2p.v5.misc.HandshakeInitParameters
import org.apache.tuweni.devp2p.v5.misc.TrackingMessage
import org.apache.tuweni.devp2p.v5.packet.NodesMessage
import org.apache.tuweni.devp2p.v5.packet.PingMessage
import org.apache.tuweni.devp2p.v5.packet.PongMessage
import org.apache.tuweni.devp2p.v5.storage.DefaultENRStorage
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import org.logl.Logger
import org.logl.LoggerProvider
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.time.Duration
import kotlin.coroutines.CoroutineContext

class DefaultUdpConnector(
  private val bindAddress: InetSocketAddress,
  private val keyPair: SECP256K1.KeyPair,
  private val selfEnr: Bytes,
  private val enrStorage: ENRStorage = DefaultENRStorage(),
  private val receiveChannel: CoroutineDatagramChannel = CoroutineDatagramChannel.open(),
  private val nodesTable: RoutingTable = RoutingTable(selfEnr),
  private val packetCodec: PacketCodec = DefaultPacketCodec(keyPair, nodesTable),
  private val authenticatingPeers: MutableMap<InetSocketAddress, Bytes> = mutableMapOf(),
  private val selfNodeRecord: EthereumNodeRecord = EthereumNodeRecord.fromRLP(selfEnr),
  private val messageListeners: MutableList<MessageObserver> = mutableListOf(),
  override val coroutineContext: CoroutineContext = Dispatchers.IO
) : UdpConnector, CoroutineScope {

  private val log: Logger = LoggerProvider.nullProvider().getLogger(DefaultUdpConnector::class.java)

  private val randomMessageHandler: MessageHandler<RandomMessage> = RandomMessageHandler()
  private val whoAreYouMessageHandler: MessageHandler<WhoAreYouMessage> = WhoAreYouMessageHandler()
  private val findNodeMessageHandler: MessageHandler<FindNodeMessage> = FindNodeMessageHandler()
  private val nodesMessageHandler: MessageHandler<NodesMessage> = NodesMessageHandler()
  private val pingMessageHandler: MessageHandler<PingMessage> = PingMessageHandler()
  private val pongMessageHandler: MessageHandler<PongMessage> = PongMessageHandler()

  private val askedNodes: MutableList<Bytes> = mutableListOf()

  private val pendingMessages: Cache<String, TrackingMessage> = CacheBuilder.newBuilder()
    .expireAfterWrite(Duration.ofMillis(REQUEST_TIMEOUT))
    .build()
  private val pings: Cache<String, Bytes> = CacheBuilder.newBuilder()
    .expireAfterWrite(Duration.ofMillis(REQUEST_TIMEOUT + PING_TIMEOUT))
    .removalListener<String, Bytes> {
      if (it.wasEvicted()) {
        getNodesTable().evict(it.value)
      }
    }.build()

  private lateinit var refreshJob: Job
  private lateinit var receiveJob: Job
  private lateinit var lookupJob: Job

  override fun available(): Boolean = receiveChannel.isOpen

  override fun started(): Boolean = ::receiveJob.isInitialized && available()

  override fun getEnrBytes(): Bytes = selfEnr

  override fun getEnr(): EthereumNodeRecord = selfNodeRecord

  override fun getNodeRecords(): ENRStorage = enrStorage

  override fun getNodesTable(): RoutingTable = nodesTable

  override fun getNodeKeyPair(): SECP256K1.KeyPair = keyPair

  override fun getPendingMessage(authTag: Bytes): TrackingMessage = pendingMessages.getIfPresent(authTag.toHexString())
      ?: throw IllegalArgumentException("Pending message not found")


  override fun start() {
    receiveChannel.bind(bindAddress)

    receiveJob = receiveDatagram()
    lookupJob = lookupNodes()
    refreshJob = refreshNodesTable()
  }

  override fun send(
    address: InetSocketAddress,
    message: UdpMessage,
    destNodeId: Bytes,
    handshakeParams: HandshakeInitParameters?
  ) {
    launch {
      val encodeResult = packetCodec.encode(message, destNodeId, handshakeParams)
      pendingMessages.put(encodeResult.authTag.toHexString(), TrackingMessage(message, destNodeId))
      receiveChannel.send(ByteBuffer.wrap(encodeResult.content.toArray()), address)
    }
  }

  override fun terminate() {
    receiveChannel.close()

    refreshJob.cancel()
    lookupJob.cancel()
    receiveJob.cancel()
  }

  override fun attachObserver(observer: MessageObserver) {
    messageListeners.add(observer)
  }

  override fun detachObserver(observer: MessageObserver) {
    messageListeners.remove(observer)
  }

  override fun addPendingNodeId(address: InetSocketAddress, nodeId: Bytes) {
    authenticatingPeers[address] = nodeId
  }

  override fun findPendingNodeId(address: InetSocketAddress): Bytes {
    val result = authenticatingPeers[address]
      ?: throw IllegalArgumentException("Authenticated peer not found with address ${address.hostName}:${address.port}")
    authenticatingPeers.remove(address)
    return result
  }

  override fun getAwaitingPongRecord(nodeId: Bytes): Bytes? {
    val nodeIdHex = nodeId.toHexString()
    val result = pings.getIfPresent(nodeIdHex)
    pings.invalidate(nodeIdHex)
    return result
  }

  // Lookup nodes
  private fun lookupNodes() = launch {
    while (true) {
      val nearestNodes = getNodesTable().nearest(selfEnr)
      if (REQUIRED_LOOKUP_NODES > nearestNodes.size) {
        lookupInternal(nearestNodes)
      } else {
        askedNodes.clear()
      }
      delay(LOOKUP_REFRESH_RATE)
    }
  }

  private fun lookupInternal(nearest: List<Bytes>) {
    val nonAskedNodes = nearest - askedNodes
    val targetNode = if (nonAskedNodes.isNotEmpty()) nonAskedNodes.random() else Bytes.random(32)
    val distance = getNodesTable().distanceToSelf(targetNode)
    for(target in nearest.take(LOOKUP_MAX_REQUESTED_NODES)) {
      val enr = EthereumNodeRecord.fromRLP(target)
      val message = FindNodeMessage(distance = distance)
      val address = InetSocketAddress(enr.ip(), enr.udp())
      send(address, message, Hash.sha2_256(target))
      askedNodes.add(target)
    }
  }

  // Process packets
  private fun receiveDatagram() = launch {
    val datagram = ByteBuffer.allocate(UdpMessage.MAX_UDP_MESSAGE_SIZE)
    while (receiveChannel.isOpen) {
      datagram.clear()
      val address = receiveChannel.receive(datagram) as InetSocketAddress
      datagram.flip()
      try {
        processDatagram(datagram, address)
      } catch (ex: Exception) {
        log.error(ex.message)
      }
    }
  }

  private fun processDatagram(datagram: ByteBuffer, address: InetSocketAddress) {
    val messageBytes = Bytes.wrapByteBuffer(datagram)
    val decodeResult = packetCodec.decode(messageBytes)
    val message = decodeResult.message
    when (message) {
      is RandomMessage -> randomMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      is WhoAreYouMessage -> whoAreYouMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      is FindNodeMessage -> findNodeMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      is NodesMessage -> nodesMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      is PingMessage -> pingMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      is PongMessage -> pongMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      else -> throw IllegalArgumentException("Unexpected message has been received - ${message::class.java.simpleName}")
    }
    messageListeners.forEach { it.observe(message) }
  }

  // Ping nodes
  private fun refreshNodesTable(): Job = launch {
    while (true) {
      if (!getNodesTable().isEmpty()) {
        val enrBytes = getNodesTable().random()
        val nodeId = Hash.sha2_256(enrBytes)
        val enr = EthereumNodeRecord.fromRLP(enrBytes)
        val address = InetSocketAddress(enr.ip(), enr.udp())
        val message = PingMessage(enrSeq = enr.seq)

        send(address, message, nodeId)
        pings.put(nodeId.toHexString(), enrBytes)
      }
      delay(TABLE_REFRESH_RATE)
    }
  }

  companion object {
    private const val REQUIRED_LOOKUP_NODES: Int = 16
    private const val LOOKUP_MAX_REQUESTED_NODES: Int = 3

    private const val LOOKUP_REFRESH_RATE: Long = 3000
    private const val TABLE_REFRESH_RATE: Long = 1000
    private const val REQUEST_TIMEOUT: Long = 1000
    private const val PING_TIMEOUT: Long = 500
  }
}
