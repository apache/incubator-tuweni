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

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.DiscoveryService.Companion.CURRENT_TIME_SUPPLIER
import org.apache.tuweni.devp2p.v5.topic.Ticket
import org.apache.tuweni.devp2p.v5.topic.Topic
import java.net.InetSocketAddress

/**
 * Handler managing topic registration messages.
 */
internal class RegTopicMessageHandler : MessageHandler<RegTopicMessage> {

  private val now: () -> Long = CURRENT_TIME_SUPPLIER

  override suspend fun handle(
    message: RegTopicMessage,
    address: InetSocketAddress,
    srcNodeId: Bytes,
    connector: UdpConnector
  ) {
    val topic = Topic(message.topic.toHexString())
    val key = connector.getSessionInitiatorKey(srcNodeId)

    val existingTicket = if (!message.ticket.isEmpty) {
      val ticket = Ticket.decrypt(message.ticket, key)
      ticket.validate(srcNodeId, address.address, now(), message.topic)
      ticket
    } else null

    // Create new ticket
    val waitTime = connector.getTopicTable().put(topic, message.nodeRecord)
    val cumTime = (existingTicket?.cumTime ?: waitTime) + waitTime
    val ticket = Ticket(message.topic, srcNodeId, address.address, now(), waitTime, cumTime)
    val encryptedTicket = ticket.encrypt(key)

    // Send ticket
    val response = TicketMessage(message.requestId, encryptedTicket, waitTime)
    connector.send(address, response, srcNodeId)

    // Send confirmation if topic was placed
    if (waitTime == 0L) {
      val confirmation = RegConfirmationMessage(message.requestId, message.topic)
      connector.send(address, confirmation, srcNodeId)
    }
  }
}
