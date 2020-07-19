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
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.CompletableAsyncCompletion
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.rlpx.wire.WireConnection

/**
 * Requests manager used to request and check requests of block data
 */
interface EthRequestsManager {
  /**
   * Requests a block header
   * @param blockHash the block hash
   * @return a handle to the completion of the operation
   */
  fun requestBlockHeader(blockHash: Hash): AsyncCompletion
  /**
   * Requests block headers
   * @param blockHashes the block hashes
   * @return a handle to the completion of the operation
   */
  fun requestBlockHeaders(blockHashes: List<Hash>): AsyncCompletion
  /**
   * Requests block headers
   * @param blockHash the hash of the block
   * @param maxHeaders the max number of headers to provide
   * @param skip the headers to skip in between each header provided
   * @param reverse whether to provide headers in forward or backwards order
   * @return a handle to the completion of the operation
   */
  fun requestBlockHeaders(blockHash: Hash, maxHeaders: Long, skip: Long, reverse: Boolean): AsyncCompletion
  /**
   * Requests block headers
   * @param blockNumber the number of the block
   * @param maxHeaders the max number of headers to provide
   * @param skip the headers to skip in between each header provided
   * @param reverse whether to provide headers in forward or backwards order
   * @return a handle to the completion of the operation
   */
  fun requestBlockHeaders(blockNumber: Long, maxHeaders: Long, skip: Long, reverse: Boolean): AsyncCompletion
  /**
   * Requests block bodies from block hashes
   * @param blockHashes the hashes of the blocks
   * @return a handle to the completion of the operation
   */
  fun requestBlockBodies(blockHashes: List<Hash>): AsyncCompletion
  /**
   * Requests a block from block hash
   * @param blockHash the hash of the block
   * @return a handle to the completion of the operation
   */
  fun requestBlock(blockHash: Hash): AsyncCompletion
  /**
   * Requests transaction receipts
   * @param blockHashes the hashes to request transaction receipts for
   * @return a handle to the completion of the operation
   */
  fun requestTransactionReceipts(blockHashes: List<Hash>): AsyncCompletion
  /**
   * Checks if a request was made to get block headers
   * @param connection the wire connection sending data
   * @param header the block header just received
   * @return a handle to the completion of the operation, or null if no such request was placed
   */
  fun wasRequested(connection: WireConnection, header: BlockHeader): CompletableAsyncCompletion?
  /**
   * Checks if a request was made to get block bodies
   * @param connection the wire connection sending data
   * @param bodies the bodies just received
   * @return a handle to the completion of the operation, with metadata, or null if no such request was placed
   */
  fun wasRequested(connection: WireConnection, bodies: List<BlockBody>): Request?
  /**
   * Checks if a request was made to get node data
   * @param connection the wire connection sending data
   * @param elements the data just received
   * @return a handle to the completion of the operation, with metadata, or null if no such request was placed
   */
  fun nodeDataWasRequested(connection: WireConnection, elements: List<Bytes?>): Request?
  /**
   * Checks if a request was made to get transaction receipts
   * @param connection the wire connection sending data
   * @param transactionReceipts the transaction receipts just received
   * @return a handle to the completion of the operation, with metadata, or null if no such request was placed
   */
  fun transactionReceiptsRequested(
    connection: WireConnection,
    transactionReceipts: List<List<TransactionReceipt>>
  ): Request?
}

/**
 * A data request handle, matching the connection on which the request was made.
 * @param connectionId the identifier of the connection
 * @param handle the handle to the request completion
 * @param data data associated with the request
 */
data class Request(val connectionId: String, val handle: CompletableAsyncCompletion, val data: Any)
