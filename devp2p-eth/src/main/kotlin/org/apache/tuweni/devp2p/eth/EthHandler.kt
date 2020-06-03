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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.CompletableAsyncCompletion
import org.apache.tuweni.concurrent.coroutines.asyncCompletion
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.DisconnectReason
import org.apache.tuweni.rlpx.wire.SubProtocolHandler
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

internal class EthHandler(
  override val coroutineContext: CoroutineContext = Dispatchers.Default,
  private val blockchainInfo: BlockchainInformation,
  private val service: RLPxService,
  private val controller: EthController
) : SubProtocolHandler, CoroutineScope {

  companion object {
    val logger = LoggerFactory.getLogger(EthHandler::class.java)!!
  }

  val peersMap: MutableMap<String, PeerInfo> = mutableMapOf()

  override fun handle(connectionId: String, messageType: Int, message: Bytes) = asyncCompletion {
    logger.debug("Receiving message of type {}", messageType)
    when (messageType) {
      MessageType.Status.code -> handleStatus(connectionId, StatusMessage.read(message))
      MessageType.NewBlockHashes.code -> handleNewBlockHashes(NewBlockHashes.read(message))
//    Transactions.code -> // do nothing.
      MessageType.GetBlockHeaders.code -> handleGetBlockHeaders(connectionId, GetBlockHeaders.read(message))
      MessageType.BlockHeaders.code -> handleHeaders(connectionId, BlockHeaders.read(message))
      MessageType.GetBlockBodies.code -> handleGetBlockBodies(connectionId, GetBlockBodies.read(message))
      MessageType.BlockBodies.code -> handleBlockBodies(BlockBodies.read(message))
      MessageType.NewBlock.code -> handleNewBlock(NewBlock.read(message))
      MessageType.GetNodeData.code -> handleGetNodeData(connectionId, GetNodeData.read(message))
//    MessageType.NodeData.code-> // not implemented yet.
      MessageType.GetReceipts.code -> handleGetReceipts(connectionId, GetReceipts.read(message))
      // MessageType.Receipts.code -> handleReceipts(Receipts.read(message)) // not implemented yet
    }
  }

  private fun handleStatus(connectionId: String, status: StatusMessage) {
    if (!status.networkID.equals(blockchainInfo.networkID())) {
      peersMap[connectionId]?.cancel()
      service.disconnect(connectionId, DisconnectReason.SUBPROTOCOL_REASON)
    }
    peersMap[connectionId]?.connect()
    // TODO send to controller.
  }

//  private fun handleReceipts(receipts: Receipts) {
//    repository.storeTransactionReceipts()
//  }

  private suspend fun handleGetReceipts(connectionId: String, getReceipts: GetReceipts) {

    service.send(
      EthSubprotocol.ETH64,
      MessageType.Receipts.code,
      connectionId,
      Receipts(controller.findTransactionReceipts(getReceipts.hashes)).toBytes()
    )
  }

  private fun handleGetNodeData(connectionId: String, nodeData: GetNodeData) {
    // TODO implement
    nodeData.toBytes()
    service.send(EthSubprotocol.ETH64, MessageType.NodeData.code, connectionId, NodeData(emptyList()).toBytes())
  }

  private suspend fun handleNewBlock(read: NewBlock) {
    controller.addNewBlock(read.block)
  }

  private fun handleBlockBodies(message: BlockBodies) {
    message.bodies.forEach {
      //      if (blockBodyRequests.remove(it)) {
//        repository.
//      } else {
//        service.disconnect(connectionId, DisconnectReason.PROTOCOL_BREACH)
//      }
    }
  }

  private suspend fun handleGetBlockBodies(connectionId: String, message: GetBlockBodies) {
    service.send(
      EthSubprotocol.ETH64,
      6,
      connectionId,
      BlockBodies(controller.findBlockBodies(message.hashes)).toBytes()
    )
  }

  private suspend fun handleHeaders(connectionId: String, headers: BlockHeaders) {
    controller.addNewBlockHeaders(connectionId, headers.headers)
  }

  private suspend fun handleGetBlockHeaders(connectionId: String, blockHeaderRequest: GetBlockHeaders) {
    val headers = controller.findHeaders(
      blockHeaderRequest.block,
      blockHeaderRequest.maxHeaders,
      blockHeaderRequest.skip,
      blockHeaderRequest.reverse
    )
    service.send(EthSubprotocol.ETH64, MessageType.BlockHeaders.code, connectionId, BlockHeaders(headers).toBytes())
  }

  private suspend fun handleNewBlockHashes(message: NewBlockHashes) {
    controller.addNewBlockHashes(message.hashes)
  }

  override fun handleNewPeerConnection(connectionId: String): AsyncCompletion {
    service.send(
      EthSubprotocol.ETH64, 0, connectionId, StatusMessage(
        EthSubprotocol.ETH64.version(),
        blockchainInfo.networkID(), blockchainInfo.totalDifficulty(),
        blockchainInfo.bestHash(), blockchainInfo.genesisHash(), blockchainInfo.getLatestForkHash(),
        blockchainInfo.getLatestFork()
      ).toBytes()
    )
    val newPeer = PeerInfo()
    peersMap[connectionId] = newPeer
    return newPeer.ready
  }

  override fun stop() = asyncCompletion {
    TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
  }
}

class PeerInfo() {

  val ready: CompletableAsyncCompletion = AsyncCompletion.incomplete()
  // TODO disconnect if not responding after timeout.

  fun connect() {
    ready.complete()
  }

  fun cancel() {
    ready.cancel()
  }
}
