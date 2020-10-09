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
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.CompletableAsyncCompletion
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

/**
 * Client of the ETH subprotocol, allowing to request block and node data
 */
class EthClient(private val service: RLPxService, private val pendingTransactionsPool: TransactionPool) :
  EthRequestsManager, SubProtocolClient {

  private val headerRequests = HashMap<Bytes32, Request>()
  private val bodiesRequests = HashMap<String, Request>()
  private val nodeDataRequests = HashMap<String, Request>()
  private val transactionReceiptRequests = HashMap<String, Request>()

  override fun requestTransactionReceipts(blockHashes: List<Hash>): AsyncCompletion {
    val conns = service.repository().asIterable(EthSubprotocol.ETH62)
    val handle = AsyncCompletion.incomplete()
    var done = false
    conns.forEach { conn ->

      transactionReceiptRequests.computeIfAbsent(conn.uri()) {
        service.send(
          EthSubprotocol.ETH62,
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

  override fun requestBlockHeaders(blockHash: Hash, maxHeaders: Long, skip: Long, reverse: Boolean): AsyncCompletion {
    val conn = service.repository().asIterable(EthSubprotocol.ETH62).firstOrNull()
    val completion = AsyncCompletion.incomplete()
    headerRequests.computeIfAbsent(blockHash) {
      service.send(
        EthSubprotocol.ETH62,
        MessageType.GetBlockHeaders.code,
        conn!!,
        GetBlockHeaders(blockHash, maxHeaders, skip, reverse).toBytes()
      )
      Request(connectionId = conn.uri(), handle = completion, data = blockHash)
    }
    return completion
  }

  override fun requestBlockHeaders(blockNumber: Long, maxHeaders: Long, skip: Long, reverse: Boolean): AsyncCompletion {
    val conn = service.repository().asIterable(EthSubprotocol.ETH62).firstOrNull()
    val blockNumberBytes = UInt256.valueOf(blockNumber).toBytes()
    val completion = AsyncCompletion.incomplete()
    headerRequests.computeIfAbsent(blockNumberBytes) {
      service.send(
        EthSubprotocol.ETH62,
        MessageType.GetBlockHeaders.code,
        conn!!,
        GetBlockHeaders(blockNumberBytes, maxHeaders, skip, reverse).toBytes()
      )
      Request(connectionId = conn.uri(), handle = completion, data = blockNumber)
    }
    return completion
  }

  override fun requestBlockHeaders(blockHashes: List<Hash>): AsyncCompletion {
    return AsyncCompletion.allOf(blockHashes.stream().map { requestBlockHeader(it) })
  }

  override fun requestBlockHeader(blockHash: Hash): AsyncCompletion {
    val conn = service.repository().asIterable(EthSubprotocol.ETH62).firstOrNull()

    val request = headerRequests.computeIfAbsent(blockHash) {
      service.send(
        EthSubprotocol.ETH62,
        MessageType.GetBlockHeaders.code,
        conn!!,
        GetBlockHeaders(blockHash, 1, 0, false).toBytes()
      )
      val completion = AsyncCompletion.incomplete()
      Request(connectionId = conn.uri(), handle = completion, data = blockHash)
    }
    return request.handle
  }

  override fun requestBlockBodies(blockHashes: List<Hash>): AsyncCompletion {
    val conns = service.repository().asIterable(EthSubprotocol.ETH62)
    val handle = AsyncCompletion.incomplete()
    conns.forEach { conn ->
      var done = false
      bodiesRequests.computeIfAbsent(conn.uri()) {
        service.send(
          EthSubprotocol.ETH62,
          MessageType.GetBlockBodies.code,
          conn,
          GetBlockBodies(blockHashes).toBytes()
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

  override fun requestBlock(blockHash: Hash): AsyncCompletion = AsyncCompletion.allOf(
    requestBlockHeader(blockHash),
    requestBlockBodies(listOf(blockHash))
  )

  override fun wasRequested(connection: WireConnection, header: BlockHeader): CompletableAsyncCompletion? {
    val request = headerRequests.remove(header.hash) ?: return null
    if (request.connectionId == connection.uri()) {
      return request.handle
    } else {
      return null
    }
  }

  override fun wasRequested(connection: WireConnection, bodies: List<BlockBody>): Request? =
    bodiesRequests[connection.uri()]

  override fun nodeDataWasRequested(connection: WireConnection, elements: List<Bytes?>): Request? =
    nodeDataRequests[connection.uri()]

  override fun transactionReceiptsRequested(
    connection: WireConnection,
    transactionReceipts: List<List<TransactionReceipt>>
  ): Request? = transactionReceiptRequests[connection.uri()]

  override suspend fun submitPooledTransaction(vararg tx: Transaction) {
    for (t in tx) { pendingTransactionsPool.add(t) }
    val hashes = tx.map { it.hash }
    val conns = service.repository().asIterable(EthSubprotocol.ETH65)
    conns.forEach { conn ->
      service.send(
        EthSubprotocol.ETH65,
        MessageType.NewPooledTransactionHashes.code,
        conn,
        GetBlockBodies(hashes).toBytes()
      )
    }
  }
}
