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

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.kv.KeyValueStore

/**
 * Repository housing blockchain information.
 *
 * This repository allows storing blocks, block headers and metadata about the blockchain, such as forks and head
 * information.
 */
class BlockchainRepository
/**
 * Default constructor.
 *
 * @param chainMetadata the key-value store to store chain metadata
 * @param blockBodyStore the key-value store to store block bodies
 * @param blockHeaderStore the key-value store to store block headers
 * @param blockchainIndex the blockchain index to index values
 */(
   private val chainMetadata: KeyValueStore<Bytes, Bytes>,
   private val blockBodyStore: KeyValueStore<Bytes, Bytes>,
   private val blockHeaderStore: KeyValueStore<Bytes, Bytes>,
   private val transactionReceiptsStore: KeyValueStore<Bytes, Bytes>,
   private val blockchainIndex: BlockchainIndex
 ) {

  companion object {

    val GENESIS_BLOCK = Bytes.wrap("genesisBlock".toByteArray())

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
      blockchainIndex: BlockchainIndex,
      genesisBlock: Block
    ): BlockchainRepository {
      val repo = BlockchainRepository(chainMetadata,
        blockBodyStore,
        blockHeaderStore,
        transactionReceiptsStore,
        blockchainIndex)
      repo.setGenesisBlock(genesisBlock)
      repo.storeBlock(genesisBlock)
      return repo
    }
  }

  /**
   * Stores a block body into the repository.
   *
   * @param blockBody the block body to store
   * @return a handle to the storage operation completion
   */
  suspend fun storeBlockBody(blockHash: Hash, blockBody: BlockBody) {
    blockBodyStore.put(blockHash, blockBody.toBytes())
  }

  /**
   * Stores a block into the repository.
   *
   * @param block the block to store
   * @return a handle to the storage operation completion
   */
  suspend fun storeBlock(block: Block) {
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
    blockHash: Bytes
  ) {
    transactionReceiptsStore.put(txHash, transactionReceipt.toBytes())
    indexTransactionReceipt(transactionReceipt, txIndex, txHash, blockHash)
  }

  /**
   * Stores a block header in the repository.
   *
   * @param header the block header to store
   * @return handle to the storage operation completion
   */
  suspend fun storeBlockHeader(header: BlockHeader) {
    blockHeaderStore.put(header.hash, header.toBytes())
    indexBlockHeader(header)
  }

  private suspend fun indexBlockHeader(header: BlockHeader) {
    blockchainIndex.index { writer -> writer.indexBlockHeader(header) }
    for (hash in findBlocksByParentHash(header.getHash())) {
      blockHeaderStore.get(hash)?.let { bytes ->
        indexBlockHeader(BlockHeader.fromBytes(bytes))
      }
    }
  }

  private suspend fun indexTransactionReceipt(
    txReceipt: TransactionReceipt,
    txIndex: Int,
    txHash: Bytes,
    blockHash: Bytes
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
   * Retrieves a block into the repository.
   *
   * @param blockHash the hash of the block stored
   * @return a future with the block if found
   */
  suspend fun retrieveBlock(blockHash: Bytes): Block? {
    return retrieveBlockBody(blockHash)?.let {
        body -> this.retrieveBlockHeader(blockHash)?.let { Block(it, body) }
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
   * Retrieves the block identified as the chain head
   *
   * @return the current chain head, or the genesis block if no chain head is present.
   */
  suspend fun retrieveChainHead(): Block? {
    return blockchainIndex.findByLargest(BlockHeaderFields.TOTAL_DIFFICULTY)
      ?.let { retrieveBlock(it) } ?: retrieveGenesisBlock()
  }

  /**
   * Retrieves the block header identified as the chain head
   *
   * @return the current chain head header, or the genesis block if no chain head is present.
   */
  suspend fun retrieveChainHeadHeader(): BlockHeader? {
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
      transactionReceiptsStore.get(it)?.let { TransactionReceipt.fromBytes(it) }
    }
  }

  /**
   * Retrieves a transaction receipt associated with a block and an index
   * @param blockHash the hash of the block
   * @param index the index of the transaction in the block
   */
  suspend fun retrieveTransactionReceipt(blockHash: Bytes, index: Int): TransactionReceipt? {
    return blockchainIndex.findByBlockHashAndIndex(blockHash, index)?.let {
      transactionReceiptsStore.get(it)?.let { TransactionReceipt.fromBytes(it) }
    }
  }

  /**
   * Retrieves a transaction receipt associated with a block and an index
   * @param txHash the hash of the transaction
   */
  suspend fun retrieveTransactionReceipt(txHash: Hash): TransactionReceipt? {
    return transactionReceiptsStore.get(txHash)?.let { TransactionReceipt.fromBytes(it) }
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
}
