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
package org.apache.tuweni.devp2p.v5.topic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.internal.DefaultUdpConnector
import org.apache.tuweni.devp2p.v5.packet.RegTopicMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext

internal class TopicRegistrar(
  override val coroutineContext: CoroutineContext = Dispatchers.IO,
  private val connector: DefaultUdpConnector
) : CoroutineScope {

  companion object {
    private const val SEND_REGTOPIC_DELAY_MS = 15 * 60 * 1000L // 15 min
  }

  suspend fun delayRegTopic(requestId: Bytes, topic: Bytes, waitTime: Long) {
    delay(waitTime)

    val ticket = connector.getTicketHolder().get(requestId)
    sendRegTopic(topic, ticket, requestId)
  }

  suspend fun registerTopic(topic: Bytes, withDelay: Boolean = false) {
    if (withDelay) {
      delay(SEND_REGTOPIC_DELAY_MS)
    }

    sendRegTopic(topic)
  }

  private suspend fun sendRegTopic(
    topic: Bytes,
    ticket: Bytes = Bytes.EMPTY,
    requestId: Bytes = UdpMessage.requestId()
  ) {
    val nodeEnr = connector.getEnrBytes()
    val message = RegTopicMessage(requestId, nodeEnr, topic, ticket)

    val distance = 1
    val receivers = connector.getNodesTable().nodesOfDistance(distance)
    receivers.forEach { rlp ->
      val receiver = EthereumNodeRecord.fromRLP(rlp)
      val address = InetSocketAddress(receiver.ip(), receiver.udp())
      val nodeId = Hash.sha2_256(rlp)
      connector.send(address, message, nodeId)
    }
  }
}
