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

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopScoreDocCollector
import org.apache.lucene.store.Directory
import org.apache.lucene.util.BytesRef
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Log
import org.apache.tuweni.eth.LogsBloomFilter
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.LuceneIndex
import org.apache.tuweni.junit.LuceneIndexWriter
import org.apache.tuweni.junit.LuceneIndexWriterExtension
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.bigints.UInt64
import org.apache.tuweni.units.ethereum.Gas
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(LuceneIndexWriterExtension::class, BouncyCastleExtension::class)
internal class BlockchainIndexTest {

  @Test
  @Throws(IOException::class)
  fun testIndexBlockHeaderElements(@LuceneIndexWriter writer: IndexWriter) {
    writer.deleteAll()

    val blockchainIndex = BlockchainIndex(writer)
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
    blockchainIndex.index { it.indexBlockHeader(header) }

    val reader = DirectoryReader.open(writer)
    val searcher = IndexSearcher(reader)
    val collector = TopScoreDocCollector.create(10, ScoreDoc(1, 1.0f))
    val query = BooleanQuery.Builder()
      .add(TermQuery(Term("_id", BytesRef(header.hash.toArrayUnsafe()))), BooleanClause.Occur.MUST)
      .add(TermQuery(Term("_type", "block")), BooleanClause.Occur.MUST)
    searcher.search(query.build(), collector)
    val hits = collector.topDocs().scoreDocs
    assertEquals(1, hits.size)
  }

  @Test
  @Throws(IOException::class)
  fun testIndexCommit(@LuceneIndexWriter writer: IndexWriter, @LuceneIndex index: Directory) {
    writer.deleteAll()
    val blockchainIndex = BlockchainIndex(writer)
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
    blockchainIndex.index { w -> w.indexBlockHeader(header) }

    val reader = DirectoryReader.open(index)
    val searcher = IndexSearcher(reader)
    val collector = TopScoreDocCollector.create(10, ScoreDoc(1, 1.0f))
    val query = BooleanQuery.Builder()
      .add(TermQuery(Term("_id", BytesRef(header.hash.toArrayUnsafe()))), BooleanClause.Occur.MUST)
      .add(TermQuery(Term("_type", "block")), BooleanClause.Occur.MUST)
    searcher.search(query.build(), collector)
    val hits = collector.topDocs().scoreDocs
    assertEquals(1, hits.size)
  }

  @Test
  @Throws(IOException::class)
  fun queryBlockHeaderByField(@LuceneIndexWriter writer: IndexWriter) {
    writer.deleteAll()
    val blockchainIndex = BlockchainIndex(writer)
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
      UInt64.random()
    )
    blockchainIndex.index { it.indexBlockHeader(header) }

    val reader = blockchainIndex as BlockchainIndexReader

    run {
      val entries = reader.findBy(BlockHeaderFields.PARENT_HASH, header.parentHash!!)
      assertEquals(1, entries.size)
      assertEquals(header.hash, entries[0])
    }

    run {
      val entries = reader.findBy(BlockHeaderFields.OMMERS_HASH, header.ommersHash)
      assertEquals(1, entries.size)
      assertEquals(header.hash, entries[0])
    }

    run {
      val entries = reader.findBy(BlockHeaderFields.COINBASE, header.coinbase)
      assertEquals(1, entries.size)
      assertEquals(header.hash, entries[0])
    }

    run {
      val entries = reader.findBy(BlockHeaderFields.STATE_ROOT, header.stateRoot)
      assertEquals(1, entries.size)
      assertEquals(header.hash, entries[0])
    }

    run {
      val entries = reader.findBy(BlockHeaderFields.STATE_ROOT, header.stateRoot)
      assertEquals(1, entries.size)
      assertEquals(header.hash, entries[0])
    }

    run {
      val entries = reader.findBy(BlockHeaderFields.DIFFICULTY, header.difficulty)
      assertEquals(1, entries.size)
      assertEquals(header.hash, entries[0])
    }

    run {
      val entries = reader.findBy(BlockHeaderFields.TIMESTAMP, header.timestamp.toEpochMilli())
      assertEquals(1, entries.size)
      assertEquals(header.hash, entries[0])
    }

    run {
      val entries = reader.findBy(BlockHeaderFields.NUMBER, header.number)
      assertEquals(1, entries.size)
      assertEquals(header.hash, entries[0])
    }

    run {
      val entries = reader.findInRange(
        BlockHeaderFields.NUMBER,
        header.number.subtract(5),
        header.number.add(5)
      )
      assertEquals(1, entries.size)
      assertEquals(header.hash, entries[0])
    }

    run {
      val entries = reader.findBy(BlockHeaderFields.EXTRA_DATA, header.extraData)
      assertEquals(1, entries.size)
      assertEquals(header.hash, entries[0])
    }

    run {
      val entries = reader.findBy(BlockHeaderFields.GAS_LIMIT, header.gasLimit)
      assertEquals(1, entries.size, entries.toString())
      assertEquals(header.hash, entries[0])
    }

    run {
      val entries = reader.findBy(BlockHeaderFields.GAS_USED, header.gasUsed)
      assertEquals(1, entries.size)
      assertEquals(header.hash, entries[0])
    }
  }

  @Test
  fun testTotalDifficulty(@LuceneIndexWriter writer: IndexWriter) {
    writer.deleteAll()
    val blockchainIndex = BlockchainIndex(writer)
    val header = BlockHeader(
      null,
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(1),
      UInt256.fromBytes(Bytes32.random()),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )
    blockchainIndex.index { w -> w.indexBlockHeader(header) }
    assertEquals(UInt256.valueOf(1), blockchainIndex.totalDifficulty(header.hash))

    val childHeader = BlockHeader(
      header.hash,
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(3),
      UInt256.fromBytes(Bytes32.random()),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      UInt64.random()
    )

    blockchainIndex.index { w -> w.indexBlockHeader(childHeader) }

    assertEquals(UInt256.valueOf(4), blockchainIndex.totalDifficulty(childHeader.hash))
  }

  @Test
  fun queryTransactionReceiptByField(@LuceneIndexWriter writer: IndexWriter) {
    writer.deleteAll()
    val blockchainIndex = BlockchainIndex(writer)

    val txReceipt = TransactionReceipt(
      Bytes32.random(),
      3,
      LogsBloomFilter(Bytes.random(256)),
      listOf(
        Log(
          Address.fromBytes(Bytes.random(20)),
          Bytes.fromHexString("deadbeef"),
          listOf(Bytes32.random(), Bytes32.random())
        )
      )
    )

    val txHash = Hash.fromBytes(Bytes32.random())
    val blockHash = Hash.fromBytes(Bytes32.random())

    blockchainIndex.index { it.indexTransactionReceipt(txReceipt, 43, txHash, blockHash) }

    val txReceiptWithStatus = TransactionReceipt(
      42,
      322,
      LogsBloomFilter(Bytes.random(256)),
      listOf(
        Log(
          Address.fromBytes(Bytes.random(20)),
          Bytes.fromHexString("deadbeef"),
          listOf(Bytes32.random(), Bytes32.random())
        )
      )
    )

    val txHash2 = Hash.fromBytes(Bytes32.random())
    val blockHash2 = Hash.fromBytes(Bytes32.random())

    blockchainIndex.index { it.indexTransactionReceipt(txReceiptWithStatus, 32, txHash2, blockHash2) }

    val reader = blockchainIndex as BlockchainIndexReader

    run {
      val entries = reader.findBy(TransactionReceiptFields.BLOCK_HASH, blockHash)
      assertEquals(1, entries.size)
      assertEquals(txHash, entries[0])
    }

    run {
      val entries = reader.findBy(TransactionReceiptFields.TRANSACTION_HASH, txHash)
      assertEquals(1, entries.size)
      assertEquals(txHash, entries[0])
    }

    run {
      val entries = reader.findBy(TransactionReceiptFields.BLOOM_FILTER, txReceipt.bloomFilter.toBytes())
      assertEquals(1, entries.size)
      assertEquals(txHash, entries[0])
    }

    run {
      val entries = reader.findBy(TransactionReceiptFields.STATE_ROOT, txReceipt.stateRoot)
      assertEquals(1, entries.size)
      assertEquals(txHash, entries[0])
    }

    run {
      val entries = reader.findBy(TransactionReceiptFields.LOGGER, txReceipt.logs[0].logger)
      assertEquals(1, entries.size)
      assertEquals(txHash, entries[0])
    }

    run {
      val entries = reader.findBy(TransactionReceiptFields.LOG_TOPIC, txReceipt.logs[0].topics[0])
      assertEquals(1, entries.size)
      assertEquals(txHash, entries[0])
    }

    run {
      val entries = reader.findBy(TransactionReceiptFields.STATUS, txReceiptWithStatus.status)
      assertEquals(1, entries.size)
      assertEquals(txHash2, entries[0])
    }

    run {
      val entries = reader.findBy(TransactionReceiptFields.CUMULATIVE_GAS_USED, txReceipt.cumulativeGasUsed)
      assertEquals(1, entries.size)
      assertEquals(txHash, entries[0])
    }
  }
}
