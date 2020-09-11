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

import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress

@ExtendWith(BouncyCastleExtension::class)
class TopicTableTest {
  private val keyPair: SECP256K1.KeyPair = SECP256K1.KeyPair.random()
  private val enr = EthereumNodeRecord.create(keyPair, ip = InetAddress.getLoopbackAddress())

  private val topicTable = TopicTable(TABLE_CAPACITY, QUEUE_CAPACITY)

  @Test
  fun putAddNodeToEmptyQueueImmediately() {
    val waitTime = topicTable.put(Topic("A"), enr)

    assertFalse(topicTable.isEmpty())
    assertEquals(waitTime, 0L)
  }

  @Test
  fun putAddNodeToNotEmptyQueueShouldReturnWaitingTime() {
    val topic = Topic("A")
    topicTable.put(topic, EthereumNodeRecord.create(SECP256K1.KeyPair.random(), ip = InetAddress.getLoopbackAddress()))
    topicTable.put(topic, EthereumNodeRecord.create(SECP256K1.KeyPair.random(), ip = InetAddress.getLoopbackAddress()))

    val waitTime = topicTable.put(topic, enr)

    assertTrue(waitTime > 0L)
  }

  @Test
  fun putAddNodeToNotEmptyTableShouldReturnWaitingTime() {
    topicTable.put(Topic("A"), enr)
    topicTable.put(Topic("B"), enr)

    val waitTime = topicTable.put(Topic("C"), enr)

    assertTrue(waitTime > 0L)
  }

  @Test
  fun getNodesReturnNodesThatProvidesTopic() {
    val topic = Topic("A")
    topicTable.put(topic, EthereumNodeRecord.create(SECP256K1.KeyPair.random(), ip = InetAddress.getLoopbackAddress()))
    topicTable.put(topic, EthereumNodeRecord.create(SECP256K1.KeyPair.random(), ip = InetAddress.getLoopbackAddress()))

    val nodes = topicTable.getNodes(topic)

    assertTrue(nodes.isNotEmpty())
    assertEquals(nodes.size, 2)
  }

  @Test
  fun contains() {
    val topic = Topic("A")
    topicTable.put(topic, enr)

    val containsTrue = topicTable.contains(topic)
    assertTrue(containsTrue)

    val containsFalse = topicTable.contains(Topic("B"))
    assertFalse(containsFalse)
  }

  @AfterEach
  fun tearDown() {
    topicTable.clear()
  }

  companion object {
    private const val TABLE_CAPACITY = 2
    private const val QUEUE_CAPACITY = 2
  }
}
