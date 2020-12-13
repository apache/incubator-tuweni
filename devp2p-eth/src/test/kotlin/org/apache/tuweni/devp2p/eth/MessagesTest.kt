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
package org.apache.tuweni.devp2p.eth

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.LogsBloomFilter
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.bigints.UInt64
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(BouncyCastleExtension::class)
class MessagesTest {

  @Test
  fun statusRoundtrip() {
    val msg =
      StatusMessage(
        63,
        UInt256.ONE,
        UInt256.ONE,
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes.fromHexString("0xe029e991"),
        12L
      )
    val read = StatusMessage.read(msg.toBytes())

    assertEquals(msg, read)
  }

  @Test
  fun newBlockHashesRoundtrip() {
    val msg =
      NewBlockHashes(listOf(Pair(Hash.fromBytes(Bytes32.random()), 1L), Pair(Hash.fromBytes(Bytes32.random()), 2L)))
    val read = NewBlockHashes.read(msg.toBytes())

    assertEquals(msg, read)
  }

  @Test
  fun blockHeadersRoundtrip() {
    val header = BlockHeader(
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      UInt256.fromBytes(Bytes32.random()),
      Gas.valueOf(3000),
      Gas.valueOf(2000),
      Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
      Hash.fromBytes(Bytes32.random()),
      UInt64.ZERO
    )
    val msg = BlockHeaders(listOf(header))
    val read = BlockHeaders.read(msg.toBytes())

    assertEquals(msg, read)
  }

  @Test
  fun getBlockBodiesRoundtrip() {
    val blockBodies = GetBlockBodies(listOf(Hash.fromBytes(Bytes32.random()), Hash.fromBytes(Bytes32.random())))
    val read = GetBlockBodies.read(blockBodies.toBytes())

    assertEquals(blockBodies, read)
  }

  @Test
  fun blockBodiesRoundtrip() {
    val header = BlockHeader(
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      UInt256.fromBytes(Bytes32.random()),
      Gas.valueOf(3000),
      Gas.valueOf(2000),
      Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
      Hash.fromBytes(Bytes32.random()),
      UInt64.ZERO
    )
    val blockBodies = BlockBodies(
      listOf(
        BlockBody(
          listOf(
            Transaction(
              UInt256.valueOf(1),
              Wei.valueOf(2),
              Gas.valueOf(2),
              Address.fromBytes(Bytes.random(20)),
              Wei.valueOf(2),
              Bytes.random(12),
              SECP256K1.KeyPair.random()
            )
          ),
          listOf(header)
        )
      )
    )
    val read = BlockBodies.read(blockBodies.toBytes())

    assertEquals(blockBodies, read)
  }

  @Test
  fun newBlockRoundtrip() {
    val header = BlockHeader(
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      UInt256.fromBytes(Bytes32.random()),
      Gas.valueOf(3000),
      Gas.valueOf(2000),
      Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
      Hash.fromBytes(Bytes32.random()),
      UInt64.ZERO
    )
    val body = BlockBody(
      listOf(
        Transaction(
          UInt256.valueOf(1),
          Wei.valueOf(2),
          Gas.valueOf(2),
          Address.fromBytes(Bytes.random(20)),
          Wei.valueOf(2),
          Bytes.random(12),
          SECP256K1.KeyPair.random()
        )
      ),
      listOf(header)
    )
    val block = Block(header, body)
    val message = NewBlock(block, UInt256.ONE)
    val read = NewBlock.read(message.toBytes())

    assertEquals(message, read)
  }

  @Test
  fun getNodeDataRoundtrip() {
    val nodeData = GetNodeData(listOf(Hash.fromBytes(Bytes32.random()), Hash.fromBytes(Bytes32.random())))
    val read = GetNodeData.read(nodeData.toBytes())

    assertEquals(nodeData, read)
  }

  @Test
  fun getReceiptsRoundtrip() {
    val getReceipts = GetReceipts(listOf(Hash.fromBytes(Bytes32.random()), Hash.fromBytes(Bytes32.random())))
    val read = GetReceipts.read(getReceipts.toBytes())

    assertEquals(getReceipts, read)
  }

  @Test
  fun transactionsRoundtrip() {
    val tx = Transactions(
      listOf(
        Transaction(
          UInt256.ZERO,
          Wei.valueOf(20),
          Gas.valueOf(20),
          Address.fromBytes(Bytes.random(20)),
          Wei.valueOf(20),
          Bytes.fromHexString("deadbeef"),
          SECP256K1.KeyPair.random(),
          1
        )
      )
    )
    val read = Transactions.read(tx.toBytes())
    assertEquals(tx, read)
  }

  @Test
  fun receiptsRoundtrip() {
    val receipts = Receipts(listOf(listOf(TransactionReceipt(Bytes32.random(), 42, LogsBloomFilter(), emptyList()))))
    val read = Receipts.read(receipts.toBytes())
    assertEquals(receipts, read)
  }

  @Test
  fun nodeDataRoundtrip() {
    val nodeData = NodeData(listOf(Bytes.fromHexString("deadbeef")))
    val read = NodeData.read(nodeData.toBytes())
    assertEquals(nodeData, read)
  }

  @Test
  fun newPooledTransactionHashesRoundTrip() {
    val newPooledTransactionHashes = NewPooledTransactionHashes(listOf(Hash.fromBytes(Bytes32.random())))
    val read = NewPooledTransactionHashes.read(newPooledTransactionHashes.toBytes())
    assertEquals(newPooledTransactionHashes, read)
  }

  @Test
  fun getPooledTransactionsRoundTrip() {
    val getPooledTransactions = GetPooledTransactions(listOf(Hash.fromBytes(Bytes32.random())))
    val read = GetPooledTransactions.read(getPooledTransactions.toBytes())
    assertEquals(getPooledTransactions, read)
  }

  @Test
  fun pooledTransactionsRoundTrip() {
    val pooledTransactions = PooledTransactions(
      listOf(
        Transaction(
          UInt256.ZERO,
          Wei.valueOf(20),
          Gas.valueOf(20),
          Address.fromBytes(Bytes.random(20)),
          Wei.valueOf(20),
          Bytes.fromHexString("deadbeef"),
          SECP256K1.KeyPair.random(),
          1
        )
      )
    )
    val read = PooledTransactions.read(pooledTransactions.toBytes())
    assertEquals(pooledTransactions, read)
  }
}
