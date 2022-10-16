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

import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.NumericDocValuesField
import org.apache.lucene.document.SortedDocValuesField
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexableField
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.SearcherFactory
import org.apache.lucene.search.SearcherManager
import org.apache.lucene.search.Sort
import org.apache.lucene.search.SortField
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TermRangeQuery
import org.apache.lucene.util.BytesRef
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.eth.repository.BlockHeaderFields.COINBASE
import org.apache.tuweni.eth.repository.BlockHeaderFields.DIFFICULTY
import org.apache.tuweni.eth.repository.BlockHeaderFields.EXTRA_DATA
import org.apache.tuweni.eth.repository.BlockHeaderFields.GAS_LIMIT
import org.apache.tuweni.eth.repository.BlockHeaderFields.GAS_USED
import org.apache.tuweni.eth.repository.BlockHeaderFields.NUMBER
import org.apache.tuweni.eth.repository.BlockHeaderFields.OMMERS_HASH
import org.apache.tuweni.eth.repository.BlockHeaderFields.PARENT_HASH
import org.apache.tuweni.eth.repository.BlockHeaderFields.STATE_ROOT
import org.apache.tuweni.eth.repository.BlockHeaderFields.TIMESTAMP
import org.apache.tuweni.eth.repository.BlockHeaderFields.TOTAL_DIFFICULTY
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import java.io.IOException
import java.io.UncheckedIOException

/**
 * Reader of a blockchain index.
 *
 * Allows to query for fields for exact or range matches.
 */
interface BlockchainIndexReader {

  /**
   * Find a value in a range.
   *
   * @param field the name of the field
   * @param minValue the minimum value, inclusive
   * @param maxValue the maximum value, inclusive
   * @return the matching block header hashes.
   */
  fun findInRange(field: BlockHeaderFields, minValue: UInt256, maxValue: UInt256): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: BlockHeaderFields, value: Bytes): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: BlockHeaderFields, value: Long): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: BlockHeaderFields, value: Gas): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   *
   * @return the matching block header hashes.
   */
  fun findBy(field: BlockHeaderFields, value: UInt256): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: BlockHeaderFields, value: Address): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: BlockHeaderFields, value: Hash): List<Hash>

  /**
   * Find the hash of the block header with the largest value of a specific block header field
   *
   * @param field the field to query on
   * @return the matching hash with the largest field value.
   */
  fun findByLargest(field: BlockHeaderFields): Hash?

  /**
   * Find the hash of the block header with the largest total difficulty.
   *
   * @param field the field to query on
   * @return the matching hash with the largest field value.
   */
  fun findLargestTotalDifficulty(): Hash?

  /**
   * Finds hashes of blocks by hash or number.
   *
   * @param hashOrNumber the hash of a block header, or its number as a 32-byte word
   * @return the matching block header hashes.
   */
  fun findByHashOrNumber(hashOrNumber: Bytes): List<Hash>

  /**
   * Find a value in a range.
   *
   * @param field the name of the field
   * @param minValue the minimum value, inclusive
   * @param maxValue the maximum value, inclusive
   * @return the matching block header hashes.
   */
  fun findInRange(field: TransactionReceiptFields, minValue: UInt256, maxValue: UInt256): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: TransactionReceiptFields, value: Bytes): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: TransactionReceiptFields, value: Int): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: TransactionReceiptFields, value: Long): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: TransactionReceiptFields, value: Gas): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   *
   * @return the matching block header hashes.
   */
  fun findBy(field: TransactionReceiptFields, value: UInt256): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: TransactionReceiptFields, value: Address): List<Hash>

  /**
   * Find exact matches for a field.
   *
   * @param field the name of the field
   * @param value the value of the field.
   * @return the matching block header hashes.
   */
  fun findBy(field: TransactionReceiptFields, value: Hash): List<Hash>

  /**
   * Find the hash of the block header with the largest value of a specific block header field
   *
   * @param field the field to query on
   * @return the matching hash with the largest field value.
   */
  fun findByLargest(field: TransactionReceiptFields): Hash?

  /**
   * Find a transaction request by block hash and index.
   * @param blockHash the block hash
   * @param index the index of the transaction in the block
   * @return the matching hash of the transaction if found
   */
  fun findByBlockHashAndIndex(blockHash: Bytes, index: Int): Hash?

  /**
   * Retrieves the total difficulty of the block header, if it has been computed.
   *
   * @param hash the hash of the header
   * @return the total difficulty of the header if it could be computed.
   */
  fun totalDifficulty(hash: Bytes): UInt256?

  /**
   * Retrieves the largest total difficulty value of the chain, if it has been computed.
   *
   */
  fun chainHeadTotalDifficulty(): UInt256?
}

/**
 * Indexer for blockchain elements.
 */
interface BlockchainIndexWriter {

  /**
   * Indexes a block header.
   *
   * @param blockHeader the block header to index
   */
  fun indexBlockHeader(blockHeader: BlockHeader, indexTotalDifficulty: Boolean = true): IndexResult

  /**
   * Indexes the total difficulty of a block header based on its position in the chain.
   *
   * @return true if the total difficulty was successfully computed, false otherwise.
   */
  fun indexTotalDifficulty(blockHeader: BlockHeader): Boolean

  /**
   * Indexes a transaction receipt.
   *
   * @param txReceipt the transaction receipt to index
   * @param txIndex the index of the transaction in the block
   * @param txHash the hash of the transaction
   * @param blockHash the hash of the block
   */
  fun indexTransactionReceipt(txReceipt: TransactionReceipt, txIndex: Int, txHash: Bytes, blockHash: Bytes)

  /**
   * Indexes a transaction.
   *
   * @param transaction the transaction to index
   */
  fun indexTransaction(transaction: Transaction)
}

/**
 * Exception thrown when an issue arises when reading the index.
 */
internal class IndexReadException(e: Exception) : RuntimeException(e)

/**
 * Exception thrown when an issue arises while writing to the index.
 */
internal class IndexWriteException(e: Exception) : RuntimeException(e)

/**
 * A Lucene-backed indexer capable of indexing blocks and block headers.
 */
class BlockchainIndex(private val indexWriter: IndexWriter) : BlockchainIndexWriter, BlockchainIndexReader {

  private val searcherManager: SearcherManager

  init {
    if (!indexWriter.isOpen) {
      throw IllegalArgumentException("Index writer should be opened")
    }
    try {
      searcherManager = SearcherManager(indexWriter, SearcherFactory())
    } catch (e: IOException) {
      throw UncheckedIOException(e)
    }
  }

  /**
   * Provides a function to index elements and committing them. If an exception is thrown in the function, the write is
   * rolled back.
   *
   * @param indexer function indexing data to be committed
   */
  fun index(indexer: (BlockchainIndexWriter) -> Unit) {
    try {
      indexer(this)
      try {
        indexWriter.commit()
        searcherManager.maybeRefresh()
      } catch (e: IOException) {
        throw IndexWriteException(e)
      }
    } catch (t: Throwable) {
      try {
        indexWriter.rollback()
      } catch (e: IOException) {
        throw IndexWriteException(e)
      }

      throw t
    }
  }

  /**
   * Provides a function to index elements and committing them. If an exception is thrown in the function, the write is
   * rolled back.
   *
   * @param indexer function indexing data to be committed
   * @return the result of the index
   */
  fun indexWithResult(indexer: (BlockchainIndexWriter) -> IndexResult): IndexResult {
    try {
      val result = indexer(this)
      try {
        indexWriter.commit()
        searcherManager.maybeRefresh()
      } catch (e: IOException) {
        throw IndexWriteException(e)
      }
      return result
    } catch (t: Throwable) {
      try {
        indexWriter.rollback()
      } catch (e: IOException) {
        throw IndexWriteException(e)
      }

      throw t
    }
  }

  override fun indexBlockHeader(blockHeader: BlockHeader, indexTotalDifficulty: Boolean): IndexResult {
    val document = mutableListOf<IndexableField>()
    val id = toBytesRef(blockHeader.getHash())
    document.add(StringField("_id", id, Field.Store.YES))
    document.add(StringField("_type", "block", Field.Store.NO))
    val parentHash = blockHeader.parentHash
    if (null != parentHash) {
      document += StringField(PARENT_HASH.fieldName, toBytesRef(parentHash), Field.Store.NO)
    }
    document += StringField(OMMERS_HASH.fieldName, toBytesRef(blockHeader.getOmmersHash()), Field.Store.NO)
    document += StringField(COINBASE.fieldName, toBytesRef(blockHeader.getCoinbase()), Field.Store.NO)
    document += StringField(STATE_ROOT.fieldName, toBytesRef(blockHeader.getStateRoot()), Field.Store.NO)
    document += StringField(DIFFICULTY.fieldName, toBytesRef(blockHeader.getDifficulty()), Field.Store.YES)
    document += StringField(NUMBER.fieldName, toBytesRef(blockHeader.getNumber()), Field.Store.NO)
    document += StringField(GAS_LIMIT.fieldName, toBytesRef(blockHeader.getGasLimit()), Field.Store.NO)
    document += StringField(GAS_USED.fieldName, toBytesRef(blockHeader.getGasUsed()), Field.Store.NO)
    document += StringField(EXTRA_DATA.fieldName, toBytesRef(blockHeader.getExtraData()), Field.Store.NO)
    document += NumericDocValuesField(TIMESTAMP.fieldName, blockHeader.getTimestamp().toEpochMilli())

    try {
      val query = BooleanQuery.Builder()
        .add(TermQuery(Term("_id", id)), BooleanClause.Occur.MUST)
        .add(TermQuery(Term("_type", "block")), BooleanClause.Occur.MUST)
      indexWriter.deleteDocuments(query.build())
      indexWriter.addDocument(document)
    } catch (e: IOException) {
      throw IndexWriteException(e)
    }
    val totalDifficultyFound = indexTotalDifficulty(blockHeader)
    return IndexResult(totalDifficultyFound)
  }

  override fun indexTotalDifficulty(blockHeader: BlockHeader): Boolean {
    val document = mutableListOf<Field>()
    val id = toBytesRef(blockHeader.hash)
    document.add(StringField("_id", id, Field.Store.YES))
    document.add(StringField("_type", "difficulty", Field.Store.NO))
    var totalDifficultyFound = false
    val diffBytes = blockHeader.getParentHash()?.let { hash ->
      val hashRef = toBytesRef(hash)
      document += StringField(
        PARENT_HASH.fieldName,
        hashRef,
        Field.Store.NO
      )
      queryDiffDocs(TermQuery(Term("_id", hashRef)), listOf(TOTAL_DIFFICULTY)).firstOrNull()?.let {
        it.getField(TOTAL_DIFFICULTY.fieldName)?.let {
          val totalDifficulty = blockHeader.getDifficulty().add(UInt256.fromBytes(Bytes.wrap(it.binaryValue().bytes)))
          totalDifficultyFound = true
          toBytesRef(totalDifficulty)
        }
      }
    } ?: run {
      if (blockHeader.number == UInt256.ZERO) {
        totalDifficultyFound = true
      }
      toBytesRef(blockHeader.difficulty)
    }
    document += StringField(TOTAL_DIFFICULTY.fieldName, diffBytes, Field.Store.YES)
    document += SortedDocValuesField(TOTAL_DIFFICULTY.fieldName, diffBytes)
    document += StoredField(TOTAL_DIFFICULTY.fieldName, diffBytes)
    try {
      val query = BooleanQuery.Builder()
        .add(TermQuery(Term("_id", id)), BooleanClause.Occur.MUST)
        .add(TermQuery(Term("_type", "difficulty")), BooleanClause.Occur.MUST)

      indexWriter.deleteDocuments(query.build())
      indexWriter.addDocument(document)
    } catch (e: IOException) {
      throw IndexWriteException(e)
    }
    return totalDifficultyFound
  }

  override fun indexTransaction(transaction: Transaction) {
    // TODO
  }

  override fun indexTransactionReceipt(txReceipt: TransactionReceipt, txIndex: Int, txHash: Bytes, blockHash: Bytes) {
    val document = mutableListOf<IndexableField>()
    val id = toBytesRef(txHash)
    document += StringField("_id", id, Field.Store.YES)
    document += StringField("_type", "txReceipt", Field.Store.NO)

    document += NumericDocValuesField(TransactionReceiptFields.INDEX.fieldName, txIndex.toLong())
    document += StringField(TransactionReceiptFields.TRANSACTION_HASH.fieldName, id, Field.Store.NO)
    document += StringField(
      TransactionReceiptFields.BLOCK_HASH.fieldName,
      toBytesRef(blockHash),
      Field.Store.NO
    )
    for (log in txReceipt.getLogs()) {
      document += StringField(TransactionReceiptFields.LOGGER.fieldName, toBytesRef(log.getLogger()), Field.Store.NO)
      for (logTopic in log.getTopics()) {
        document += StringField(TransactionReceiptFields.LOG_TOPIC.fieldName, toBytesRef(logTopic), Field.Store.NO)
      }
    }
    txReceipt.getStateRoot()?.let {
      document += StringField(TransactionReceiptFields.STATE_ROOT.fieldName, toBytesRef(it), Field.Store.NO)
    }
    document += StringField(
      TransactionReceiptFields.BLOOM_FILTER.fieldName,
      toBytesRef(txReceipt.getBloomFilter().toBytes()),
      Field.Store.NO
    )
    document += NumericDocValuesField(
      TransactionReceiptFields.CUMULATIVE_GAS_USED.fieldName,
      txReceipt.getCumulativeGasUsed()
    )
    txReceipt.getStatus()?.let {
      document += NumericDocValuesField(TransactionReceiptFields.STATUS.fieldName, it.toLong())
    }

    try {
      indexWriter.updateDocument(Term("_id", id), document)
    } catch (e: IOException) {
      throw IndexWriteException(e)
    }
  }

  private fun queryBlockDocs(query: Query): List<Document> = queryBlockDocs(query, emptyList())

  private fun queryTxReceiptDocs(query: Query): List<Document> = queryTxReceiptDocs(query, emptyList())

  private fun queryTxReceiptDocs(query: Query, fields: List<BlockHeaderFields>): List<Document> {
    val txQuery = BooleanQuery.Builder().add(
      query,
      BooleanClause.Occur.MUST
    )
      .add(TermQuery(Term("_type", "txReceipt")), BooleanClause.Occur.MUST).build()

    return search(txQuery, fields.map { it.fieldName })
  }

  private fun search(query: Query, fields: List<String>): List<Document> {
    var searcher: IndexSearcher? = null
    try {
      searcher = searcherManager.acquire()
      val topDocs = searcher!!.search(query, HITS)
      val docs = mutableListOf<Document>()
      if (topDocs.scoreDocs.isNotEmpty()) {
        val doc = searcher.doc(topDocs.scoreDocs.elementAt(0).doc, setOf("_id") + fields)
        docs += doc
      }
      return docs
    } catch (e: IOException) {
      throw IndexReadException(e)
    } finally {
      try {
        searcherManager.release(searcher)
      } catch (e: IOException) {
      }
    }
  }

  private fun queryBlockDocs(query: Query, fields: List<BlockHeaderFields>): List<Document> {
    val blockQuery = BooleanQuery.Builder().add(
      query,
      BooleanClause.Occur.MUST
    )
      .add(TermQuery(Term("_type", "block")), BooleanClause.Occur.MUST).build()

    return search(blockQuery, fields.map { it.fieldName })
  }

  private fun queryDiffDocs(query: Query, fields: List<BlockHeaderFields>): List<Document> {
    val blockQuery = BooleanQuery.Builder().add(
      query,
      BooleanClause.Occur.MUST
    )
      .add(TermQuery(Term("_type", "difficulty")), BooleanClause.Occur.MUST).build()

    return search(blockQuery, fields.map { it.fieldName })
  }

  private fun queryBlocks(query: Query): List<Hash> {
    val hashes = mutableListOf<Hash>()
    for (doc in queryBlockDocs(query)) {
      val bytes = doc.getBinaryValue("_id")
      hashes.add(Hash.fromBytes(Bytes32.wrap(bytes.bytes)))
    }
    return hashes
  }

  private fun queryTxReceipts(query: Query): List<Hash> {
    val hashes = mutableListOf<Hash>()
    for (doc in queryTxReceiptDocs(query)) {
      val bytes = doc.getBinaryValue("_id")
      hashes.add(Hash.fromBytes(Bytes32.wrap(bytes.bytes)))
    }
    return hashes
  }

  override fun findInRange(field: BlockHeaderFields, minValue: UInt256, maxValue: UInt256): List<Hash> {
    return queryBlocks(TermRangeQuery(field.fieldName, toBytesRef(minValue), toBytesRef(maxValue), true, true))
  }

  override fun findBy(field: BlockHeaderFields, value: Bytes): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findBy(field: BlockHeaderFields, value: Long): List<Hash> {
    return queryBlocks(NumericDocValuesField.newSlowExactQuery(field.fieldName, value))
  }

  override fun findLargestTotalDifficulty(): Hash? {
    var searcher: IndexSearcher? = null
    try {
      searcher = searcherManager.acquire()

      val topDocs = searcher!!.search(
        TermQuery(Term("_type", "difficulty")),
        HITS,
        Sort(SortField(TOTAL_DIFFICULTY.fieldName, SortField.Type.STRING, true))
      )
      if (topDocs.scoreDocs.isEmpty()) {
        return null
      }
      val doc = searcher.doc(topDocs.scoreDocs.elementAt(0).doc, setOf("_id"))
      val bytes = doc.getBinaryValue("_id")
      if (bytes != null) {
        return Hash.fromBytes(Bytes32.wrap(bytes.bytes))
      }
      return null
    } catch (e: IOException) {
      throw IndexReadException(e)
    } finally {
      try {
        searcherManager.release(searcher)
      } catch (e: IOException) {
      }
    }
  }

  override fun findByLargest(field: BlockHeaderFields): Hash? {
    var searcher: IndexSearcher? = null
    try {
      searcher = searcherManager.acquire()

      val topDocs = searcher!!.search(
        TermQuery(Term("_type", "block")),
        HITS,
        Sort(SortField.FIELD_SCORE, SortField(field.fieldName, SortField.Type.DOC, true))
      )
      if (topDocs.scoreDocs.isEmpty()) {
        return null
      }
      val doc = searcher.doc(topDocs.scoreDocs.elementAt(0).doc, setOf("_id"))
      val bytes = doc.getBinaryValue("_id")
      if (bytes != null) {
        return Hash.fromBytes(Bytes32.wrap(bytes.bytes))
      }
      return null
    } catch (e: IOException) {
      throw IndexReadException(e)
    } finally {
      try {
        searcherManager.release(searcher)
      } catch (e: IOException) {
      }
    }
  }

  override fun chainHeadTotalDifficulty(): UInt256 {
    var searcher: IndexSearcher? = null
    try {
      searcher = searcherManager.acquire()
      val topDocs = searcher!!.search(
        TermQuery(Term("_type", "block")),
        1,
        Sort(SortField.FIELD_SCORE, SortField(TOTAL_DIFFICULTY.fieldName, SortField.Type.DOC, true))
      )

      if (topDocs.scoreDocs.isEmpty()) {
        return UInt256.ZERO
      }

      val doc = searcher.doc(topDocs.scoreDocs[0].doc, setOf(TOTAL_DIFFICULTY.fieldName))
      val fieldValue = doc.getBinaryValue(TOTAL_DIFFICULTY.fieldName)

      return UInt256.fromBytes(Bytes32.wrap(fieldValue.bytes))
    } catch (e: IOException) {
      throw IndexReadException(e)
    } finally {
      try {
        searcherManager.release(searcher)
      } catch (e: IOException) {
      }
    }
  }

  override fun findBy(field: BlockHeaderFields, value: Gas): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findBy(field: BlockHeaderFields, value: UInt256): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findBy(field: BlockHeaderFields, value: Address): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findBy(field: BlockHeaderFields, value: Hash): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findInRange(field: TransactionReceiptFields, minValue: UInt256, maxValue: UInt256): List<Hash> {
    return queryBlocks(TermRangeQuery(field.fieldName, toBytesRef(minValue), toBytesRef(maxValue), true, true))
  }

  override fun findBy(field: TransactionReceiptFields, value: Bytes): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findBy(field: TransactionReceiptFields, value: Int): List<Hash> {
    return findBy(field, value.toLong())
  }

  override fun findBy(field: TransactionReceiptFields, value: Long): List<Hash> {
    return queryTxReceipts(NumericDocValuesField.newSlowExactQuery(field.fieldName, value))
  }

  override fun findByLargest(field: TransactionReceiptFields): Hash? {
    var searcher: IndexSearcher? = null
    try {
      searcher = searcherManager.acquire()
      val topDocs = searcher!!.search(
        TermQuery(Term("_type", "txReceipt")),
        HITS,
        Sort(SortField.FIELD_SCORE, SortField(field.fieldName, SortField.Type.DOC, false))
      )
      if (topDocs.scoreDocs.isEmpty()) {
        return null
      }
      val doc = searcher.doc(topDocs.scoreDocs.elementAt(0).doc, setOf("_id"))
      val bytes = doc.getBinaryValue("_id")
      if (bytes != null) {
        return Hash.fromBytes(Bytes32.wrap(bytes.bytes))
      }
      return null
    } catch (e: IOException) {
      throw IndexReadException(e)
    } finally {
      try {
        searcherManager.release(searcher)
      } catch (e: IOException) {
      }
    }
  }

  override fun findBy(field: TransactionReceiptFields, value: Gas): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findBy(field: TransactionReceiptFields, value: UInt256): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findBy(field: TransactionReceiptFields, value: Address): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findBy(field: TransactionReceiptFields, value: Hash): List<Hash> {
    return findByOneTerm(field, toBytesRef(value))
  }

  override fun findByBlockHashAndIndex(blockHash: Bytes, index: Int): Hash? {
    return queryTxReceipts(
      BooleanQuery.Builder()
        .add(
          TermQuery(Term(TransactionReceiptFields.BLOCK_HASH.fieldName, toBytesRef(blockHash))),
          BooleanClause.Occur.MUST
        )
        .add(
          NumericDocValuesField.newSlowExactQuery(TransactionReceiptFields.INDEX.fieldName, index.toLong()),
          BooleanClause.Occur.MUST
        ).build()
    ).firstOrNull()
  }

  override fun findByHashOrNumber(hashOrNumber: Bytes): List<Hash> {
    val query = BooleanQuery.Builder()
      .setMinimumNumberShouldMatch(1)
      .add(BooleanClause(TermQuery(Term("_id", toBytesRef(hashOrNumber))), BooleanClause.Occur.SHOULD))
      .add(
        BooleanClause(
          TermQuery(Term(NUMBER.fieldName, toBytesRef(hashOrNumber))),
          BooleanClause.Occur.SHOULD
        )
      )
      .build()
    return queryBlocks(query)
  }

  override fun totalDifficulty(hash: Bytes): UInt256? =
    queryDiffDocs(TermQuery(Term("_id", toBytesRef(hash))), listOf(TOTAL_DIFFICULTY)).firstOrNull()?.let {
      it.getField(TOTAL_DIFFICULTY.fieldName)?.binaryValue()?.bytes?.let { bytes ->
        UInt256.fromBytes(Bytes.wrap(bytes))
      }
    }

  private fun findByOneTerm(field: BlockHeaderFields, value: BytesRef): List<Hash> {
    return queryBlocks(TermQuery(Term(field.fieldName, value)))
  }

  private fun findByOneTerm(field: TransactionReceiptFields, value: BytesRef): List<Hash> {
    return queryTxReceipts(TermQuery(Term(field.fieldName, value)))
  }

  private fun toBytesRef(gas: Gas): BytesRef {
    return BytesRef(gas.toBytes().toArrayUnsafe())
  }

  private fun toBytesRef(bytes: Bytes): BytesRef {
    return BytesRef(bytes.toArrayUnsafe())
  }

  private fun toBytesRef(uint: UInt256): BytesRef {
    return toBytesRef(uint.toBytes())
  }

  companion object {

    private val HITS = 1
  }
}
