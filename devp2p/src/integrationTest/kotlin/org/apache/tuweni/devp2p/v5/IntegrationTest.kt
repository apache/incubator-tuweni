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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

@Timeout(10)
class IntegrationTest : AbstractIntegrationTest() {

  @Test
  fun testHandshake() = runBlocking {
    val node1 = createNode(19090)
    val node2 = createNode(19091)

    val result = handshake(node1, node2)
    assertTrue(result)

    node1.service.terminate()
    node2.service.terminate()
  }

  @Test
  fun testPing() = runBlocking {
    val node1 = createNode(29090)
    val node2 = createNode(29091)

    handshake(node1, node2)

    val pong = sendAndAwait<PongMessage>(node1, node2, PingMessage())

    assertTrue(node1.port == pong.recipientPort)

    node1.service.terminate()
    node2.service.terminate()
  }

  @Test
  fun testTableMaintenance() = runBlocking {
    val node1 = createNode(39090)
    val node2 = createNode(39091)

    handshake(node1, node2)

    assertTrue(!node1.routingTable.isEmpty())

    node2.service.terminate()

    delay(5000)

    assertTrue(node1.routingTable.isEmpty())

    node1.service.terminate()
  }

  @Test
  @Disabled
  fun testNetworkLookup() = runBlocking {
    val targetNode = createNode(49090)

    val node1 = createNode(49091)
    val node2 = createNode(49092)
    val node3 = createNode(49093)
    val node4 = createNode(49094)
    val node5 = createNode(49095)
    val node6 = createNode(49096)
    val node7 = createNode(49097)
    val node8 = createNode(49098)
    val node9 = createNode(49099)
    val node10 = createNode(49100)
    val node11 = createNode(49101)
    val node12 = createNode(49102)
    val node13 = createNode(49103)
    val node14 = createNode(49104)
    val node15 = createNode(49105)
    val node16 = createNode(49106)
    val node17 = createNode(49107)

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

    node1.service.terminate()
    node2.service.terminate()
    node3.service.terminate()
    node4.service.terminate()
    node5.service.terminate()
    node6.service.terminate()
    node7.service.terminate()
    node8.service.terminate()
    node9.service.terminate()
    node10.service.terminate()
    node11.service.terminate()
    node12.service.terminate()
    node13.service.terminate()
    node14.service.terminate()
    node15.service.terminate()
    node16.service.terminate()
    node17.service.terminate()

    targetNode.service.terminate()
  }
}
