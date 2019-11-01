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
package org.apache.tuweni.devp2p.v5.internal.handler

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.DiscoveryService.Companion.CURRENT_TIME_SUPPLIER
import org.apache.tuweni.devp2p.v5.MessageHandler
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.packet.RegConfirmationMessage
import org.apache.tuweni.devp2p.v5.packet.RegTopicMessage
import org.apache.tuweni.devp2p.v5.packet.TicketMessage
import org.apache.tuweni.devp2p.v5.topic.Ticket
import org.apache.tuweni.devp2p.v5.topic.Ticket.Companion.TICKET_INVALID_MSG
import org.apache.tuweni.devp2p.v5.topic.TicketHolder
import org.apache.tuweni.devp2p.v5.topic.Topic
import java.net.InetSocketAddress
import java.security.SecureRandom
import javax.crypto.KeyGenerator

class RegTopicMessageHandler : MessageHandler<RegTopicMessage> {

  private val now: () -> Long = CURRENT_TIME_SUPPLIER


  override fun handle(message: RegTopicMessage, address: InetSocketAddress, srcNodeId: Bytes, connector: UdpConnector) {
    val topic = Topic(message.topic.toHexString())
    val ticketHolder = connector.getTicketHolder()

    val existingTicket = if (!message.ticket.isEmpty) {
      val ticket = decodeTicket(message.ticket, message.requestId, ticketHolder)
      ticket.validate(srcNodeId, address.address, now())
      ticket
    } else null

    // Create new ticket
    val waitTime = connector.getTopicTable().put(topic, message.nodeRecord)
    val cumTime = existingTicket?.cumTime ?: waitTime
    val ticket = Ticket(message.topic, srcNodeId, address.address, now(), waitTime, cumTime)
    val key = generateTicketKey()
    val encryptedTicket = ticket.encrypt(key)

    // Send ticket
    val response = TicketMessage(message.requestId, encryptedTicket, waitTime)
    connector.send(address, response, srcNodeId)
    ticketHolder.putKey(message.requestId, key, now() + waitTime + Ticket.TIME_WINDOW_MS)

    // Send confirmation if topic was placed
    if (waitTime == 0L) {
      val confirmation = RegConfirmationMessage(message.requestId, message.topic)
      connector.send(address, confirmation, srcNodeId)
      ticketHolder.removeKey(message.requestId)
    }
  }

  private fun decodeTicket(ticketBytes: Bytes, requestId: Bytes, ticketHolder: TicketHolder): Ticket {
    val key = ticketHolder.getKey(requestId) ?: throw IllegalArgumentException(TICKET_INVALID_MSG)
    return Ticket.decrypt(ticketBytes, key)
  }

  private fun generateTicketKey(): Bytes {
    val generator = KeyGenerator.getInstance(TICKET_KEY_ALGORITHM)
    generator.init(128, SecureRandom())
    return Bytes.wrap(generator.generateKey().encoded)
  }

  companion object {
    private const val TICKET_KEY_ALGORITHM = "AES"
  }
}
