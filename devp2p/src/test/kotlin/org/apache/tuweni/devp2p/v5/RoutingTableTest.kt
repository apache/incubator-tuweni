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

import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress

@ExtendWith(BouncyCastleExtension::class)
class RoutingTableTest {

  private val keyPair: SECP256K1.KeyPair = SECP256K1.KeyPair.random()
  private val enr = EthereumNodeRecord.create(keyPair, ip = InetAddress.getLoopbackAddress())
  private val routingTable: RoutingTable = RoutingTable(enr)

  private val newKeyPair = SECP256K1.KeyPair.random()
  private val newEnr = EthereumNodeRecord.toRLP(newKeyPair, ip = InetAddress.getLoopbackAddress())

  @Test
  fun addCreatesRecordInBucket() {
    routingTable.add(newEnr)
    assertTrue(!routingTable.isEmpty())
  }

  @Test
  fun evictRemovesRecord() {
    routingTable.add(newEnr)

    assertTrue(!routingTable.isEmpty())

    routingTable.evict(newEnr)

    assertTrue(routingTable.isEmpty())
  }

  @Test
  fun getSelfEnrGivesTableOwnerEnr() {
    val result = routingTable.getSelfEnr()
    assertEquals(enr, result)
  }

  @Test
  fun distanceToSelf() {
    assertEquals(0, routingTable.distanceToSelf(routingTable.getSelfEnr().toRLP()))
    assertNotEquals(0, routingTable.distanceToSelf(newEnr))
  }

  @AfterEach
  fun tearDown() {
    routingTable.clear()
  }
}
