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
package org.apache.tuweni.les

import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.CompletableAsyncCompletion
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.SubProtocolClient
import org.apache.tuweni.units.bigints.UInt256
import java.util.concurrent.atomic.AtomicLong

import org.apache.tuweni.les.GetBlockHeadersMessage.BlockHeaderQuery.Direction.BACKWARDS
import org.apache.tuweni.les.GetBlockHeadersMessage.BlockHeaderQuery.Direction.FORWARD

/**
 * Calls to LES functions from the point of view of the consumer of the subprotocol.
 *
 * When executing those calls, the client will store all data transferred in the blockchain repository.
 *
 */
interface LightClient : SubProtocolClient {

  /**
   * Get block headers from remote peers.
   *
   * @param blockNumberOrHash the block number or the hash to start to look for headers from
   * @param maxHeaders maximum number of headers to return
   * @param skip the number of block apart to skip when returning headers
   * @param reverse if true, walk the chain in descending order
   */
  fun getBlockHeaders(
    blockNumberOrHash: Bytes32,
    maxHeaders: Int = 10,
    skip: Int = 0,
    reverse: Boolean = false
  ): AsyncCompletion

  /**
   * Get block bodies from remote peers.
   *
   * @param blockHashes hashes identifying block bodies
   */
  fun getBlockBodies(vararg blockHashes: Hash): AsyncCompletion

  /**
   * Get transaction receipts from remote peers for blocks.
   *
   * @param blockHashes hashes identifying blocks
   */
  fun getReceipts(vararg blockHashes: Hash): AsyncCompletion
}

internal class LightClientImpl(private val service: RLPxService) : LightClient {

  private val counter = AtomicLong()
  private val headerRequests = HashMap<Bytes32, Request>()
  private val bodiesRequests = HashMap<String, Request>()
  private val transactionReceiptRequests = HashMap<String, Request>()

  override fun getBlockHeaders(
    blockNumberOrHash: Bytes32,
    maxHeaders: Int,
    skip: Int,
    reverse: Boolean
  ): AsyncCompletion {
    val conn = service.repository().asIterable(LESSubprotocol.LES_ID).firstOrNull()

    val request = headerRequests.computeIfAbsent(blockNumberOrHash) {
      val query = GetBlockHeadersMessage.BlockHeaderQuery(
        blockNumberOrHash,
        UInt256.valueOf(maxHeaders.toLong()),
        UInt256.valueOf(skip.toLong()),
        if (reverse) BACKWARDS else FORWARD
      )

      service.send(
        LESSubprotocol.LES_ID,
        2,
        conn!!,
        GetBlockHeadersMessage(counter.incrementAndGet(), listOf(query)).toBytes()
      )
      val completion = AsyncCompletion.incomplete()
      Request(connectionId = conn.uri(), handle = completion, data = blockNumberOrHash)
    }
    return request.handle
  }

  override fun getBlockBodies(vararg blockHashes: Hash): AsyncCompletion {
    val conns = service.repository().asIterable(LESSubprotocol.LES_ID)
    val handle = AsyncCompletion.incomplete()
    conns.forEach { conn ->
      var done = false
      bodiesRequests.computeIfAbsent(conn.uri()) {
        service.send(
          LESSubprotocol.LES_ID,
          4,
          conn,
          GetBlockBodiesMessage(counter.incrementAndGet(), blockHashes.asList()).toBytes()
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

  override fun getReceipts(vararg blockHashes: Hash): AsyncCompletion {
    val conns = service.repository().asIterable(LESSubprotocol.LES_ID)
    val handle = AsyncCompletion.incomplete()
    var done = false
    conns.forEach { conn ->

      transactionReceiptRequests.computeIfAbsent(conn.uri()) {
        service.send(
          LESSubprotocol.LES_ID,
          6,
          conn,
          GetReceiptsMessage(counter.incrementAndGet(), blockHashes.asList()).toBytes()
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
}

data class Request(val connectionId: String, val handle: CompletableAsyncCompletion, val data: Any)
