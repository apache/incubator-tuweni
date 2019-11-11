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

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.AbstractIntegrationTest
import org.apache.tuweni.devp2p.v5.packet.NodesMessage
import org.apache.tuweni.devp2p.v5.packet.RegTopicMessage
import org.apache.tuweni.devp2p.v5.packet.TicketMessage
import org.apache.tuweni.devp2p.v5.packet.TopicQueryMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.junit.jupiter.api.Test
import java.net.InetAddress


class TopicIntegrationTest : AbstractIntegrationTest() {

  @Test
  fun advertiseTopicAndRegistrationSuccessful() {
    val node1 = createNode(9090)
    val node2 = createNode(9091)
    handshake(node1, node2)

    val requestId = UdpMessage.requestId()
    val topic = Topic("0x41")
    val message = RegTopicMessage(requestId, node1.enr, topic.toBytes(), Bytes.EMPTY)
    val ticketMessage = sendAndAwait<TicketMessage>(node1, node2, message)

    assert(ticketMessage.requestId == requestId)
    assert(ticketMessage.waitTime == 0L)
    assert(node2.topicTable.contains(topic))

    node1.service.terminate(true)
    node2.service.terminate(true)
  }

  @Test
  fun advertiseTopicAndNeedToWaitWhenTopicQueueIsFull() {
    val node1 = createNode(9090)
    val node2 = createNode(9091, topicTable = TopicTable(2, 2))
    handshake(node1, node2)

    val topic = Topic("0x41")
    node2.topicTable.put(topic, node2.enr)
    node2.topicTable.put(topic, EthereumNodeRecord.toRLP(SECP256K1.KeyPair.random(), ip = InetAddress.getLocalHost()))

    val requestId = UdpMessage.requestId()
    val message = RegTopicMessage(requestId, node1.enr, topic.toBytes(), Bytes.EMPTY)
    val ticketMessage = sendAndAwait<TicketMessage>(node1, node2, message)

    assert(ticketMessage.requestId == requestId)
    assert(ticketMessage.waitTime > 0L)
    assert(node1.ticketHolder.contains(ticketMessage.ticket))
    assert(!node2.topicTable.getNodes(topic).contains(node1.enr))

    node1.service.terminate(true)
    node2.service.terminate(true)
  }

  @Test
  fun searchTopicReturnListOfNodes() {
    val node1 = createNode(9090)
    val node2 = createNode(9091)
    handshake(node1, node2)

    runBlocking {
      val topic = Topic("0x41")
      node2.topicTable.put(topic, node2.enr)
      val requestId = UdpMessage.requestId()
      val message = TopicQueryMessage(requestId, topic.toBytes())
      val result = sendAndAwait<NodesMessage>(node1, node2, message)

      assert(result.requestId == requestId)
      assert(result.nodeRecords.isNotEmpty())
    }

    node1.service.terminate(true)
    node2.service.terminate(true)
  }
}
