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

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.devp2p.v5.packet.PingMessage
import org.apache.tuweni.devp2p.v5.packet.PongMessage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class IntegrationTest : AbstractIntegrationTest() {

  @Test
  fun testHandshake() {
    val node1 = createNode(9090)
    val node2 = createNode(9091)

    val result = handshake(node1, node2)

    assert(result)

    node1.service.terminate(true)
    node2.service.terminate(true)
  }

  @Test
  fun testPing() {
    val node1 = createNode(9090)
    val node2 = createNode(9091)

    handshake(node1, node2)
    val pong = sendAndAwait<PongMessage>(node1, node2, PingMessage())

    assert(node1.port == pong.recipientPort)

    node1.service.terminate(true)
    node2.service.terminate(true)
  }

  @Test
  fun testTableMaintenance() {
    val node1 = createNode(9090)
    val node2 = createNode(9091)

    handshake(node1, node2)
    runBlocking {
      assert(!node1.routingTable.isEmpty())

      node2.service.terminate( true)

      delay(5000)

      assert(node1.routingTable.isEmpty())

      node1.service.terminate(true)
    }
  }

  @Test
  @Disabled
  fun testNetworkLookup() {
    val targetNode = createNode(9090)

    val node1 = createNode(9091)
    val node2 = createNode(9092)
    val node3 = createNode(9093)
    val node4 = createNode(9094)
    val node5 = createNode(9095)
    val node6 = createNode(9096)
    val node7 = createNode(9097)
    val node8 = createNode(9098)
    val node9 = createNode(9099)
    val node10 = createNode(9100)
    val node11 = createNode(9101)
    val node12 = createNode(9102)
    val node13 = createNode(9103)
    val node14 = createNode(9104)
    val node15 = createNode(9105)
    val node16 = createNode(9106)
    val node17 = createNode(9107)

    handshake(node1, node2)
    handshake(node2, node3)
    handshake(node3, node4)
    handshake(node4, node5)
    handshake(node5, node6)
    handshake(node6, node7)
    handshake(node7, node8)
    handshake(node9, node10)
    handshake(node10, node11)
    handshake(node11, node12)
    handshake(node12, node13)
    handshake(node13, node14)
    handshake(node14, node15)
    handshake(node15, node16)
    handshake(node16, node17)

    handshake(targetNode, node1)
    handshake(targetNode, node4)
    handshake(targetNode, node7)

    var size = targetNode.routingTable.size
    while (size < 8) {
      val newSize = targetNode.routingTable.size
      if (size < newSize) {
        size = newSize
        println(size)
      }
    }

    node1.service.terminate(true)
    node2.service.terminate(true)
    node3.service.terminate(true)
    node4.service.terminate(true)
    node5.service.terminate(true)
    node6.service.terminate(true)
    node7.service.terminate(true)
    node8.service.terminate(true)
    node9.service.terminate(true)
    node10.service.terminate(true)
    node11.service.terminate(true)
    node12.service.terminate(true)
    node13.service.terminate(true)
    node14.service.terminate(true)
    node15.service.terminate(true)
    node16.service.terminate(true)
    node17.service.terminate(true)

    targetNode.service.terminate(true)
  }
}
