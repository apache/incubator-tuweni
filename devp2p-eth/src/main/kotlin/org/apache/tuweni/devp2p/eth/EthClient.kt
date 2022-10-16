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
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.ExpiringMap
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.eth.repository.TransactionPool
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.SubProtocolClient
import org.apache.tuweni.rlpx.wire.WireConnection
import org.apache.tuweni.units.bigints.UInt256
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger(EthClient::class.java)

/**
 * Client of the ETH subprotocol, allowing to request block and node data
 */
open class EthClient(
  private val service: RLPxService,
  private val pendingTransactionsPool: TransactionPool,
  private val connectionSelectionStrategy: ConnectionSelectionStrategy
) :
  EthRequestsManager, SubProtocolClient {

  private val headerRequests = ExpiringMap<String, Request<List<BlockHeader>>>(600000)
  private val bodiesRequests = ExpiringMap<String, Request<List<BlockBody>>>(600000)
  private val nodeDataRequests = ExpiringMap<String, Request<List<Bytes?>>>(600000)
  private val transactionReceiptRequests = ExpiringMap<String, Request<List<List<TransactionReceipt>>>>(600000)

  override fun connectionSelectionStrategy() = connectionSelectionStrategy

  override fun requestTransactionReceipts(
    blockHashes: List<Hash>,
    connection: WireConnection
  ): AsyncResult<List<List<TransactionReceipt>>> {
    val conns = service.repository().asIterable(EthSubprotocol.ETH62)
    val handle = AsyncResult.incomplete<List<List<TransactionReceipt>>>()
    var done = false
    conns.forEach { conn ->

      transactionReceiptRequests.computeIfAbsent(conn.uri()) {
        service.send(
          conn.agreedSubprotocolVersion(EthSubprotocol.ETH65.name()),
          MessageType.GetReceipts.code,
          conn,
          GetReceipts(blockHashes).toBytes()
        )
        done = true
        Request(conn.uri(), handle, blockHashes)
      }
      if (done) {
        return handle
      }
    }
    throw RuntimeException("No connection available")
  }

  override fun requestBlockHeaders(
    blockHash: Hash,
    maxHeaders: Long,
    skip: Long,
    reverse: Boolean,
    connection: WireConnection
  ): AsyncResult<List<BlockHeader>> {
    logger.info("Requesting headers hash: $blockHash maxHeaders: $maxHeaders skip: $skip reverse: $reverse with uri ${connection.uri()}")
    val conn = connectionSelectionStrategy.selectConnection()
    val completion = AsyncResult.incomplete<List<BlockHeader>>()
    headerRequests.computeIfAbsent(connection.uri() + blockHash.toHexString()) {
      service.send(
        conn.agreedSubprotocolVersion(EthSubprotocol.ETH65.name()),
        MessageType.GetBlockHeaders.code,
        conn,
        GetBlockHeaders(blockHash, maxHeaders, skip, reverse).toBytes()
      )
      Request(connectionId = conn.uri(), handle = completion, data = blockHash)
    }
    return completion
  }

  override fun requestBlockHeaders(
    blockNumber: Long,
    maxHeaders: Long,
    skip: Long,
    reverse: Boolean,
    connection: WireConnection
  ): AsyncResult<List<BlockHeader>> {
    val blockNumberBytes = UInt256.valueOf(blockNumber).toBytes()
    val completion = AsyncResult.incomplete<List<BlockHeader>>()
    headerRequests.computeIfAbsent(connection.uri() + blockNumberBytes.toHexString()) {
      service.send(
        connection.agreedSubprotocolVersion(EthSubprotocol.ETH65.name()),
        MessageType.GetBlockHeaders.code,
        connection,
        GetBlockHeaders(blockNumberBytes, maxHeaders, skip, reverse).toBytes()
      )
      Request(connectionId = connection.uri(), handle = completion, data = blockNumber)
    }
    return completion
  }

  override fun requestBlockHeaders(
    blockHashes: List<Hash>,
    connection: WireConnection
  ): AsyncResult<List<BlockHeader>> {
    return AsyncResult.combine(blockHashes.stream().map { requestBlockHeader(it) })
  }

  override fun requestBlockHeader(blockHash: Hash, connection: WireConnection): AsyncResult<BlockHeader> {
    val request = headerRequests.computeIfAbsent(connection.uri() + blockHash.toHexString()) {
      service.send(
        connection.agreedSubprotocolVersion(EthSubprotocol.ETH62.name()),
        MessageType.GetBlockHeaders.code,
        connection,
        GetBlockHeaders(blockHash, 1, 0, false).toBytes()
      )
      val completion = AsyncResult.incomplete<List<BlockHeader>>()
      Request(connectionId = connection.uri(), handle = completion, data = blockHash)
    }
    return request.handle.thenApply { it?.firstOrNull() }
  }

  override fun requestBlockBodies(blockHashes: List<Hash>, connection: WireConnection): AsyncResult<List<BlockBody>> {
    val handle = AsyncResult.incomplete<List<BlockBody>>()
    var oldRequestPresent = true
    val oldRequest = bodiesRequests.computeIfAbsent(connection.uri()) {
      oldRequestPresent = false
      service.send(
        connection.agreedSubprotocolVersion(EthSubprotocol.ETH65.name()),
        MessageType.GetBlockBodies.code,
        connection,
        GetBlockBodies(blockHashes).toBytes()
      )
      Request(connection.uri(), handle, blockHashes)
    }
    if (oldRequestPresent) {
      oldRequest.handle.thenRun {
        requestBlockBodies(blockHashes, connection).thenAccept() {
          handle.complete(it)
        }
      }
    }
    return handle
  }

  override fun requestBlock(blockHash: Hash, connection: WireConnection): AsyncResult<Block> {
    val headerRequest = requestBlockHeader(blockHash)
    val bodyRequest = requestBlockBodies(listOf(blockHash))

    val result = AsyncResult.incomplete<Block>()
    headerRequest.thenCombine(bodyRequest) { header, body ->
      if (body.size == 1) {
        result.complete(Block(header, body[0]))
      } else {
        result.completeExceptionally(NullPointerException("No block body found"))
      }
    }

    return result
  }

  @Suppress("TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING")
  fun headersRequested(
    connection: WireConnection,
    blockHeader: BlockHeader
  ): Request<List<BlockHeader>>? = headerRequests.remove(connection.uri() + blockHeader.hash.toHexString())
    ?: headerRequests.remove(connection.uri() + blockHeader.number.toHexString())

  fun wasRequested(connection: WireConnection): Request<List<BlockBody>>? =
    bodiesRequests.remove(connection.uri())

  fun nodeDataWasRequested(connection: WireConnection): Request<List<Bytes?>>? =
    nodeDataRequests[connection.uri()]

  fun transactionReceiptsRequested(connection: WireConnection): Request<List<List<TransactionReceipt>>>? = transactionReceiptRequests[connection.uri()]

  override suspend fun submitPooledTransaction(vararg tx: Transaction) {
    for (t in tx) {
      pendingTransactionsPool.add(t)
    }
    val hashes = tx.map { it.hash }
    val conns = service.repository().asIterable(EthSubprotocol.ETH65)
    conns.forEach { conn ->
      service.send(
        conn.agreedSubprotocolVersion(EthSubprotocol.ETH65.name()),
        MessageType.NewPooledTransactionHashes.code,
        conn,
        GetBlockBodies(hashes).toBytes()
      )
    }
  }
}
