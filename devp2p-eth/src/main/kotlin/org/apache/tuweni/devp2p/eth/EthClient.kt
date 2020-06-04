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

import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.CompletableAsyncCompletion
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.SubProtocolClient
import org.apache.tuweni.units.bigints.UInt256

class EthClient(private val service: RLPxService) : EthRequestsManager, SubProtocolClient {

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
      BlockHeaderRequest(connectionId = conn.id(), handle = completion)
    }
    return completion
  }

  private val headerRequests = HashMap<Bytes32, BlockHeaderRequest>()
  private val bodiesRequests = HashMap<String, List<Hash>>()

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
      BlockHeaderRequest(connectionId = conn.id(), handle = completion)
    }
    return completion
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
      BlockHeaderRequest(connectionId = conn.id(), handle = completion)
    }
    return completion
  }

  override fun requestBlockBodies(blockHashes: List<Hash>) {
    val conns = service.repository().asIterable(EthSubprotocol.ETH62)
    conns.forEach { conn ->
      if (bodiesRequests.computeIfAbsent(conn.id()) {
        service.send(
          EthSubprotocol.ETH62,
          MessageType.GetBlockBodies.code,
          conn.id(),
          GetBlockBodies(blockHashes).toBytes()
        )
        blockHashes
      } == blockHashes) {
        return
      }
    }
  }

  override fun requestBlock(blockHash: Hash) {
    requestBlockHeader(blockHash)
    requestBlockBodies(listOf(blockHash))
  }

  override fun wasRequested(connectionId: String, header: BlockHeader): CompletableAsyncCompletion? {
    val request = headerRequests.remove(header.hash) ?: return null
    if (request.connectionId == connectionId) {
      return request.handle
    } else {
      return null
    }
  }

  override fun wasRequested(connectionId: String, bodies: List<BlockBody>): List<Hash>? =
    bodiesRequests.get(connectionId)
}

internal data class BlockHeaderRequest(val connectionId: String, val handle: CompletableAsyncCompletion)
