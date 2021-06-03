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

import io.opentelemetry.api.metrics.Meter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.ByteBuffersDirectory
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.AccountState
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.kv.KeyValueStore
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.trie.MerkleStorage
import org.apache.tuweni.trie.StoredMerklePatriciaTrie
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * Repository housing blockchain information.
 *
 * This repository allows storing blocks, block headers and metadata about the blockchain, such as forks and head
 * information.
 *
 * @param chainMetadata the key-value store to store chain metadata
 * @param blockBodyStore the key-value store to store block bodies
 * @param blockHeaderStore the key-value store to store block headers
 * @param transactionReceiptStore the key-value store to store transaction receipts
 * @param transactionStore the key-value store to store transactions
 * @param stateStore the key-value store to store the global state
 * @param blockchainIndex the blockchain index to index values
 * @param meter an optional metering provider to watch metrics in the repository
 */
class BlockchainRepository(
  private val chainMetadata: KeyValueStore<Bytes, Bytes>,
  private val blockBodyStore: KeyValueStore<Bytes, Bytes>,
  private val blockHeaderStore: KeyValueStore<Bytes, Bytes>,
  private val transactionReceiptStore: KeyValueStore<Bytes, Bytes>,
  private val transactionStore: KeyValueStore<Bytes, Bytes>,
  private val stateStore: KeyValueStore<Bytes, Bytes>,
  private val blockchainIndex: BlockchainIndex,
  private val meter: Meter? = null,
  override val coroutineContext: CoroutineContext = Dispatchers.Default,
) : CoroutineScope {

  companion object {

    internal val logger = LoggerFactory.getLogger(BlockchainRepository::class.java)
    internal val GENESIS_BLOCK = Bytes.wrap("genesisBlock".toByteArray())

    /**
     * Constructs a blockchain repository that resides entirely in heap.
     *
     * @return an in-memory repository
     */
    fun inMemory(): BlockchainRepository {
      val analyzer = StandardAnalyzer()
      val config = IndexWriterConfig(analyzer)
      val writer = IndexWriter(ByteBuffersDirectory(), config)
      return BlockchainRepository(
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        BlockchainIndex(writer),
      )
    }

    /**
     * Initializes a blockchain repository with metadata, placing it in key-value stores.
     *
     * @return a new blockchain repository made from the metadata passed in parameter.
     */
    suspend fun init(
      blockBodyStore: KeyValueStore<Bytes, Bytes>,
      blockHeaderStore: KeyValueStore<Bytes, Bytes>,
      chainMetadata: KeyValueStore<Bytes, Bytes>,
      transactionReceiptsStore: KeyValueStore<Bytes, Bytes>,
      transactionStore: KeyValueStore<Bytes, Bytes>,
      stateStore: KeyValueStore<Bytes, Bytes>,
      blockchainIndex: BlockchainIndex,
      genesisBlock: Block,
      meter: Meter? = null,
    ): BlockchainRepository {
      val repo = BlockchainRepository(
        chainMetadata,
        blockBodyStore,
        blockHeaderStore,
        transactionReceiptsStore,
        transactionStore,
        stateStore,
        blockchainIndex,
        meter
      )
      repo.setGenesisBlock(genesisBlock)
      repo.storeBlock(genesisBlock)
      return repo
    }
  }

  val blockHeaderListeners = mutableMapOf<String, (BlockHeader) -> Unit>()
  val blocksStoredCounter =
    meter?.longCounterBuilder("blocks_stored")?.setDescription("Number of blocks stored")?.build()
  val blockHeadersStoredCounter =
    meter?.longCounterBuilder("block_headers_stored")?.setDescription("Number of block headers stored")?.build()
  val blockBodiesStoredCounter =
    meter?.longCounterBuilder("blocks_bodies_stored")?.setDescription("Number of block bodies stored")?.build()
  var indexing = true

  /**
   * Stores a block body into the repository.
   *
   * @param blockBody the block body to store
   */
  suspend fun storeBlockBody(blockHash: Hash, blockBody: BlockBody) {
    blockBodiesStoredCounter?.add(1)
    blockBodyStore.put(blockHash, blockBody.toBytes())
  }

  /**
   * Stores state node data into the repository.
   *
   * @param bytes the node data to store
   * @return a handle to the storage operation completion
   */
  suspend fun storeNodeData(hash: Hash, bytes: Bytes) {
    stateStore.put(hash, bytes)
  }

  /**
   * Stores a block into the repository.
   *
   * @param block the block to store
   * @return a handle to the storage operation completion
   */
  suspend fun storeBlock(block: Block) {
    blocksStoredCounter?.add(1)
    storeBlockBody(block.getHeader().getHash(), block.getBody())
    blockHeaderStore.put(block.getHeader().getHash(), block.getHeader().toBytes())
    indexBlockHeader(block.getHeader())
  }

  /**
   * Stores a transaction receipt in the repository.
   *
   * @param transactionReceipt the transaction receipt to store
   * @param txIndex the index of the transaction in the block
   * @param txHash the hash of the transaction
   * @param blockHash the hash of the block that this transaction belongs to
   */
  suspend fun storeTransactionReceipt(
    transactionReceipt: TransactionReceipt,
    txIndex: Int,
    txHash: Bytes,
    blockHash: Bytes,
  ) {
    transactionReceiptStore.put(txHash, transactionReceipt.toBytes())
    indexTransactionReceipt(transactionReceipt, txIndex, txHash, blockHash)
  }

  /**
   * Stores a block header in the repository.
   *
   * @param header the block header to store
   * @return handle to the storage operation completion
   */
  suspend fun storeBlockHeader(header: BlockHeader) {
    blockHeadersStoredCounter?.add(1)
    blockHeaderStore.put(header.hash, header.toBytes())
    if (indexing) {
      indexBlockHeader(header)
    }
    logger.debug("Stored header {} {}", header.number, header.hash)
    blockHeaderListeners.values.forEach {
      it(header)
    }
  }

  fun addBlockHeaderListener(listener: (BlockHeader) -> Unit): String {
    val uuid = UUID.randomUUID().toString()
    blockHeaderListeners[uuid] = listener
    return uuid
  }

  fun removeBlockHeaderListener(listenerId: String) {
    blockHeaderListeners.remove(listenerId)
  }

  suspend fun indexBlockHeader(header: BlockHeader) {
    logger.info("Indexing ${header.number} ${header.hash}")
    blockchainIndex.index { writer -> writer.indexBlockHeader(header, indexing) }
  }

  suspend fun reIndexTotalDifficulty() {
    val header = retrieveGenesisBlock().header
    blockchainIndex.index { writer ->
      runBlocking {
        reIndexTotalDifficultyInternal(writer, header)
      }
    }
  }

  private suspend fun reIndexTotalDifficultyInternal(writer: BlockchainIndexWriter, header: BlockHeader) {
    writer.indexTotalDifficulty(header)

    findBlocksByParentHash(header.getHash()).map { hash ->
      coroutineScope {
        async {
          blockHeaderStore.get(hash)?.let { bytes ->
            reIndexTotalDifficultyInternal(writer, BlockHeader.fromBytes(bytes))
          }
        }
      }
    }.awaitAll()
  }

  private suspend fun indexTransactionReceipt(
    txReceipt: TransactionReceipt,
    txIndex: Int,
    txHash: Bytes,
    blockHash: Bytes,
  ) {
    blockchainIndex.index {
      it.indexTransactionReceipt(txReceipt, txIndex, txHash, blockHash)
    }
  }

  /**
   * Retrieves a block body into the repository as its serialized RLP bytes representation.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the bytes if found
   */
  suspend fun retrieveBlockBodyBytes(blockHash: Bytes): Bytes? {
    return blockBodyStore.get(blockHash)
  }

  /**
   * Retrieves a block body into the repository.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the block if found
   */
  suspend fun retrieveBlockBody(blockHash: Bytes): BlockBody? {
    return retrieveBlockBodyBytes(blockHash)?.let { BlockBody.fromBytes(it) }
  }

  /**
   * Returns true if the store contains the block body.
   *
   * @param blockHash the hash of the block stored
   * @return a future with a boolean result
   */
  suspend fun hasBlockBody(blockHash: Bytes): Boolean {
    return blockBodyStore.containsKey(blockHash)
  }

  /**
   * Retrieves a block into the repository.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the block if found
   */
  suspend fun retrieveBlock(blockHash: Bytes): Block? {
    return retrieveBlockBody(blockHash)?.let { body ->
      this.retrieveBlockHeader(blockHash)?.let { Block(it, body) }
    } ?: return null
  }

  /**
   * Retrieves a block header into the repository as its serialized RLP bytes representation.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the block header bytes if found
   */
  suspend fun retrieveBlockHeaderBytes(blockHash: Bytes): Bytes? {
    return blockHeaderStore.get(blockHash)
  }

  /**
   * Returns true if the store contains the block header.
   *
   * @param blockHash the hash of the block stored
   * @return a future with a boolean result
   */
  suspend fun hasBlockHeader(blockHash: Bytes): Boolean {
    return blockHeaderStore.containsKey(blockHash)
  }

  /**
   * Retrieves a block header into the repository.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the block header if found
   */
  suspend fun retrieveBlockHeader(blockHash: Bytes): BlockHeader? {
    val bytes = retrieveBlockHeaderBytes(blockHash) ?: return null
    return BlockHeader.fromBytes(bytes)
  }

  /**
   * Retrieves the block identified as the chain head.
   *
   * @return the current chain head, or the genesis block if no chain head is present.
   */
  suspend fun retrieveChainHead(): Block {
    return blockchainIndex.findByLargest(BlockHeaderFields.TOTAL_DIFFICULTY)
      ?.let { retrieveBlock(it) } ?: retrieveGenesisBlock()
  }

  /**
   * Retrieves the block header identified as the chain head.
   *
   * @return the current chain head header, or the genesis block if no chain head is present.
   */
  suspend fun retrieveChainHeadHeader(): BlockHeader {
    return blockchainIndex.findByLargest(BlockHeaderFields.TOTAL_DIFFICULTY)
      ?.let { retrieveBlockHeader(it) } ?: retrieveGenesisBlock().getHeader()
  }

  /**
   * Retrieves the block identified as the genesis block
   *
   * @return the genesis block
   */
  suspend fun retrieveGenesisBlock(): Block {
    return chainMetadata.get(GENESIS_BLOCK).let { retrieveBlock(it!!)!! }
  }

  /**
   * Retrieves all transaction receipts associated with a block.
   *
   * @param blockHash the hash of the block
   * @return all transaction receipts associated with a block, in the correct order
   */
  suspend fun retrieveTransactionReceipts(blockHash: Bytes): List<TransactionReceipt> {
    return blockchainIndex.findBy(TransactionReceiptFields.BLOCK_HASH, blockHash).mapNotNull {
      transactionReceiptStore.get(it)?.let { TransactionReceipt.fromBytes(it) }
    }
  }

  /**
   * Retrieves a transaction receipt associated with a block and an index
   * @param blockHash the hash of the block
   * @param index the index of the transaction in the block
   */
  suspend fun retrieveTransactionReceipt(blockHash: Bytes, index: Int): TransactionReceipt? {
    return blockchainIndex.findByBlockHashAndIndex(blockHash, index)?.let {
      transactionReceiptStore.get(it)?.let { TransactionReceipt.fromBytes(it) }
    }
  }

  /**
   * Retrieves a transaction receipt associated with a block and an index
   * @param txHash the hash of the transaction
   */
  suspend fun retrieveTransactionReceipt(txHash: Hash): TransactionReceipt? {
    return transactionReceiptStore.get(txHash)?.let { TransactionReceipt.fromBytes(it) }
  }

  /**
   * Finds a block according to the bytes, which can be a block number or block hash.
   *
   * @param blockNumberOrBlockHash the number or hash of the block
   * @return the matching blocks
   */
  fun findBlockByHashOrNumber(blockNumberOrBlockHash: Bytes): List<Hash> {
    return blockchainIndex.findByHashOrNumber(blockNumberOrBlockHash)
  }

  /**
   * Finds hashes of blocks which have a matching parent hash.
   *
   * @param parentHash the parent hash
   * @return the matching blocks
   */
  fun findBlocksByParentHash(parentHash: Bytes): List<Hash> {
    return blockchainIndex.findBy(BlockHeaderFields.PARENT_HASH, parentHash)
  }

  private suspend fun setGenesisBlock(block: Block) {
    return chainMetadata
      .put(GENESIS_BLOCK, block.getHeader().getHash())
  }

  /**
   * Retrieves data, sending back exactly the list requested. If data is missing, the list entry is null.
   * @param hashes the hashes of data to retrieve
   * @return the data retrieved
   */
  suspend fun retrieveNodeData(hashes: List<Hash>): List<Bytes?> {
    return hashes.map {
      stateStore.get(it)
    }
  }

  /**
   * Stores a transaction.
   *
   * @param transaction the transaction to store
   */
  suspend fun storeTransaction(transaction: Transaction) {
    transactionStore.put(transaction.hash, transaction.toBytes())
    blockchainIndex.indexTransaction(transaction)
  }

  /**
   * Stores an account state for a given account.
   *
   * @param address the address of the account
   * @param account the account's state
   */
  suspend fun storeAccount(address: Address, account: AccountState) =
    stateStore.put(Hash.hash(address), account.toBytes())

  /**
   * Retrieves an account state for a given account.
   *
   * @param address the address of the account
   * @return the account's state, or null if not found
   */
  suspend fun getAccount(address: Address): AccountState? =
    stateStore.get(Hash.hash(address))?.let { AccountState.fromBytes(it) }

  /**
   * Checks if a given account is stored in the repository.
   *
   * @param address the address of the account
   * @return true if the accounts exists
   */
  suspend fun accountsExists(address: Address): Boolean = stateStore.containsKey(Hash.hash(address))

  /**
   * Gets a value stored in an account store, or null if the account doesn't exist.
   *
   * @param address the address of the account
   * @param key the key of the value to retrieve in the account storage.
   */
  suspend fun getAccountStoreValue(address: Address, key: Bytes32): Bytes32? {
    logger.trace("Entering getAccountStoreValue")
    val accountStateBytes = stateStore.get(Hash.hash(address)) ?: return null
    val accountState = AccountState.fromBytes(accountStateBytes)
    val tree = StoredMerklePatriciaTrie.storingBytes32(
      object : MerkleStorage {
        override suspend fun get(hash: Bytes32): Bytes? {
          return stateStore.get(hash)
        }

        override suspend fun put(hash: Bytes32, content: Bytes) {
          stateStore.put(hash, content)
        }
      },
      accountState.storageRoot
    )
    return tree.get(key)
  }

  /**
   * Gets the code of an account
   *
   * @param address the address of the account
   * @return the code or null if the address doesn't exist or the code is not present.
   */
  suspend fun getAccountCode(address: Address): Bytes? {
    val accountStateBytes = stateStore.get(Hash.hash(address))
    if (accountStateBytes == null) {
      return null
    }
    val accountState = AccountState.fromBytes(accountStateBytes)
    return stateStore.get(accountState.codeHash)
  }

  /**
   * Closes the repository.
   */
  fun close() {
    blockBodyStore.close()
    blockHeaderStore.close()
    chainMetadata.close()
    stateStore.close()
    transactionStore.close()
    transactionReceiptStore.close()
  }

  /**
   * Stores account code in world state
   *
   * @param code the code to store
   */
  suspend fun storeCode(code: Bytes) {
    stateStore.put(Hash.hash(code), code)
  }
}
