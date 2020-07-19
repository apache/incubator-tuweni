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
import org.apache.tuweni.devp2p.v5.MessageHandler
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.packet.TicketMessage
import org.apache.tuweni.devp2p.v5.topic.Ticket
import java.net.InetSocketAddress

internal class TicketMessageHandler : MessageHandler<TicketMessage> {

  override suspend fun handle(
    message: TicketMessage,
    address: InetSocketAddress,
    srcNodeId: Bytes,
    connector: UdpConnector
  ) {
    val ticketHolder = connector.getTicketHolder()
    ticketHolder.put(message.requestId, message.ticket)

    if (message.waitTime != 0L) {
      val key = connector.getSessionInitiatorKey(srcNodeId)
      val ticket = Ticket.decrypt(message.ticket, key)
      connector.getTopicRegistrar().delayRegTopic(message.requestId, ticket.topic, message.waitTime)
    }
  }
}
