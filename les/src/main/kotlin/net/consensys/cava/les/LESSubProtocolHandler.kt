/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.les

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.coroutines.asyncCompletion
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.DisconnectReason
import org.apache.tuweni.rlpx.wire.SubProtocolHandler
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier
import org.apache.tuweni.units.bigints.UInt256
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

internal class LESSubProtocolHandler(
  private val service: RLPxService,
  private val subProtocolIdentifier: SubProtocolIdentifier,
  private val networkId: Int,
  private val serveHeaders: Boolean,
  private val serveChainSince: UInt256,
  private val serveStateSince: UInt256,
  private val flowControlBufferLimit: UInt256,
  private val flowControlMaximumRequestCostTable: UInt256,
  private val flowControlMinimumRateOfRecharge: UInt256,
  private val repo: BlockchainRepository,
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : SubProtocolHandler, CoroutineScope {

  private val peerStateMap = ConcurrentHashMap<String, LESPeerState>()

  override fun handle(connectionId: String, messageType: Int, message: Bytes): AsyncCompletion {
    return asyncCompletion {
      val state = peerStateMap.computeIfAbsent(connectionId) { LESPeerState() }
      if (messageType == 0) {
        if (state.handshakeComplete()) {
          service.disconnect(connectionId, DisconnectReason.PROTOCOL_BREACH)
          throw IllegalStateException("Handshake message sent after handshake completed")
        }
        state.peerStatusMessage = StatusMessage.read(message)
      } else {
        if (!state.handshakeComplete()) {
          service.disconnect(connectionId, DisconnectReason.PROTOCOL_BREACH)
          throw IllegalStateException("Message sent before handshake completed")
        }
        if (messageType == 1) {
          throw UnsupportedOperationException()
        } else if (messageType == 2) {
          val getBlockHeadersMessage = GetBlockHeadersMessage.read(message)
          handleGetBlockHeaders(connectionId, getBlockHeadersMessage)
        } else if (messageType == 3) {
          val blockHeadersMessage = BlockHeadersMessage.read(message)
          handleBlockHeadersMessage(blockHeadersMessage)
        } else if (messageType == 4) {
          val blockBodiesMessage = GetBlockBodiesMessage.read(message)
          handleGetBlockBodiesMessage(connectionId, blockBodiesMessage)
        } else if (messageType == 5) {
          val blockBodiesMessage = BlockBodiesMessage.read(message)
          handleBlockBodiesMessage(state, blockBodiesMessage)
        } else if (messageType == 6) {
          val getReceiptsMessage = GetReceiptsMessage.read(message)
          handleGetReceiptsMessage(connectionId, getReceiptsMessage)
        } else {
          throw UnsupportedOperationException()
        }
      }
    }
  }

  private suspend fun handleGetReceiptsMessage(
    connectionId: String,
    receiptsMessage: GetReceiptsMessage
  ) {
    val receipts = ArrayList<List<TransactionReceipt>>()
    for (blockHash in receiptsMessage.blockHashes) {
      repo.retrieveTransactionReceipts(blockHash).let { transactionReceipts ->
        receipts.add(transactionReceipts.filterNotNull())
      }
    }
    return service.send(
      subProtocolIdentifier,
      5,
      connectionId,
      ReceiptsMessage(receiptsMessage.reqID, 0, receipts).toBytes()
    )
  }

  private suspend fun handleGetBlockBodiesMessage(
    connectionId: String,
    blockBodiesMessage: GetBlockBodiesMessage
  ) {
    val bodies = ArrayList<BlockBody>()
    for (blockHash in blockBodiesMessage.blockHashes) {
      repo.retrieveBlock(blockHash)?.let { block ->
        bodies.add(block.body())
      }
    }
    return service.send(
        subProtocolIdentifier,
        5,
        connectionId,
        BlockBodiesMessage(blockBodiesMessage.reqID, 0, bodies).toBytes()
      )
  }

  private suspend fun handleBlockBodiesMessage(
    state: LESPeerState,
    blockBodiesMessage: BlockBodiesMessage
  ) {
    for (index in 0..blockBodiesMessage.blockBodies.size) {
      state.requestsCache[blockBodiesMessage.reqID]?.get(index)?.let {
        repo.storeBlockBody(it, blockBodiesMessage.blockBodies[index])
      }
    }
  }

  private suspend fun handleBlockHeadersMessage(blockHeadersMessage: BlockHeadersMessage) {
    for (header in blockHeadersMessage.blockHeaders) {
      repo.storeBlockHeader(header)
    }
  }

  private suspend fun handleGetBlockHeaders(
    connectionId: String,
    getBlockHeadersMessage: GetBlockHeadersMessage
  ) {
    val headersFound = TreeSet<BlockHeader>()
    for (query in getBlockHeadersMessage.queries) {
      val hashes = repo.findBlockByHashOrNumber(query.blockNumberOrBlockHash)
      for (h in hashes) {
        repo.retrieveBlockHeader(h)?.let { header ->
            headersFound.add(header)
        }
      }
    }
    service.send(
        subProtocolIdentifier,
        3,
        connectionId,
        BlockHeadersMessage(getBlockHeadersMessage.reqID, 0L, ArrayList(headersFound)).toBytes()
      )
  }

  override fun handleNewPeerConnection(connectionId: String): AsyncCompletion {
    return asyncCompletion {
        val head = repo.retrieveChainHead()!!
        val genesis = repo.retrieveGenesisBlock()!!
        val headTd = head.header().difficulty()
        val headHash = head.header().hash()
        val state = peerStateMap.computeIfAbsent(connectionId) { LESPeerState() }
        state.ourStatusMessage = StatusMessage(
          subProtocolIdentifier.version(),
          networkId,
          headTd,
          headHash.toBytes(),
          head.header().number(),
          genesis.header().hash().toBytes(),
          serveHeaders,
          serveChainSince,
          serveStateSince,
          false,
          flowControlBufferLimit,
          flowControlMaximumRequestCostTable,
          flowControlMinimumRateOfRecharge,
          0
        )
        service.send(subProtocolIdentifier, 0, connectionId, state.ourStatusMessage!!.toBytes())
    }
  }

  override fun stop(): AsyncCompletion {
    return AsyncCompletion.completed()
  }
}
