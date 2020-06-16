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
import org.apache.tuweni.rlpx.wire.WireConnection
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

  override fun handle(connection: WireConnection, messageType: Int, message: Bytes) = asyncCompletion {
    logger.trace("Receiving message of type {}", messageType)
    when (messageType) {
      MessageType.Status.code -> handleStatus(connection, StatusMessage.read(message))
      MessageType.NewBlockHashes.code -> handleNewBlockHashes(NewBlockHashes.read(message))
      MessageType.Transactions.code -> handleTransactions(Transactions.read(message))
      MessageType.GetBlockHeaders.code -> handleGetBlockHeaders(connection.id(), GetBlockHeaders.read(message))
      MessageType.BlockHeaders.code -> handleHeaders(connection.id(), BlockHeaders.read(message))
      MessageType.GetBlockBodies.code -> handleGetBlockBodies(connection.id(), GetBlockBodies.read(message))
      MessageType.BlockBodies.code -> handleBlockBodies(connection.id(), BlockBodies.read(message))
      MessageType.NewBlock.code -> handleNewBlock(NewBlock.read(message))
      MessageType.GetNodeData.code -> handleGetNodeData(connection.id(), GetNodeData.read(message))
      MessageType.NodeData.code -> handleNodeData(connection.id(), NodeData.read(message))
      MessageType.GetReceipts.code -> handleGetReceipts(connection.id(), GetReceipts.read(message))
      MessageType.Receipts.code -> handleReceipts(connection.id(), Receipts.read(message))
      else -> logger.warn("Unknown message type {}", messageType) // TODO disconnect
    }
  }

  private suspend fun handleTransactions(transactions: Transactions) {
    controller.addNewTransactions(transactions.transactions)
  }

  private suspend fun handleNodeData(connectionId: String, read: NodeData) {
    controller.addNewNodeData(connectionId, read.elements)
  }

  private suspend fun handleStatus(connection: WireConnection, status: StatusMessage) {
    if (!status.networkID.equals(blockchainInfo.networkID()) ||
      !status.genesisHash.equals(blockchainInfo.genesisHash())) {
      peersMap[connection.id()]?.cancel()
      service.disconnect(connection.id(), DisconnectReason.SUBPROTOCOL_REASON)
    }
    peersMap[connection.id()]?.connect()
    controller.receiveStatus(connection, status)
  }

  private suspend fun handleReceipts(connectionId: String, receipts: Receipts) {
    controller.addNewTransactionReceipts(connectionId, receipts.transactionReceipts)
  }

  private suspend fun handleGetReceipts(connectionId: String, getReceipts: GetReceipts) {

    service.send(
      EthSubprotocol.ETH64,
      MessageType.Receipts.code,
      connectionId,
      Receipts(controller.findTransactionReceipts(getReceipts.hashes)).toBytes()
    )
  }

  private suspend fun handleGetNodeData(connectionId: String, nodeData: GetNodeData) {
    service.send(
      EthSubprotocol.ETH64,
      MessageType.NodeData.code,
      connectionId,
      NodeData(controller.findNodeData(nodeData.hashes)).toBytes()
    )
  }

  private suspend fun handleNewBlock(read: NewBlock) {
    controller.addNewBlock(read.block)
  }

  private suspend fun handleBlockBodies(connectionId: String, message: BlockBodies) {
    controller.addNewBlockBodies(connectionId, message.bodies)
  }

  private suspend fun handleGetBlockBodies(connectionId: String, message: GetBlockBodies) {
    service.send(
      EthSubprotocol.ETH64,
      MessageType.BlockBodies.code,
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
      EthSubprotocol.ETH64, MessageType.Status.code, connectionId, StatusMessage(
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
  }
}

class PeerInfo() {

  val ready: CompletableAsyncCompletion = AsyncCompletion.incomplete()

  fun connect() {
    ready.complete()
  }

  fun cancel() {
    ready.cancel()
  }
}
