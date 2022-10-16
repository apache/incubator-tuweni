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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.coroutines.asyncCompletion
import org.apache.tuweni.devp2p.eth.BlockchainInformation
import org.apache.tuweni.devp2p.eth.EthController
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.DisconnectReason
import org.apache.tuweni.rlpx.wire.SubProtocolHandler
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier
import org.apache.tuweni.rlpx.wire.WireConnection
import org.apache.tuweni.units.bigints.UInt256
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

internal class LESSubProtocolHandler(
  private val service: RLPxService,
  private val subProtocolIdentifier: SubProtocolIdentifier,
  private val blockchainInfo: BlockchainInformation,
  private val serveHeaders: Boolean,
  private val serveChainSince: UInt256,
  private val serveStateSince: UInt256,
  private val flowControlBufferLimit: UInt256,
  private val flowControlMaximumRequestCostTable: UInt256,
  private val flowControlMinimumRateOfRecharge: UInt256,
  private val controller: EthController,
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : SubProtocolHandler, CoroutineScope {

  private val peerStateMap = ConcurrentHashMap<String, LESPeerState>()

  override fun handle(connection: WireConnection, messageType: Int, message: Bytes): AsyncCompletion {
    return asyncCompletion {
      val state = peerStateMap.computeIfAbsent(connection.uri()) { LESPeerState() }
      if (messageType == 0) {
        if (state.handshakeComplete()) {
          peerStateMap.remove(connection.uri())
          state.ready.cancel()
          service.disconnect(connection, DisconnectReason.PROTOCOL_BREACH)
          throw IllegalStateException("Handshake message sent after handshake completed")
        }
        val peerStatus = StatusMessage.read(message)
        state.peerStatusMessage = peerStatus
        state.ready.complete()
        controller.receiveStatus(connection, peerStatus.toStatus())
      } else {
        if (!state.handshakeComplete()) {
          peerStateMap.remove(connection.uri())
          state.ready.cancel()
          service.disconnect(connection, DisconnectReason.PROTOCOL_BREACH)
          throw IllegalStateException("Message sent before handshake completed")
        }
        if (messageType == 1) {
          throw UnsupportedOperationException()
        } else if (messageType == 2) {
          val getBlockHeadersMessage = GetBlockHeadersMessage.read(message)
          handleGetBlockHeaders(connection, getBlockHeadersMessage)
        } else if (messageType == 3) {
          val blockHeadersMessage = BlockHeadersMessage.read(message)
          handleBlockHeadersMessage(connection, blockHeadersMessage)
        } else if (messageType == 4) {
          val blockBodiesMessage = GetBlockBodiesMessage.read(message)
          handleGetBlockBodiesMessage(connection, blockBodiesMessage)
        } else if (messageType == 5) {
          val blockBodiesMessage = BlockBodiesMessage.read(message)
          handleBlockBodiesMessage(connection, blockBodiesMessage)
        } else if (messageType == 6) {
          val getReceiptsMessage = GetReceiptsMessage.read(message)
          handleGetReceiptsMessage(connection, getReceiptsMessage)
        } else {
          peerStateMap.remove(connection.uri())
          state.ready.cancel()
          service.disconnect(connection, DisconnectReason.PROTOCOL_BREACH)
        }
      }
    }
  }

  private suspend fun handleGetReceiptsMessage(
    connection: WireConnection,
    receiptsMessage: GetReceiptsMessage
  ) {
    val receipts = controller.findTransactionReceipts(receiptsMessage.blockHashes)
    return service.send(
      subProtocolIdentifier,
      5,
      connection,
      ReceiptsMessage(receiptsMessage.reqID, 0, receipts).toBytes()
    )
  }

  private suspend fun handleGetBlockBodiesMessage(
    connection: WireConnection,
    blockBodiesMessage: GetBlockBodiesMessage
  ) {
    val bodies = controller.findBlockBodies(blockBodiesMessage.blockHashes)
    return service.send(
      subProtocolIdentifier,
      5,
      connection,
      BlockBodiesMessage(blockBodiesMessage.reqID, 0, bodies).toBytes()
    )
  }

  private suspend fun handleBlockBodiesMessage(
    conn: WireConnection,
    blockBodiesMessage: BlockBodiesMessage
  ) {
    controller.addNewBlockBodies(conn, null, blockBodiesMessage.blockBodies)
  }

  private suspend fun handleBlockHeadersMessage(connection: WireConnection, blockHeadersMessage: BlockHeadersMessage) {
    controller.addNewBlockHeaders(connection, null, blockHeadersMessage.blockHeaders)
  }

  private suspend fun handleGetBlockHeaders(
    connection: WireConnection,
    getBlockHeadersMessage: GetBlockHeadersMessage
  ) {
    val headersFound = TreeSet<BlockHeader>()
    for (query in getBlockHeadersMessage.queries) {
      headersFound.addAll(
        controller.findHeaders(
          query.blockNumberOrBlockHash,
          query.maxHeaders.toLong(),
          query.skip.toLong(),
          query.direction == GetBlockHeadersMessage.BlockHeaderQuery.Direction.BACKWARDS
        )
      )
    }
    service.send(
      subProtocolIdentifier,
      3,
      connection,
      BlockHeadersMessage(getBlockHeadersMessage.reqID, 0L, ArrayList(headersFound)).toBytes()
    )
  }

  override fun handleNewPeerConnection(connection: WireConnection): AsyncCompletion {
    val state = peerStateMap.computeIfAbsent(connection.uri()) { LESPeerState() }
    state.ourStatusMessage = StatusMessage(
      subProtocolIdentifier.version(),
      blockchainInfo.networkID(),
      blockchainInfo.totalDifficulty(),
      blockchainInfo.bestHash(),
      blockchainInfo.bestNumber(),
      blockchainInfo.genesisHash(),
      serveHeaders,
      serveChainSince,
      serveStateSince,
      false,
      flowControlBufferLimit,
      flowControlMaximumRequestCostTable,
      flowControlMinimumRateOfRecharge,
      0
    )
    service.send(subProtocolIdentifier, 0, connection, state.ourStatusMessage!!.toBytes())
    return state.ready
  }

  override fun stop(): AsyncCompletion {
    return AsyncCompletion.completed()
  }
}
