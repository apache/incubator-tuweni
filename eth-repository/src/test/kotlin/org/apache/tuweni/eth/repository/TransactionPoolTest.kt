// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.repository

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class)
class TransactionPoolTest {

  @Test
  fun testContains() = runBlocking {
    val tx = Transaction(
      UInt256.valueOf(1),
      Wei.valueOf(2),
      Gas.valueOf(2),
      Address.fromBytes(Bytes.random(20)),
      Wei.valueOf(2),
      Bytes.random(12),
      SECP256K1.KeyPair.random()
    )
    val transactionPool = MemoryTransactionPool()
    assertFalse(transactionPool.contains(tx.hash))
    transactionPool.add(tx)
    assertTrue(transactionPool.contains(tx.hash))
    assertEquals(tx, transactionPool.get(tx.hash))
  }
}
