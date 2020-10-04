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
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.TransactionPool
import org.apache.tuweni.rlpx.wire.WireConnection

/**
 * Controller managing the state of the ETH or LES subprotocol handlers.
 */
class EthController(
  val repository: BlockchainRepository,
  val pendingTransactionsPool: TransactionPool,
  val requestsManager: EthRequestsManager,
  val connectionsListener: (WireConnection, Status) -> Unit = { _, _ -> }
) {

  suspend fun findTransactionReceipts(hashes: List<Hash>): List<List<TransactionReceipt>> {
    val receipts = ArrayList<List<TransactionReceipt>>()
    hashes.forEach {
      receipts.add(repository.retrieveTransactionReceipts(it))
    }
    return receipts
  }

  suspend fun addNewBlock(newBlock: Block) {
    repository.storeBlock(newBlock)
  }

  suspend fun findBlockBodies(hashes: List<Hash>): List<BlockBody> {
    val bodies = ArrayList<BlockBody>()
    hashes.forEach { hash ->
      repository.retrieveBlockBody(hash)?.let {
        bodies.add(it)
      }
    }
    return bodies
  }

  suspend fun findHeaders(block: Bytes, maxHeaders: Long, skip: Long, reverse: Boolean): List<BlockHeader> {
    val matches = repository.findBlockByHashOrNumber(block)
    val headers = ArrayList<BlockHeader>()
    if (matches.isNotEmpty()) {
      val header = repository.retrieveBlockHeader(matches[0])
      header?.let {
        headers.add(it)
        var blockNumber = it.number
        for (i in 2..maxHeaders) {
          blockNumber = if (reverse) {
            blockNumber.subtract(skip + 1)
          } else {
            blockNumber.add(skip + 1)
          }
          val nextMatches = repository.findBlockByHashOrNumber(blockNumber.toBytes())
          if (nextMatches.isEmpty()) {
            break
          }
          val nextHeader = repository.retrieveBlockHeader(nextMatches[0]) ?: break
          headers.add(nextHeader)
        }
      }
    }
    return headers
  }

  suspend fun addNewBlockHashes(hashes: List<Pair<Hash, Long>>) {
    hashes.forEach { pair ->
      repository.retrieveBlockHeader(pair.first).takeIf { null == it }.apply {
        requestBlockHeader(pair.first)
      }
      repository.retrieveBlockBody(pair.first).takeIf { null == it }.apply {
        requestBlockBody(pair.first)
      }
    }
  }

  fun requestBlockHeader(blockHash: Hash) {
    requestsManager.requestBlockHeader(blockHash)
  }

  private fun requestBlockBody(blockHash: Hash) {
    requestsManager.requestBlockBodies(listOf(blockHash))
  }

  suspend fun addNewBlockHeaders(connection: WireConnection, headers: List<BlockHeader>) {
    val handle = requestsManager.wasRequested(connection, headers.first()) ?: return
    val bodiesToRequest = mutableListOf<Hash>()
    val headersToRequest = mutableListOf<Hash>()
    headers.forEach { header ->
      repository.storeBlockHeader(header)
      if (!repository.hasBlockBody(header.hash)) {
        bodiesToRequest.add(header.hash)
      }
      header.parentHash?.let {
        if (!repository.hasBlockHeader(it)) {
          headersToRequest.add(it)
        }
      }
    }
    requestsManager.requestBlockHeaders(headersToRequest)
    requestsManager.requestBlockBodies(bodiesToRequest)
    handle.complete()
  }

  suspend fun addNewBlockBodies(connection: WireConnection, bodies: List<BlockBody>) {
    val request = requestsManager.wasRequested(connection, bodies)
    if (request != null) {
      val hashes = request.data as List<*>
      for (i in 0..hashes.size) {
        repository.storeBlockBody(hashes[i] as Hash, bodies[i])
      }
    }
  }

  fun receiveStatus(connection: WireConnection, status: Status) {
    connectionsListener(connection, status)
  }

  suspend fun findNodeData(hashes: List<Hash>) = repository.retrieveNodeData(hashes)

  suspend fun addNewNodeData(connection: WireConnection, elements: List<Bytes?>) {
    val request = requestsManager.nodeDataWasRequested(connection, elements)
    if (request != null) {
      val hashes = request.data as List<*>
      for (i in 0..hashes.size) {
        val elt = elements[i]
        if (elt != null) {
          repository.storeNodeData(hashes[i] as Hash, elt)
        }
      }
    }
  }

  suspend fun addNewTransactionReceipts(
    connection: WireConnection,
    transactionReceipts: List<List<TransactionReceipt>>
  ) {
    val request = requestsManager.transactionReceiptsRequested(connection, transactionReceipts)
    if (request != null) {
      val hashes = request.data as List<*>
      for (i in 0..hashes.size) {
        val blockBody = repository.retrieveBlockBody(hashes[i] as Hash)
        val blockReceipts = transactionReceipts[i]
        for (j in 0..blockReceipts.size) {
          repository.storeTransactionReceipt(
            blockReceipts[j],
            j,
            blockBody?.transactions?.get(j)?.hash ?: Bytes.EMPTY,
            hashes[i] as Hash
          )
        }
      }
    }
  }

  suspend fun addNewTransactions(transactions: List<Transaction>) {
    transactions.forEach {
      repository.storeTransaction(it)
    }
  }

  suspend fun addNewPooledTransactions(transactions: List<Transaction>) {
    for (tx in transactions) {
      pendingTransactionsPool.add(tx)
    }
  }

  suspend fun findPooledTransactions(hashes: List<Hash>): List<Transaction> {
    val result = ArrayList<Transaction>()
    for (hash in hashes) {
      pendingTransactionsPool.get(hash)?.let {
        result.add(it)
      }
    }
    return result
  }
}
