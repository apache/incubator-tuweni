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
import org.apache.tuweni.concurrent.CompletableAsyncResult
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.eth.repository.TransactionPool
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.SubProtocolClient
import org.apache.tuweni.rlpx.wire.WireConnection
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.bigints.UInt64

/**
 * Client of the ETH subprotocol, allowing to request block and node data
 */
class EthClient66(
  private val service: RLPxService,
  private val pendingTransactionsPool: TransactionPool,
  private val connectionSelectionStrategy: ConnectionSelectionStrategy,
) :
  EthRequestsManager, SubProtocolClient {

  private val headerRequests = mutableMapOf<Bytes, Request<List<BlockHeader>>>()
  private val bodiesRequests = HashMap<Bytes, Request<List<BlockBody>>>()
  private val nodeDataRequests = HashMap<Bytes, Request<List<Bytes?>>>()
  private val transactionReceiptRequests = HashMap<Bytes, Request<List<List<TransactionReceipt>>>>()

  override fun connectionSelectionStrategy() = connectionSelectionStrategy

  override fun requestTransactionReceipts(
    blockHashes: List<Hash>,
    connection: WireConnection?,
  ): AsyncResult<List<List<TransactionReceipt>>> {
    val conns = service.repository().asIterable(EthSubprotocol.ETH62)
    val handle = AsyncResult.incomplete<List<List<TransactionReceipt>>>()
    var done = false
    conns.forEach { conn ->
      transactionReceiptRequests.computeIfAbsent(UInt64.random().toBytes()) { key ->
        service.send(
          EthSubprotocol.ETH62,
          MessageType.GetReceipts.code,
          conn,
          RLP.encodeList {
            it.writeValue(key)
            it.writeRLP(GetReceipts(blockHashes).toBytes())
          }
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
    connection: WireConnection?,
  ): AsyncResult<List<BlockHeader>> {
    logger.info("Requesting headers hash: $blockHash maxHeaders: $maxHeaders skip: $skip reverse: $reverse")
    val conn = connectionSelectionStrategy.selectConnection()
    val completion = AsyncResult.incomplete<List<BlockHeader>>()
    headerRequests.computeIfAbsent(UInt64.random().toBytes()) { key ->
      service.send(
        EthSubprotocol.ETH62,
        MessageType.GetBlockHeaders.code,
        conn!!,
        RLP.encodeList {
          it.writeValue(key)
          it.writeRLP(GetBlockHeaders(blockHash, maxHeaders, skip, reverse).toBytes())
        }
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
    connection: WireConnection?,
  ): AsyncResult<List<BlockHeader>> {
    val blockNumberBytes = UInt256.valueOf(blockNumber)
    val completion = AsyncResult.incomplete<List<BlockHeader>>()
    headerRequests.computeIfAbsent(UInt64.random().toBytes()) { key ->
      service.send(
        EthSubprotocol.ETH62,
        MessageType.GetBlockHeaders.code,
        connection!!,
        RLP.encodeList {
          it.writeValue(key)
          it.writeRLP(GetBlockHeaders(blockNumberBytes, maxHeaders, skip, reverse).toBytes())
        }

      )
      Request(connectionId = connection.uri(), handle = completion, data = blockNumber)
    }
    return completion
  }

  override fun requestBlockHeaders(
    blockHashes: List<Hash>,
    connection: WireConnection?,
  ): AsyncResult<List<BlockHeader>> {
    return AsyncResult.combine(blockHashes.stream().map { requestBlockHeader(it) })
  }

  override fun requestBlockHeader(blockHash: Hash, connection: WireConnection?): AsyncResult<BlockHeader> {
    val request = headerRequests.computeIfAbsent(UInt64.random().toBytes()) { key ->
      service.send(
        EthSubprotocol.ETH62,
        MessageType.GetBlockHeaders.code,
        connection!!,
        RLP.encodeList {
          it.writeValue(key)
          it.writeRLP(GetBlockHeaders(blockHash, 1, 0, false).toBytes())
        }
      )
      val completion = AsyncResult.incomplete<List<BlockHeader>>()
      Request(connectionId = connection.uri(), handle = completion, data = blockHash)
    }
    return request.handle.thenApply { it?.firstOrNull() }
  }

  override fun requestBlockBodies(blockHashes: List<Hash>, connection: WireConnection?): AsyncResult<List<BlockBody>> {
    val handle = AsyncResult.incomplete<List<BlockBody>>()
    bodiesRequests.compute(UInt64.random().toBytes()) { key, _ ->
      service.send(
        EthSubprotocol.ETH62,
        MessageType.GetBlockBodies.code,
        connection!!,
        RLP.encodeList {
          it.writeValue(key)
          it.writeRLP(GetBlockBodies(blockHashes).toBytes())
        }
      )
      Request(connection.uri(), handle, blockHashes)
    }
    return handle
  }

  override fun requestBlock(blockHash: Hash, connection: WireConnection?): AsyncResult<Block> {
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

  fun headersRequested(
    requestIdentifier: Bytes,
  ): CompletableAsyncResult<List<BlockHeader>>? {
    val request = headerRequests.remove(requestIdentifier) ?: return null
    return request.handle
  }

  fun bodiesRequested(requestIdentifier: Bytes): Request<List<BlockBody>>? =
    bodiesRequests[requestIdentifier]

  fun nodeDataWasRequested(requestIdentifier: Bytes): Request<List<Bytes?>>? =
    nodeDataRequests[requestIdentifier]

  fun transactionReceiptsRequested(
    requestIdentifier: Bytes,
  ): Request<List<List<TransactionReceipt>>>? = transactionReceiptRequests[requestIdentifier]

  override suspend fun submitPooledTransaction(vararg tx: Transaction) {
    for (t in tx) {
      pendingTransactionsPool.add(t)
    }
    val hashes = tx.map { it.hash }
    val conns = service.repository().asIterable(EthSubprotocol.ETH66)
    conns.forEach { conn ->
      service.send(
        EthSubprotocol.ETH66,
        MessageType.NewPooledTransactionHashes.code,
        conn,
        RLP.encodeList {
          it.writeValue(UInt64.random().toBytes())
          it.writeRLP(GetBlockBodies(hashes).toBytes())
        }
      )
    }

    val conns65 = service.repository().asIterable(EthSubprotocol.ETH65)
    conns65.forEach { conn ->
      service.send(
        EthSubprotocol.ETH65,
        MessageType.NewPooledTransactionHashes.code,
        conn,
        GetBlockBodies(hashes).toBytes()
      )
    }
  }
}
