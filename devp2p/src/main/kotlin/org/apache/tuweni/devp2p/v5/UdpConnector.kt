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
import org.apache.tuweni.devp2p.DiscoveryService
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.misc.HandshakeInitParameters
import org.apache.tuweni.devp2p.v5.misc.TrackingMessage
import org.apache.tuweni.devp2p.v5.topic.Ticket
import org.apache.tuweni.devp2p.v5.topic.TicketHolder
import org.apache.tuweni.devp2p.v5.topic.Topic
import org.apache.tuweni.devp2p.v5.topic.TopicRegistrar
import org.apache.tuweni.devp2p.v5.topic.TopicTable
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * Module used for network communication. It accepts and sends incoming messages and also provides peer information,
 * like node's ENR, key pair
 */
internal class UdpConnector(
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
  private val packetCodec: PacketCodec = PacketCodec(keyPair, nodesTable),
  private val selfNodeRecord: EthereumNodeRecord = EthereumNodeRecord.fromRLP(selfEnr),
  private val messageListeners: MutableList<MessageObserver> = mutableListOf(),
  override val coroutineContext: CoroutineContext = Dispatchers.IO
): CoroutineScope {

  companion object {
    private val logger = LoggerFactory.getLogger(UdpConnector::class.java)

    private const val LOOKUP_MAX_REQUESTED_NODES: Int = 3
    private const val LOOKUP_REFRESH_RATE: Long = 3000
    private const val PING_TIMEOUT: Long = 500
    private const val REQUEST_TIMEOUT: Long = 1000
    private const val REQUIRED_LOOKUP_NODES: Int = 16
    private const val TABLE_REFRESH_RATE: Long = 1000
    private const val MAX_NODES_IN_RESPONSE: Int = 4
  }

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

  /**
   * Gives information about connector, whether receive loop is working
   *
   * @return availability information
   */
  fun started(): Boolean = started.get()

  /**
   * Provides node's ENR in RLP encoded representation
   *
   * @return node's RLP encoded ENR
   */
  fun getEnrBytes(): Bytes = selfEnr

  /**
   * Provides node's ENR
   *
   * @return node's ENR
   */
  fun getEnr(): EthereumNodeRecord = selfNodeRecord

  /**
   * Provides enr storage of known nodes
   *
   * @return nodes storage
   */
  fun getNodeRecords(): ENRStorage = enrStorage

  /**
   * Get kademlia routing table
   *
   * @return kademlia table
   */
  fun getNodesTable(): RoutingTable = nodesTable

  /**
   * Retrieve last sent message, in case if it unauthorized and node can resend with authentication header
   *
   * @param authTag message's authentication tag
   *
   * @return message, including node identifier
   */
  fun getPendingMessage(authTag: Bytes): TrackingMessage? = pendingMessages.getIfPresent(authTag.toHexString())

  /**
   * Bootstraps receive loop for incoming message handling
   */
  @ObsoleteCoroutinesApi
  suspend fun start() {
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

  /**
   * Sends udp message by socket address
   *
   * @param address receiver address
   * @param message message to send
   * @param destNodeId destination node identifier
   * @param handshakeParams optional parameter to create handshake
   */
  suspend fun send(
    address: InetSocketAddress,
    message: Message,
    destNodeId: Bytes,
    handshakeParams: HandshakeInitParameters? = null
  ) {
    val encodeResult = packetCodec.encode(message, destNodeId, handshakeParams)
    pendingMessages.put(encodeResult.authTag.toHexString(), TrackingMessage(message, destNodeId))
    logger.trace("sending message {} for {}", message.type(), address)
    receiveChannel.send(ByteBuffer.wrap(encodeResult.content.toArrayUnsafe()), address)
  }

  /**
   * Shut downs both udp receive loop and sender socket
   */
  suspend fun terminate() {
    if (started.compareAndSet(true, false)) {
      refreshJob.cancel()
      lookupJob.cancel()
      receiveJob.cancel()
      receiveChannel.close()
    }
  }

  /**
   * Attach observer for listening processed messages
   *
   * @param observer instance, proceeding observation
   */
  fun attachObserver(observer: MessageObserver) {
    messageListeners.add(observer)
  }

  /**
   * Remove observer for listening processed message
   *
   * @param observer observer for removal
   */
  fun detachObserver(observer: MessageObserver) {
    messageListeners.remove(observer)
  }

  /**
   * Retrieve enr of pinging node
   *
   * @param node identifier
   *
   * @return node record
   */
  fun getAwaitingPongRecord(nodeId: Bytes): Bytes? {
    val nodeIdHex = nodeId.toHexString()
    val result = pings.getIfPresent(nodeIdHex)
    pings.invalidate(nodeIdHex)
    return result
  }

  /**
   * Provides node's session initiator key
   *
   * @return node's session initiator key
   */
  fun getSessionInitiatorKey(nodeId: Bytes): Bytes {
    return authenticationProvider.findSessionKey(nodeId.toHexString())?.initiatorKey
      ?: throw IllegalArgumentException("Session key not found.")
  }

  /**
   * Provides node's topic table
   *
   * @return node's topic table
   */
  fun getTopicTable(): TopicTable = topicTable

  /**
   * Provides node's ticket holder
   *
   * @return node's ticket holder
   */
  fun getTicketHolder(): TicketHolder = ticketHolder

  /**
   * Provides node's topic registrar
   *
   * @return node's topic registrar
   */
  fun getTopicRegistrar(): TopicRegistrar = topicRegistrar

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
      val datagram = ByteBuffer.allocate(Message.MAX_UDP_MESSAGE_SIZE)
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
    if (datagram.limit() > Message.MAX_UDP_MESSAGE_SIZE) {
      return
    }
    val messageBytes = Bytes.wrapByteBuffer(datagram)
    val decodeResult = packetCodec.decode(messageBytes)
    val message = decodeResult.message
    logger.trace("Received message of type {}", message.type())
    when (message.type()) {
      MessageType.RANDOM -> handleRandom(message as RandomMessage, address, decodeResult.srcNodeId)
      MessageType.WHOAREYOU -> handleWhoAreYou(message as WhoAreYouMessage, address)
      MessageType.FINDNODE -> handleFindNode(message as FindNodeMessage, address, decodeResult.srcNodeId )
      MessageType.NODES -> handleNodes(message as NodesMessage)
      MessageType.PING -> handlePing(message as PingMessage, address, decodeResult.srcNodeId)
      MessageType.PONG -> handlePong(message as PongMessage, address, decodeResult.srcNodeId)
      MessageType.REGTOPIC -> handleRegTopic(
        message as RegTopicMessage,
        address,
        decodeResult.srcNodeId
      )
      MessageType.REGCONFIRM -> handleRegConfirmation(
        message as RegConfirmationMessage
      )
      MessageType.TICKET -> handleTicket(message as TicketMessage, decodeResult.srcNodeId)
      MessageType.TOPICQUERY -> handleTopicQuery(
        message as TopicQueryMessage,
        address,
        decodeResult.srcNodeId
      )
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

  private suspend fun handleWhoAreYou(
    message: WhoAreYouMessage,
    address: InetSocketAddress
  ) {
    // Retrieve enr
    val trackingMessage = getPendingMessage(message.authTag)
    if (trackingMessage == null) {
      logger.trace("No tracking message matching this authTag, not responding: {}", message.authTag)
    } else {
      val rlpEnr = getNodeRecords().find(trackingMessage.nodeId)
      if (rlpEnr == null) {
        logger.trace("No known ENR matching this nodeId, dropping: {}", trackingMessage.nodeId)
      } else {
        logger.trace("Responding to WHOAREYOU for peer: {}", trackingMessage.nodeId)
        val handshakeParams = HandshakeInitParameters(message.idNonce, message.authTag, rlpEnr)
        val response = if (trackingMessage.message.type() == MessageType.RANDOM) {
          FindNodeMessage()
        } else {
          trackingMessage.message
        }
        send(address, response, trackingMessage.nodeId, handshakeParams)
      }
    }
  }

  private suspend fun handleNodes(
    message: NodesMessage
  ) {
    message.nodeRecords.forEach {
      EthereumNodeRecord.fromRLP(it)
      getNodeRecords().set(it)
      getNodesTable().add(it)
    }
  }

  private suspend fun handleFindNode(
  message: FindNodeMessage,
  address: InetSocketAddress,
  srcNodeId: Bytes
  ) {
    if (0 == message.distance) {
      val response = NodesMessage(message.requestId, 1, listOf(getEnrBytes()))
      send(address, response, srcNodeId)
      return
    }

    val nodes = getNodesTable().nodesOfDistance(message.distance)

    nodes.chunked(MAX_NODES_IN_RESPONSE).forEach {
      val response = NodesMessage(message.requestId, nodes.size, it)
      send(address, response, srcNodeId)
    }
  }

  private suspend fun handleTicket(
    message: TicketMessage,
    srcNodeId: Bytes
  ) {
    val ticketHolder = getTicketHolder()
    ticketHolder.put(message.requestId, message.ticket)

    if (message.waitTime != 0L) {
      val key = getSessionInitiatorKey(srcNodeId)
      val ticket = Ticket.decrypt(message.ticket, key)
      getTopicRegistrar().delayRegTopic(message.requestId, ticket.topic, message.waitTime)
    }
  }

  private suspend fun handleTopicQuery(
    message: TopicQueryMessage,
    address: InetSocketAddress,
    srcNodeId: Bytes
  ) {
    val topicTable = getTopicTable()
    val nodes = topicTable.getNodes(Topic(message.topic.toHexString()))

    nodes.chunked(MAX_NODES_IN_RESPONSE).forEach {
      val response = NodesMessage(message.requestId, nodes.size, it)
      send(address, response, srcNodeId)
    }
  }

  private suspend fun handleRandom(
    message: RandomMessage,
    address: InetSocketAddress,
    srcNodeId: Bytes
  ) {
    val response = WhoAreYouMessage(message.authTag)
    send(address, response, srcNodeId)
  }

  private suspend fun handlePong(
  message: PongMessage,
  address: InetSocketAddress,
  srcNodeId: Bytes
  ) {
    val enrBytes = getAwaitingPongRecord(srcNodeId) ?: return
    val enr = EthereumNodeRecord.fromRLP(enrBytes)
    if (enr.seq != message.enrSeq) {
      val request = FindNodeMessage(message.requestId)
      send(address, request, srcNodeId)
    }
  }

  private suspend fun handlePing(
    message: PingMessage,
    address: InetSocketAddress,
    srcNodeId: Bytes
  ) {
    val response =
      PongMessage(message.requestId, getEnr().seq, address.address, address.port)
    send(address, response, srcNodeId)
  }

  private val now: () -> Long = DiscoveryService.CURRENT_TIME_SUPPLIER

  private suspend fun handleRegTopic(
    message: RegTopicMessage,
    address: InetSocketAddress,
    srcNodeId: Bytes
  ) {
    val topic = Topic(message.topic.toHexString())
    val key = getSessionInitiatorKey(srcNodeId)

    val existingTicket = if (!message.ticket.isEmpty) {
      val ticket = Ticket.decrypt(message.ticket, key)
      ticket.validate(srcNodeId, address.address, now(), message.topic)
      ticket
    } else null

    // Create new ticket
    val waitTime = getTopicTable().put(topic, message.nodeRecord)
    val cumTime = (existingTicket?.cumTime ?: waitTime) + waitTime
    val ticket = Ticket(message.topic, srcNodeId, address.address, now(), waitTime, cumTime)
    val encryptedTicket = ticket.encrypt(key)

    // Send ticket
    val response = TicketMessage(message.requestId, encryptedTicket, waitTime)
    send(address, response, srcNodeId)

    // Send confirmation if topic was placed
    if (waitTime == 0L) {
      val confirmation = RegConfirmationMessage(message.requestId, message.topic)
      send(address, confirmation, srcNodeId)
    }
  }

  private suspend fun handleRegConfirmation(
    message: RegConfirmationMessage
  ) {
    val ticketHolder = getTicketHolder()
    ticketHolder.remove(message.requestId)
    getTopicRegistrar().registerTopic(message.topic, true)
  }

}
