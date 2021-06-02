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

import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.CompletableAsyncResult
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.rlpx.wire.WireConnection

/**
 * Requests manager used to request and check requests of block data
 */
interface EthRequestsManager {

  /**
   * Strategy to pick a connection.
   * @return the connection selection strategy
   */
  fun connectionSelectionStrategy(): ConnectionSelectionStrategy

  /**
   * Requests a block header
   * @param blockHash the block hash
   * @param connection the connection to use
   * @return a handle to the completion of the operation
   */
  fun requestBlockHeader(
    blockHash: Hash,
    connection: WireConnection = connectionSelectionStrategy().selectConnection()
  ): AsyncResult<BlockHeader>

  /**
   * Requests block headers
   * @param blockHashes the block hashes
   * @param connection the connection to use
   * @return a handle to the completion of the operation
   */
  fun requestBlockHeaders(
    blockHashes: List<Hash>,
    connection: WireConnection = connectionSelectionStrategy().selectConnection()
  ): AsyncResult<List<BlockHeader>>

  /**
   * Requests block headers
   * @param blockHash the hash of the block
   * @param maxHeaders the max number of headers to provide
   * @param skip the headers to skip in between each header provided
   * @param reverse whether to provide headers in forward or backwards order
   * @param connection the connection to use
   * @return a handle to the completion of the operation
   */
  fun requestBlockHeaders(
    blockHash: Hash,
    maxHeaders: Long,
    skip: Long,
    reverse: Boolean,
    connection: WireConnection = connectionSelectionStrategy().selectConnection()
  ): AsyncResult<List<BlockHeader>>

  /**
   * Requests block headers
   * @param blockNumber the number of the block
   * @param maxHeaders the max number of headers to provide
   * @param skip the headers to skip in between each header provided
   * @param reverse whether to provide headers in forward or backwards order
   * @param connection the connection to use
   * @return a handle to the completion of the operation
   */
  fun requestBlockHeaders(
    blockNumber: Long,
    maxHeaders: Long,
    skip: Long,
    reverse: Boolean,
    connection: WireConnection = connectionSelectionStrategy().selectConnection()
  ): AsyncResult<List<BlockHeader>>

  /**
   * Requests block bodies from block hashes
   * @param blockHashes the hashes of the blocks
   * @param connection the connection to use
   * @return a handle to the completion of the operation
   */
  fun requestBlockBodies(
    blockHashes: List<Hash>,
    connection: WireConnection = connectionSelectionStrategy().selectConnection()
  ): AsyncResult<List<BlockBody>>

  /**
   * Requests a block from block hash
   * @param blockHash the hash of the block
   * @param connection the connection to use
   * @return a handle to the completion of the operation
   */
  fun requestBlock(
    blockHash: Hash,
    connection: WireConnection = connectionSelectionStrategy().selectConnection()
  ): AsyncResult<Block>

  /**
   * Requests transaction receipts
   * @param blockHashes the hashes to request transaction receipts for
   * @param connection the connection to use
   * @return a handle to the completion of the operation
   */
  fun requestTransactionReceipts(
    blockHashes: List<Hash>,
    connection: WireConnection = connectionSelectionStrategy().selectConnection()
  ): AsyncResult<List<List<TransactionReceipt>>>

  /**
   * Submits a new pending transaction to the transaction pool to be gossiped to peers.
   * @param tx a new transaction
   */
  suspend fun submitPooledTransaction(vararg tx: Transaction)
}

/**
 * A data request handle, matching the connection on which the request was made.
 * @param connectionId the identifier of the connection
 * @param handle the handle to the request completion
 * @param data data associated with the request
 */
data class Request<T>(val connectionId: String, val handle: CompletableAsyncResult<T>, val data: Any)
