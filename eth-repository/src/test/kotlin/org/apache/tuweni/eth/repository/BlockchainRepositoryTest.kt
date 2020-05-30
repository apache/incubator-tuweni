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
package org.apache.tuweni.eth.repository

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Log
import org.apache.tuweni.eth.LogsBloomFilter
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.LuceneIndexWriter
import org.apache.tuweni.junit.LuceneIndexWriterExtension
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.apache.lucene.index.IndexWriter
import org.apache.tuweni.units.bigints.UInt64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(BouncyCastleExtension::class, LuceneIndexWriterExtension::class)
internal class BlockchainRepositoryTest {

  @Test
  @Throws(Exception::class)
  fun storeAndRetrieveBlock(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    val genesisHeader = BlockHeader(
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
      UInt64.random()
    )
    val genesisBlock = Block(genesisHeader, BlockBody(emptyList(), emptyList()))
    val repo = BlockchainRepository
      .init(
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        BlockchainIndex(writer),
        genesisBlock
      )
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
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
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
      emptyList()
    )
    val block = Block(header, body)
    repo.storeBlock(block)
    val read = repo.retrieveBlock(block.getHeader().getHash())
    assertEquals(block, read)
    assertEquals(block.getHeader(), repo.retrieveBlockHeader(block.getHeader().getHash()))
  }

  @Test
  @Throws(Exception::class)
  fun storeChainHead(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    val genesisHeader = BlockHeader(
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
      UInt64.random()
    )
    val genesisBlock = Block(genesisHeader, BlockBody(emptyList(), emptyList()))
    val repo = BlockchainRepository
      .init(
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        BlockchainIndex(writer),
        genesisBlock
      )

    val header = BlockHeader(
      genesisHeader.getHash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      genesisHeader.getNumber().add(UInt256.valueOf(1)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )
    val biggerNumber = BlockHeader(
      header.getHash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      header.getNumber().add(UInt256.valueOf(1)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )
    val biggerNumber2 = BlockHeader(
      biggerNumber.getHash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      header.getNumber().add(UInt256.valueOf(2)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )
    val biggerNumber3 = BlockHeader(
      biggerNumber2.getHash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      header.getNumber().add(UInt256.valueOf(3)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )

    repo.storeBlockHeader(header)
    repo.storeBlockHeader(biggerNumber)
    repo.storeBlockHeader(biggerNumber2)
    repo.storeBlockHeader(biggerNumber3)

    assertEquals(biggerNumber3.getHash(), repo.retrieveChainHeadHeader()!!.getHash())
  }

  @Test
  @Throws(Exception::class)
  fun storeChainHeadBlocks(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    val genesisHeader = BlockHeader(
      null,
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(0),
      UInt256.fromBytes(Bytes32.random()),
      Gas.valueOf(3000),
      Gas.valueOf(2000),
      Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )
    val genesisBlock = Block(genesisHeader, BlockBody(emptyList(), emptyList()))
    val repo = BlockchainRepository.init(
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        BlockchainIndex(writer),
        genesisBlock
      )

    val header = BlockHeader(
      genesisHeader.getHash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(1),
      genesisHeader.getNumber().add(UInt256.valueOf(1)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )
    val biggerNumber = BlockHeader(
      header.getHash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(2),
      header.getNumber().add(UInt256.valueOf(1)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )
    val biggerNumber2 = BlockHeader(
      biggerNumber.getHash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(3),
      header.getNumber().add(UInt256.valueOf(2)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )
    val biggerNumber3 = BlockHeader(
      biggerNumber2.getHash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(4),
      header.getNumber().add(UInt256.valueOf(3)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )

    repo.storeBlock(Block(header, BlockBody(emptyList(), emptyList())))
    repo.storeBlock(Block(biggerNumber, BlockBody(emptyList(), emptyList())))
    repo.storeBlock(Block(biggerNumber2, BlockBody(emptyList(), emptyList())))
    repo.storeBlock(Block(biggerNumber3, BlockBody(emptyList(), emptyList())))

    assertEquals(biggerNumber3.getHash(), repo.retrieveChainHeadHeader()!!.getHash())
  }

  @Test
  fun StoreChainHeadDifferentOrder(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    val genesisHeader = BlockHeader(
      null,
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(0),
      UInt256.fromBytes(Bytes32.random()),
      Gas.valueOf(3000),
      Gas.valueOf(2000),
      Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )
    val genesisBlock = Block(genesisHeader, BlockBody(emptyList(), emptyList()))
    val repo = BlockchainRepository.init(
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      BlockchainIndex(writer),
      genesisBlock
    )

    val header = BlockHeader(
      genesisHeader.getHash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(1),
      genesisHeader.getNumber().add(UInt256.valueOf(1)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )
    val biggerNumber = BlockHeader(
      header.getHash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(2),
      header.getNumber().add(UInt256.valueOf(1)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )
    val biggerNumber2 = BlockHeader(
      biggerNumber.getHash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(3),
      header.getNumber().add(UInt256.valueOf(2)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )
    val biggerNumber3 = BlockHeader(
      biggerNumber2.getHash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(4),
      header.getNumber().add(UInt256.valueOf(3)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )

    repo.storeBlock(Block(biggerNumber3, BlockBody(emptyList(), emptyList())))
    repo.storeBlock(Block(biggerNumber2, BlockBody(emptyList(), emptyList())))
    repo.storeBlock(Block(biggerNumber, BlockBody(emptyList(), emptyList())))
    repo.storeBlock(Block(header, BlockBody(emptyList(), emptyList())))

    assertEquals(biggerNumber3.getHash(), repo.retrieveChainHeadHeader()!!.getHash())
  }

  @Test
  fun storeTransactionReceipt(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    val genesisHeader = BlockHeader(
      null,
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(0),
      UInt256.fromBytes(Bytes32.random()),
      Gas.valueOf(3000),
      Gas.valueOf(2000),
      Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )
    val genesisBlock = Block(genesisHeader, BlockBody(emptyList(), emptyList()))
    val repo = BlockchainRepository.init(
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      BlockchainIndex(writer),
      genesisBlock
    )

    val txReceipt = TransactionReceipt(Bytes32.random(), 3, LogsBloomFilter(Bytes.random(256)),
      listOf(Log(Address.fromBytes(
        Bytes.random(20)),
        Bytes.fromHexString("deadbeef"),
        listOf(Bytes32.random(), Bytes32.random()))))

    val txHash = Hash.fromBytes(Bytes32.random())
    val blockHash = Hash.fromBytes(Bytes32.random())
    repo.storeTransactionReceipt(txReceipt, 4, txHash, blockHash)

    assertEquals(txReceipt, repo.retrieveTransactionReceipt(blockHash, 4))
    assertEquals(listOf(txReceipt), repo.retrieveTransactionReceipts(blockHash))
    assertEquals(txReceipt, repo.retrieveTransactionReceipt(txHash))
  }
}
