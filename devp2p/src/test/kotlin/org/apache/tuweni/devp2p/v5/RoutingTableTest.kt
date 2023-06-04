// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
