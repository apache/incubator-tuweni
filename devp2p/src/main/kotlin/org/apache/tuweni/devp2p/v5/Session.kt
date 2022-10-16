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

import io.vertx.core.net.SocketAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.CompletableAsyncCompletion
import org.apache.tuweni.concurrent.CompletableAsyncResult
import org.apache.tuweni.concurrent.ExpiringMap
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.DiscoveryService
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.encrypt.AES128GCM
import org.apache.tuweni.devp2p.v5.encrypt.SessionKey
import org.apache.tuweni.devp2p.v5.topic.Ticket
import org.apache.tuweni.devp2p.v5.topic.Topic
import org.apache.tuweni.devp2p.v5.topic.TopicTable
import org.apache.tuweni.rlp.InvalidRLPTypeException
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlp.RLPReader
import org.slf4j.LoggerFactory
import java.lang.UnsupportedOperationException
import kotlin.coroutines.CoroutineContext

private const val MAX_NODES_IN_RESPONSE: Int = 4
private const val WHO_ARE_YOU_MESSAGE_LENGTH = 48
private const val SEND_REGTOPIC_DELAY_MS = 15 * 60 * 1000L // 15 min

/**
 * Tracks a session with another peer.
 */
internal class Session(
  val enr: EthereumNodeRecord,
  private val keyPair: SECP256K1.KeyPair,
  private val nodeId: Bytes32,
  private val tag: Bytes32,
  private val sessionKey: SessionKey,
  private val address: SocketAddress,
  private val sendFn: (SocketAddress, Bytes) -> Unit,
  private val ourENR: () -> EthereumNodeRecord,
  private val routingTable: RoutingTable,
  private val topicTable: TopicTable,
  private val failedPingsListener: (missedPings: Int) -> Boolean,
  override val coroutineContext: CoroutineContext
) : CoroutineScope {

  companion object {
    private val logger = LoggerFactory.getLogger(Session::class.java)

    const val PING_REFRESH = 10000L
  }

  val activeFindNodes = HashMap<Bytes, CompletableAsyncResult<List<EthereumNodeRecord>>>()
  private var activePing: CompletableAsyncCompletion? = null
  private val chunkedNodeResults = ExpiringMap<Bytes, MutableList<EthereumNodeRecord>>()
  private var missedPings = 0
  private val ticketHolder = HashMap<Bytes, Bytes>()
  private var peerSeq: Long = -1

  private fun launchPing() {
    launch {
      delay(PING_REFRESH)
      sendPing()
    }
  }

  private suspend fun sendPing(): AsyncCompletion {
    activePing?.let {
      if (!it.isDone) {
        it.cancel()
      }
    }
    val newPing = AsyncCompletion.incomplete()
    newPing.exceptionally {
      missedPings++
      if (!failedPingsListener(missedPings)) {
        launchPing()
      }
    }
    newPing.thenRun {
      this.missedPings = 0
      launchPing()
    }
    activePing = newPing
    send(PingMessage())
    return newPing
  }

  suspend fun sendFindNodes(distance: Int): AsyncResult<List<EthereumNodeRecord>> {
    val message = FindNodeMessage(distance = distance)
    val result: CompletableAsyncResult<List<EthereumNodeRecord>> = AsyncResult.incomplete()
    activeFindNodes[message.requestId] = result
    send(message)
    return result
  }

  suspend fun processMessage(messageBytes: Bytes) {
    if (messageBytes.size() > Message.MAX_UDP_MESSAGE_SIZE) {
      logger.trace("Message too long, dropping from {}", address)
      return
    }
    logger.trace("Received message from {}", address)

    var message: Message
    try {
      message = decode(messageBytes)
    } catch (e: InvalidRLPTypeException) {
      logger.trace("Bad message content, dropping from {}: {}", address, messageBytes)
      return
    }
    logger.trace("Received message of type {}", message.type())
    when (message.type()) {
      MessageType.FINDNODE -> handleFindNode(message as FindNodeMessage)
      MessageType.NODES -> handleNodes(message as NodesMessage)
      MessageType.PING -> handlePing(message as PingMessage)
      MessageType.PONG -> handlePong(message as PongMessage)
      MessageType.REGTOPIC -> handleRegTopic(
        message as RegTopicMessage
      )
      MessageType.REGCONFIRM -> handleRegConfirmation(
        message as RegConfirmationMessage
      )
      MessageType.TICKET -> handleTicket(message as TicketMessage)
      MessageType.TOPICQUERY -> handleTopicQuery(message as TopicQueryMessage)
      MessageType.RANDOM -> throw UnsupportedOperationException("random")
      MessageType.WHOAREYOU -> throw UnsupportedOperationException("whoareyou")
    }
  }

  private suspend fun handleTopicQuery(message: TopicQueryMessage) {
    val nodes = topicTable.getNodes(Topic(message.topic.toHexString()))

    for (chunk in nodes.chunked(MAX_NODES_IN_RESPONSE)) {
      val response = NodesMessage(message.requestId, nodes.size, chunk)
      send(response)
    }
  }

  private suspend fun handlePong(
    message: PongMessage
  ) {
    if (activePing?.isDone == true) {
      logger.trace("Received pong when no ping was active")
      return
    }
    if (peerSeq != message.enrSeq) {
      val request = FindNodeMessage(message.requestId)
      send(request)
    }
    activePing?.complete()
  }

  private suspend fun handlePing(
    message: PingMessage
  ) {
    activePing = AsyncCompletion.incomplete()
    val response =
      PongMessage(message.requestId, ourENR().seq(), address.host(), address.port())
    send(response)
  }

  private val now: () -> Long = DiscoveryService.CURRENT_TIME_SUPPLIER

  private suspend fun handleNodes(message: NodesMessage) {
    if (activeFindNodes[message.requestId] == null) {
      logger.trace("Received NODES message but no matching FINDNODES present. Dropping")
      return
    }
    val enrs = message.nodeRecords
    val records = chunkedNodeResults.computeIfAbsent(message.requestId) { mutableListOf() }
    records.addAll(enrs)
    logger.debug("Received ${enrs.size} for ${records.size}/${message.total}")

    // there seems to be a bug where no nodes are sent yet the total is above 0.
    if ((records.size == 0 && message.total != 0) || records.size >= message.total) {
      activeFindNodes[message.requestId]?.let {
        it.complete(chunkedNodeResults[message.requestId])
        chunkedNodeResults.remove(message.requestId)
        activeFindNodes.remove(message.requestId)
      }
    }
  }

  private suspend fun handleTicket(message: TicketMessage) {
    ticketHolder.put(message.requestId, message.ticket)

    if (message.waitTime != 0L) {
      val ticket = Ticket.decrypt(message.ticket, sessionKey.initiatorKey)
      delayRegTopic(message.requestId, ticket.topic, message.waitTime)
    }
  }

  private suspend fun handleRegTopic(
    message: RegTopicMessage
  ) {
    val topic = Topic(message.topic.toHexString())

    val existingTicket = if (!message.ticket.isEmpty) {
      val ticket = Ticket.decrypt(message.ticket, sessionKey.initiatorKey)
      ticket.validate(nodeId, address.host(), now(), message.topic)
      ticket
    } else {
      null
    }

    // Create new ticket
    val waitTime = topicTable.put(topic, message.nodeRecord)
    val cumTime = (existingTicket?.cumTime ?: waitTime) + waitTime
    val ticket = Ticket(message.topic, nodeId, address.host(), now(), waitTime, cumTime)
    val encryptedTicket = ticket.encrypt(sessionKey.initiatorKey)

    // Send ticket
    val response = TicketMessage(message.requestId, encryptedTicket, waitTime)
    sendFn(address, response.toRLP())

    // Send confirmation if topic was placed
    if (waitTime == 0L) {
      val confirmation = RegConfirmationMessage(message.requestId, message.topic)
      send(confirmation)
    }
  }

  private suspend fun handleRegConfirmation(message: RegConfirmationMessage) {
    ticketHolder.remove(message.requestId)
    registerTopic(message.topic, true)
  }

  private suspend fun send(message: Message) {
    logger.trace("Sending an encrypted message of type {}", message.type())
    val messagePlain = Bytes.concatenate(Bytes.of(message.type().byte()), message.toRLP())
    val authTag = Message.authTag()
    val encryptionResult = AES128GCM.encrypt(sessionKey.initiatorKey, authTag, messagePlain, tag)
    sendFn(address, Bytes.concatenate(tag, RLP.encodeValue(authTag), encryptionResult))
  }

  private suspend fun handleFindNode(message: FindNodeMessage) {
    if (0 == message.distance) {
      val response = NodesMessage(message.requestId, 1, listOf(ourENR()))
      send(response)
      return
    }

    val nodes = routingTable.nodesOfDistance(message.distance)

    for (chunk in nodes.chunked(MAX_NODES_IN_RESPONSE)) {
      val response = NodesMessage(message.requestId, nodes.size, chunk)
      send(response)
    }
  }

  fun decode(message: Bytes): Message {
    val tag = message.slice(0, Message.TAG_LENGTH)
    val contentWithHeader = message.slice(Message.TAG_LENGTH)
    val decodedMessage = RLP.decode(contentWithHeader) { reader -> read(tag, contentWithHeader, reader) }
    return decodedMessage
  }

  internal fun read(tag: Bytes, contentWithHeader: Bytes, reader: RLPReader): Message {
    if (reader.nextIsList()) {
      // this looks like a WHOAREYOU.
    }
    val authTag = reader.readValue()

    val encryptedContent = contentWithHeader.slice(reader.position())
    val decryptionKey = sessionKey.recipientKey
    val decryptedContent = AES128GCM.decrypt(decryptionKey, authTag, encryptedContent, tag)
    val type = decryptedContent.slice(0, 1)
    val message = decryptedContent.slice(1)

    // Retrieve result
    val messageType = MessageType.valueOf(type.toInt())
    return when (messageType) {
      MessageType.PING -> PingMessage.create(message)
      MessageType.PONG -> PongMessage.create(message)
      MessageType.FINDNODE -> FindNodeMessage.create(message)
      MessageType.NODES -> NodesMessage.create(message)
      MessageType.REGTOPIC -> RegTopicMessage.create(message)
      MessageType.TICKET -> TicketMessage.create(message)
      MessageType.REGCONFIRM -> RegConfirmationMessage.create(message)
      MessageType.TOPICQUERY -> TopicQueryMessage.create(message)
      else -> throw IllegalArgumentException("Unsupported message type $messageType")
    }
  }

  suspend fun delayRegTopic(requestId: Bytes, topic: Bytes, waitTime: Long) {
    delay(waitTime)

    val ticket = ticketHolder.get(requestId)
    ticket?.let {
      sendRegTopic(topic, ticket, requestId)
    }
  }

  suspend fun registerTopic(topic: Bytes, withDelay: Boolean = false) {
    if (withDelay) {
      delay(SEND_REGTOPIC_DELAY_MS)
    }

    sendRegTopic(topic, Bytes.EMPTY)
  }

  private suspend fun sendRegTopic(
    topic: Bytes,
    ticket: Bytes,
    requestId: Bytes = Message.requestId()
  ) {
    TODO("" + topic + ticket + requestId)
  }

//  private suspend fun sendRegTopic(
//    topic: Bytes,
//    ticket: Bytes,
//    requestId: Bytes = Message.requestId()
//  ) {
//    val nodeEnr = enr().toRLP()
//    //val message = RegTopicMessage(requestId, nodeEnr, topic, ticket)
//
//    val distance = 1
//    val receivers = routingTable.nodesOfDistance(distance)
//    receivers.forEach { rlp ->
//      val receiver = EthereumNodeRecord.fromRLP(rlp)
//      val address = InetSocketAddress(receiver.ip(), receiver.udp())
//      val nodeId = Hash.sha2_256(rlp)
//      TODO("" +address + nodeId)
//      //send(address, message, nodeId)
//    }
//  }
}
