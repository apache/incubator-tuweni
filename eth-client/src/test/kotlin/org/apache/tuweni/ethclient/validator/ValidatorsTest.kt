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
package org.apache.tuweni.ethclient.validator

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.trie.MerklePatriciaTrie
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.bigints.UInt64
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(BouncyCastleExtension::class)
class TransactionsHashValidatorTest {

  @Test
  fun testInvalidTransactionRootHash() {
    val tx = Transaction(
      UInt256.valueOf(1),
      Wei.valueOf(2),
      Gas.valueOf(2),
      Address.fromBytes(Bytes.random(20)),
      Wei.valueOf(2),
      Bytes.random(12),
      SECP256K1.KeyPair.random()
    )

    val block = Block(
      BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        UInt256.ZERO,
        Gas.valueOf(3000),
        Gas.valueOf(2000),
        Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
        Hash.fromBytes(Bytes32.random()),
        UInt64.ZERO
      ),
      BlockBody(listOf(tx), listOf())
    )

    assertFalse(TransactionsHashValidator(from = null, to = null).validate(block))
  }

  @Test
  fun testValidTransactionRootHash() = runBlocking {
    val tx = Transaction(
      UInt256.valueOf(1),
      Wei.valueOf(2),
      Gas.valueOf(2),
      Address.fromBytes(Bytes.random(20)),
      Wei.valueOf(2),
      Bytes.random(12),
      SECP256K1.KeyPair.random()
    )

    val transactionsTrie = MerklePatriciaTrie.storingBytes()

    val indexKey = RLP.encodeValue(UInt256.valueOf(0L).trimLeadingZeros())
    transactionsTrie.put(indexKey, tx.toBytes())

    val block = Block(
      BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(transactionsTrie.rootHash()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        UInt256.ZERO,
        Gas.valueOf(3000),
        Gas.valueOf(2000),
        Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
        Hash.fromBytes(Bytes32.random()),
        UInt64.ZERO
      ),
      BlockBody(listOf(tx), listOf())
    )

    assertTrue(TransactionsHashValidator().validate(block))
  }
}

@ExtendWith(BouncyCastleExtension::class)
class ChainIdValidatorTest {

  @Test
  fun testBadChainId() {
    val tx = Transaction(
      UInt256.valueOf(1),
      Wei.valueOf(2),
      Gas.valueOf(2),
      Address.fromBytes(Bytes.random(20)),
      Wei.valueOf(2),
      Bytes.random(12),
      SECP256K1.KeyPair.random(),
      34
    )

    val goodTx = Transaction(
      UInt256.valueOf(1),
      Wei.valueOf(2),
      Gas.valueOf(2),
      Address.fromBytes(Bytes.random(20)),
      Wei.valueOf(2),
      Bytes.random(12),
      SECP256K1.KeyPair.random(),
      35
    )

    val block = Block(
      BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        UInt256.ZERO,
        Gas.valueOf(3000),
        Gas.valueOf(2000),
        Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
        Hash.fromBytes(Bytes32.random()),
        UInt64.ZERO
      ),
      BlockBody(listOf(goodTx, tx), listOf())
    )

    assertFalse(ChainIdValidator(chainId = 35).validate(block))
  }

  @Test
  fun testSameChainId() {
    val tx = Transaction(
      UInt256.valueOf(1),
      Wei.valueOf(2),
      Gas.valueOf(2),
      Address.fromBytes(Bytes.random(20)),
      Wei.valueOf(2),
      Bytes.random(12),
      SECP256K1.KeyPair.random(),
      34
    )

    val block = Block(
      BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        UInt256.ZERO,
        Gas.valueOf(3000),
        Gas.valueOf(2000),
        Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
        Hash.fromBytes(Bytes32.random()),
        UInt64.ZERO
      ),
      BlockBody(listOf(tx, tx, tx), listOf())
    )

    assertTrue(ChainIdValidator(chainId = 34).validate(block))
  }
}
