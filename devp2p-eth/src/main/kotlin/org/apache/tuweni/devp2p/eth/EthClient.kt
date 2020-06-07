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
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.SubProtocolClient
import org.apache.tuweni.units.bigints.UInt256

class EthClient(private val service: RLPxService) : EthRequestsManager, SubProtocolClient {

  private val headerRequests = HashMap<Bytes32, Request>()
  private val bodiesRequests = HashMap<String, Request>()
  private val nodeDataRequests = HashMap<String, Request>()
  private val transactionReceiptRequests = HashMap<String, Request>()

  override fun requestTransactionReceipts(blockHashes: List<Hash>): AsyncCompletion {
    val conns = service.repository().asIterable(EthSubprotocol.ETH62)
    val handle = AsyncCompletion.incomplete()
    conns.forEach { conn ->
      var done = false
      transactionReceiptRequests.computeIfAbsent(conn.id()) {
        service.send(
          EthSubprotocol.ETH62,
          MessageType.GetReceipts.code,
          conn.id(),
          GetReceipts(blockHashes).toBytes()
        )
        done = true
        Request(conn.id(), handle, blockHashes)
      }
      return handle
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
        conn!!.id(),
        GetBlockHeaders(blockHash, maxHeaders, skip, reverse).toBytes()
      )
      Request(connectionId = conn.id(), handle = completion, data = blockHash)
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
        conn!!.id(),
        GetBlockHeaders(blockNumberBytes, maxHeaders, skip, reverse).toBytes()
      )
      Request(connectionId = conn.id(), handle = completion, data = blockNumber)
    }
    return completion
  }

  override fun requestBlockHeaders(blockHashes: List<Hash>): AsyncCompletion {
    return AsyncCompletion.allOf(blockHashes.stream().map { requestBlockHeader(it) })
  }

  override fun requestBlockHeader(blockHash: Hash): AsyncCompletion {
    val conn = service.repository().asIterable(EthSubprotocol.ETH62).firstOrNull()
    val completion = AsyncCompletion.incomplete()
    headerRequests.computeIfAbsent(blockHash) {
      service.send(
        EthSubprotocol.ETH62,
        MessageType.GetBlockHeaders.code,
        conn!!.id(),
        GetBlockHeaders(blockHash, 1, 0, false).toBytes()
      )
      Request(connectionId = conn.id(), handle = completion, data = blockHash)
    }
    return completion
  }

  override fun requestBlockBodies(blockHashes: List<Hash>): AsyncCompletion {
    val conns = service.repository().asIterable(EthSubprotocol.ETH62)
    val handle = AsyncCompletion.incomplete()
    conns.forEach { conn ->
      var done = false
      bodiesRequests.computeIfAbsent(conn.id()) {
        service.send(
          EthSubprotocol.ETH62,
          MessageType.GetBlockBodies.code,
          conn.id(),
          GetBlockBodies(blockHashes).toBytes()
        )
        done = true
        Request(conn.id(), handle, blockHashes)
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

  override fun wasRequested(connectionId: String, header: BlockHeader): CompletableAsyncCompletion? {
    val request = headerRequests.remove(header.hash) ?: return null
    if (request.connectionId == connectionId) {
      return request.handle
    } else {
      return null
    }
  }

  override fun wasRequested(connectionId: String, bodies: List<BlockBody>): Request? =
    bodiesRequests[connectionId]

  override fun nodeDataWasRequested(connectionId: String, elements: List<Bytes?>): Request? =
    nodeDataRequests[connectionId]

  override fun transactionRequestsWasRequested(
    connectionId: String,
    transactionReceipts: List<List<TransactionReceipt>>
  ): Request? = transactionReceiptRequests[connectionId]
}

data class Request(val connectionId: String, val handle: CompletableAsyncCompletion, val data: Any)
