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
import org.apache.tuweni.devp2p.eth.EthSubprotocol.Companion.ETH62
import org.apache.tuweni.devp2p.eth.EthSubprotocol.Companion.ETH65
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.DisconnectReason
import org.apache.tuweni.rlpx.wire.SubProtocolHandler
import org.apache.tuweni.rlpx.wire.WireConnection
import org.slf4j.LoggerFactory
import java.util.WeakHashMap
import kotlin.collections.ArrayList
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext

internal class EthHandler(
  override val coroutineContext: CoroutineContext = Dispatchers.Default,
  private val blockchainInfo: BlockchainInformation,
  private val service: RLPxService,
  private val controller: EthController,
) : SubProtocolHandler, CoroutineScope {

  private val pendingStatus = WeakHashMap<String, PeerInfo>()

  companion object {
    val logger = LoggerFactory.getLogger(EthHandler::class.java)!!
    val MAX_NEW_POOLED_TX_HASHES = 4096
    val MAX_POOLED_TX = 256
  }

  override fun handle(connection: WireConnection, messageType: Int, message: Bytes) = asyncCompletion {
    logger.debug("Receiving message of type {}", messageType)
    when (messageType) {
      MessageType.Status.code -> handleStatus(connection, StatusMessage.read(message))
      MessageType.NewBlockHashes.code -> handleNewBlockHashes(NewBlockHashes.read(message))
      MessageType.Transactions.code -> handleTransactions(Transactions.read(message))
      MessageType.GetBlockHeaders.code -> handleGetBlockHeaders(connection, GetBlockHeaders.read(message))
      MessageType.BlockHeaders.code -> handleHeaders(connection, BlockHeaders.read(message))
      MessageType.GetBlockBodies.code -> handleGetBlockBodies(connection, GetBlockBodies.read(message))
      MessageType.BlockBodies.code -> handleBlockBodies(connection, BlockBodies.read(message))
      MessageType.NewBlock.code -> handleNewBlock(NewBlock.read(message))
      MessageType.GetNodeData.code -> handleGetNodeData(connection, GetNodeData.read(message))
      MessageType.NodeData.code -> handleNodeData(connection, NodeData.read(message))
      MessageType.GetReceipts.code -> handleGetReceipts(connection, GetReceipts.read(message))
      MessageType.Receipts.code -> handleReceipts(connection, Receipts.read(message))
      MessageType.NewPooledTransactionHashes.code -> handleNewPooledTransactionHashes(
        connection,
        NewPooledTransactionHashes.read(message)
      )
      MessageType.GetPooledTransactions.code -> handleGetPooledTransactions(
        connection,
        GetPooledTransactions.read(message)
      )
      MessageType.PooledTransactions.code -> handlePooledTransactions(PooledTransactions.read(message))
      else -> {
        logger.warn("Unknown message type {}", messageType)
        service.disconnect(connection, DisconnectReason.SUBPROTOCOL_REASON)
      }
    }
  }

  private suspend fun handlePooledTransactions(read: PooledTransactions) {
    controller.addNewPooledTransactions(read.transactions)
  }

  private suspend fun handleGetPooledTransactions(connection: WireConnection, read: GetPooledTransactions) {
    val tx = controller.findPooledTransactions(read.hashes)
    logger.debug("Responding to GetPooledTransactions with {} transactions", tx.size)
    service.send(
      EthSubprotocol.ETH65,
      MessageType.PooledTransactions.code,
      connection,
      PooledTransactions(tx).toBytes()
    )
  }

  private suspend fun handleTransactions(transactions: Transactions) {
    controller.addNewTransactions(transactions.transactions)
  }

  private suspend fun handleNodeData(connection: WireConnection, read: NodeData) {
    controller.addNewNodeData(connection, null, read.elements)
  }

  private suspend fun handleStatus(connection: WireConnection, status: StatusMessage) {
    logger.debug("Received status message {}", status)
    var peerInfo = pendingStatus.remove(connection.uri())
    var disconnect = false
    if (status.networkID != blockchainInfo.networkID()) {
      logger.info("Peer with different networkId ${status.networkID} (expected ${blockchainInfo.networkID()})")
      disconnect = true
    }
    if (!status.genesisHash.equals(blockchainInfo.genesisHash())) {
      logger.info("Peer with different genesisHash ${status.genesisHash} (expected ${blockchainInfo.genesisHash()})")
      disconnect = true
    }
    if (peerInfo == null) {
      logger.info("Unexpected status message ${connection.uri()}")
      val newPeerInfo = PeerInfo()
      pendingStatus.put(connection.uri(), newPeerInfo)
      peerInfo = newPeerInfo
    }
    if (disconnect) {
      peerInfo.cancel()
      service.disconnect(connection, DisconnectReason.SUBPROTOCOL_REASON)
    } else {
      peerInfo.connect()
      controller.receiveStatus(connection, status.toStatus())
    }
  }

  private suspend fun handleNewPooledTransactionHashes(
    connection: WireConnection,
    newPooledTransactionHashes: NewPooledTransactionHashes
  ) {
    if (newPooledTransactionHashes.hashes.size > MAX_NEW_POOLED_TX_HASHES) {
      service.disconnect(connection, DisconnectReason.SUBPROTOCOL_REASON)
      return
    }
    var missingTx = ArrayList<Hash>()
    var message = GetPooledTransactions(missingTx)
    for (hash in newPooledTransactionHashes.hashes) {
      if (!controller.pendingTransactionsPool.contains(hash)) {
        missingTx.add(hash)
      }
      if (missingTx.size == MAX_POOLED_TX) {
        service.send(
          connection.agreedSubprotocol(ETH65),
          MessageType.GetPooledTransactions.code,
          connection,
          message.toBytes()
        )
        missingTx = ArrayList()
        message = GetPooledTransactions(missingTx)
      }
    }
    if (!missingTx.isEmpty()) {
      service.send(
        connection.agreedSubprotocol(ETH65),
        MessageType.GetPooledTransactions.code,
        connection,
        message.toBytes()
      )
    }
  }

  private suspend fun handleReceipts(connection: WireConnection, receipts: Receipts) {
    controller.addNewTransactionReceipts(connection, null, receipts.transactionReceipts)
  }

  private suspend fun handleGetReceipts(connection: WireConnection, getReceipts: GetReceipts) {

    service.send(
      connection.agreedSubprotocol(ETH62),
      MessageType.Receipts.code,
      connection,
      Receipts(controller.findTransactionReceipts(getReceipts.hashes)).toBytes()
    )
  }

  private suspend fun handleGetNodeData(connection: WireConnection, nodeData: GetNodeData) {
    service.send(
      connection.agreedSubprotocol(ETH65),
      MessageType.NodeData.code,
      connection,
      NodeData(controller.findNodeData(nodeData.hashes)).toBytes()
    )
  }

  private suspend fun handleNewBlock(read: NewBlock) {
    controller.addNewBlock(read.block)
  }

  private suspend fun handleBlockBodies(connection: WireConnection, message: BlockBodies) {
    controller.addNewBlockBodies(connection, null, message.bodies)
  }

  private suspend fun handleGetBlockBodies(connection: WireConnection, message: GetBlockBodies) {
    service.send(
      connection.agreedSubprotocol(ETH62),
      MessageType.BlockBodies.code,
      connection,
      BlockBodies(controller.findBlockBodies(message.hashes)).toBytes()
    )
  }

  private suspend fun handleHeaders(connection: WireConnection, headers: BlockHeaders) {
    controller.addNewBlockHeaders(connection, null, headers.headers)
  }

  private suspend fun handleGetBlockHeaders(connection: WireConnection, blockHeaderRequest: GetBlockHeaders) {
    val headers = controller.findHeaders(
      blockHeaderRequest.block,
      blockHeaderRequest.maxHeaders,
      blockHeaderRequest.skip,
      blockHeaderRequest.reverse
    )
    service.send(connection.agreedSubprotocol(ETH62), MessageType.BlockHeaders.code, connection, BlockHeaders(headers).toBytes())
  }

  private suspend fun handleNewBlockHashes(message: NewBlockHashes) {
    controller.addNewBlockHashes(message.hashes)
  }

  override fun handleNewPeerConnection(connection: WireConnection): AsyncCompletion {
    val newPeer = pendingStatus.computeIfAbsent(connection.uri()) { PeerInfo() }
    val ethSubProtocol = connection.agreedSubprotocol(EthSubprotocol.ETH65)
    if (ethSubProtocol == null) {
      newPeer.cancel()
      return newPeer.ready
    }
    logger.info("Sending status message to ${connection.uri()}")
    service.send(
      ethSubProtocol, MessageType.Status.code, connection,
      StatusMessage(
        ethSubProtocol.version(),
        blockchainInfo.networkID(), blockchainInfo.totalDifficulty(),
        blockchainInfo.bestHash(), blockchainInfo.genesisHash(), blockchainInfo.getLatestForkHash(),
        blockchainInfo.getLatestFork()
      ).toBytes()
    )

    return newPeer.ready
  }

  override fun stop() = asyncCompletion {
  }
}

internal class PeerInfo {

  val ready: CompletableAsyncCompletion = AsyncCompletion.incomplete()

  fun connect() {
    ready.complete()
  }

  fun cancel() {
    ready.cancel()
  }
}
