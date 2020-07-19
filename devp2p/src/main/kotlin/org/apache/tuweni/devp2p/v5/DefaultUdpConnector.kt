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

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalCause
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.misc.HandshakeInitParameters
import org.apache.tuweni.devp2p.v5.misc.TrackingMessage
import org.apache.tuweni.devp2p.v5.topic.TicketHolder
import org.apache.tuweni.devp2p.v5.topic.TopicRegistrar
import org.apache.tuweni.devp2p.v5.topic.TopicTable
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

internal class DefaultUdpConnector(
  private val bindAddress: InetSocketAddress,
  private val keyPair: SECP256K1.KeyPair,
  private val selfEnr: Bytes,
  private val enrStorage: ENRStorage = DefaultENRStorage(),
  private val receiveChannel: CoroutineDatagramChannel = CoroutineDatagramChannel.open(),
  private val nodesTable: RoutingTable = RoutingTable(selfEnr),
  private val topicTable: TopicTable = TopicTable(),
  private val ticketHolder: TicketHolder = TicketHolder(),
  private val authenticationProvider: AuthenticationProvider = DefaultAuthenticationProvider(
    keyPair,
    nodesTable
  ),
  private val packetCodec: PacketCodec = DefaultPacketCodec(keyPair, nodesTable),
  private val selfNodeRecord: EthereumNodeRecord = EthereumNodeRecord.fromRLP(selfEnr),
  private val messageListeners: MutableList<MessageObserver> = mutableListOf(),
  override val coroutineContext: CoroutineContext = Dispatchers.IO
) : UdpConnector, CoroutineScope {

  companion object {
    private const val LOOKUP_MAX_REQUESTED_NODES: Int = 3
    private const val LOOKUP_REFRESH_RATE: Long = 3000
    private const val PING_TIMEOUT: Long = 500
    private const val REQUEST_TIMEOUT: Long = 1000
    private const val REQUIRED_LOOKUP_NODES: Int = 16
    private const val TABLE_REFRESH_RATE: Long = 1000
  }

  private val randomMessageHandler: MessageHandler<RandomMessage> = RandomMessageHandler()
  private val whoAreYouMessageHandler: MessageHandler<WhoAreYouMessage> =
    WhoAreYouMessageHandler()
  private val findNodeMessageHandler: MessageHandler<FindNodeMessage> =
    FindNodeMessageHandler()
  private val nodesMessageHandler: MessageHandler<NodesMessage> = NodesMessageHandler()
  private val pingMessageHandler: MessageHandler<PingMessage> = PingMessageHandler()
  private val pongMessageHandler: MessageHandler<PongMessage> = PongMessageHandler()
  private val regConfirmationMessageHandler: MessageHandler<RegConfirmationMessage> =
    RegConfirmationMessageHandler()
  private val regTopicMessageHandler: MessageHandler<RegTopicMessage> =
    RegTopicMessageHandler()
  private val ticketMessageHandler: MessageHandler<TicketMessage> = TicketMessageHandler()
  private val topicQueryMessageHandler: MessageHandler<TopicQueryMessage> =
    TopicQueryMessageHandler()
  private val topicRegistrar = TopicRegistrar(coroutineContext, this)
  private val askedNodes: MutableList<Bytes> = mutableListOf()

  private val pendingMessages: Cache<String, TrackingMessage> = CacheBuilder.newBuilder()
    .expireAfterWrite(Duration.ofMillis(REQUEST_TIMEOUT))
    .build()
  private val pings: Cache<String, Bytes> = CacheBuilder.newBuilder()
    .expireAfterWrite(Duration.ofMillis(REQUEST_TIMEOUT + PING_TIMEOUT))
    .removalListener<String, Bytes> {
      if (RemovalCause.EXPIRED == it.cause) {
        getNodesTable().evict(it.value)
      }
    }.build()

  private lateinit var refreshJob: Job
  private lateinit var receiveJob: Job
  private lateinit var lookupJob: Job

  private val started = AtomicBoolean(false)

  override fun started(): Boolean = started.get()

  override fun getEnrBytes(): Bytes = selfEnr

  override fun getEnr(): EthereumNodeRecord = selfNodeRecord

  override fun getNodeRecords(): ENRStorage = enrStorage

  override fun getNodesTable(): RoutingTable = nodesTable

  override fun getNodeKeyPair(): SECP256K1.KeyPair = keyPair

  override fun getPendingMessage(authTag: Bytes): TrackingMessage? = pendingMessages.getIfPresent(authTag.toHexString())

  @ObsoleteCoroutinesApi
  override suspend fun start() {
    if (started.compareAndSet(false, true)) {
      receiveChannel.bind(bindAddress)

      receiveJob = launch { receiveDatagram() }
      val lookupTimer = ticker(delayMillis = LOOKUP_REFRESH_RATE, initialDelayMillis = LOOKUP_REFRESH_RATE)
      val refreshTimer = ticker(delayMillis = TABLE_REFRESH_RATE, initialDelayMillis = TABLE_REFRESH_RATE)
      lookupJob = launch {
        for (event in lookupTimer) {
          lookupNodes()
        }
      }
      refreshJob = launch {
        for (event in refreshTimer) {
          refreshNodesTable()
        }
      }
    }
  }

  override suspend fun send(
    address: InetSocketAddress,
    message: UdpMessage,
    destNodeId: Bytes,
    handshakeParams: HandshakeInitParameters?
  ) {
    val encodeResult = packetCodec.encode(message, destNodeId, handshakeParams)
    pendingMessages.put(encodeResult.authTag.toHexString(), TrackingMessage(message, destNodeId))
    receiveChannel.send(ByteBuffer.wrap(encodeResult.content.toArrayUnsafe()), address)
  }

  override suspend fun terminate() {
    if (started.compareAndSet(true, false)) {
      refreshJob.cancel()
      lookupJob.cancel()
      receiveJob.cancel()
      receiveChannel.close()
    }
  }

  override fun attachObserver(observer: MessageObserver) {
    messageListeners.add(observer)
  }

  override fun detachObserver(observer: MessageObserver) {
    messageListeners.remove(observer)
  }

  override fun getAwaitingPongRecord(nodeId: Bytes): Bytes? {
    val nodeIdHex = nodeId.toHexString()
    val result = pings.getIfPresent(nodeIdHex)
    pings.invalidate(nodeIdHex)
    return result
  }

  override fun getSessionInitiatorKey(nodeId: Bytes): Bytes {
    return authenticationProvider.findSessionKey(nodeId.toHexString())?.initiatorKey
      ?: throw IllegalArgumentException("Session key not found.")
  }

  override fun getTopicTable(): TopicTable = topicTable

  override fun getTicketHolder(): TicketHolder = ticketHolder

  override fun getTopicRegistrar(): TopicRegistrar = topicRegistrar

  /**
   * Look up nodes, starting with nearest ones, until we have enough stored.
   */
  private suspend fun lookupNodes() {
    val nearestNodes = getNodesTable().nearest(selfEnr)
    if (REQUIRED_LOOKUP_NODES > nearestNodes.size) {
      lookupInternal(nearestNodes)
    } else {
      askedNodes.clear()
    }
  }

  private suspend fun lookupInternal(nearest: List<Bytes>) {
    val nonAskedNodes = nearest - askedNodes
    val targetNode = if (nonAskedNodes.isNotEmpty()) nonAskedNodes.random() else Bytes.random(32)
    val distance = getNodesTable().distanceToSelf(targetNode)
    for (target in nearest.take(LOOKUP_MAX_REQUESTED_NODES)) {
      val enr = EthereumNodeRecord.fromRLP(target)
      val message = FindNodeMessage(distance = distance)
      val address = InetSocketAddress(enr.ip(), enr.udp())
      send(address, message, Hash.sha2_256(target))
      askedNodes.add(target)
    }
  }

  // Process packets
  private suspend fun receiveDatagram() {
    while (receiveChannel.isOpen) {
      val datagram = ByteBuffer.allocate(UdpMessage.MAX_UDP_MESSAGE_SIZE)
      val address = receiveChannel.receive(datagram) as InetSocketAddress
      datagram.flip()
      try {
        processDatagram(datagram, address)
      } catch (e: ClosedChannelException) {
        break
      }
    }
  }

  private suspend fun processDatagram(datagram: ByteBuffer, address: InetSocketAddress) {
    if (datagram.limit() > UdpMessage.MAX_UDP_MESSAGE_SIZE) {
      return
    }
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
      is RegTopicMessage -> regTopicMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      is RegConfirmationMessage -> regConfirmationMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      is TicketMessage -> ticketMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      is TopicQueryMessage -> topicQueryMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      else -> throw IllegalArgumentException("Unexpected message has been received - ${message::class.java.simpleName}")
    }
    messageListeners.forEach { it.observe(message) }
  }

  // Ping nodes
  private suspend fun refreshNodesTable() {
    if (!getNodesTable().isEmpty()) {
      val enrBytes = getNodesTable().random()
      val nodeId = Hash.sha2_256(enrBytes)
      if (null == pings.getIfPresent(nodeId.toHexString())) {
        val enr = EthereumNodeRecord.fromRLP(enrBytes)
        val address = InetSocketAddress(enr.ip(), enr.udp())
        val message = PingMessage(enrSeq = enr.seq)

        send(address, message, nodeId)
        pings.put(nodeId.toHexString(), enrBytes)
      }
    }
  }
}
