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
import org.apache.tuweni.concurrent.coroutines.asyncCompletion
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.DisconnectReason
import org.apache.tuweni.rlpx.wire.SubProtocolHandler
import org.apache.tuweni.rlpx.wire.WireConnection
import org.apache.tuweni.units.bigints.UInt64
import org.slf4j.LoggerFactory
import java.util.WeakHashMap
import kotlin.collections.ArrayList
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext

internal class EthHandler66(
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

  override fun handle(connection: WireConnection, messageType: Int, payload: Bytes) = asyncCompletion {
    logger.debug("Receiving message of type {}", messageType)
    val pair = RLP.decode(payload) {
      Pair(it.readValue(), it.readRemaining())
    }
    val requestIdentifier = pair.first
    val message = pair.second

    when (messageType) {
      MessageType.Status.code -> handleStatus(connection, StatusMessage.read(message))
      MessageType.NewBlockHashes.code -> handleNewBlockHashes(NewBlockHashes.read(message))
      MessageType.Transactions.code -> handleTransactions(Transactions.read(message))
      MessageType.GetBlockHeaders.code -> handleGetBlockHeaders(connection, requestIdentifier, GetBlockHeaders.read(message))
      MessageType.BlockHeaders.code -> handleHeaders(connection, requestIdentifier, BlockHeaders.read(message))
      MessageType.GetBlockBodies.code -> handleGetBlockBodies(connection, requestIdentifier, GetBlockBodies.read(message))
      MessageType.BlockBodies.code -> handleBlockBodies(connection, requestIdentifier, BlockBodies.read(message))
      MessageType.NewBlock.code -> handleNewBlock(NewBlock.read(message))
      MessageType.GetNodeData.code -> handleGetNodeData(connection, requestIdentifier, GetNodeData.read(message))
      MessageType.NodeData.code -> handleNodeData(connection, requestIdentifier, NodeData.read(message))
      MessageType.GetReceipts.code -> handleGetReceipts(connection, requestIdentifier, GetReceipts.read(message))
      MessageType.Receipts.code -> handleReceipts(connection, requestIdentifier, Receipts.read(message))
      MessageType.NewPooledTransactionHashes.code -> handleNewPooledTransactionHashes(
        connection, NewPooledTransactionHashes.read(message)
      )
      MessageType.GetPooledTransactions.code -> handleGetPooledTransactions(
        connection, requestIdentifier,
        GetPooledTransactions.read(message)
      )
      MessageType.PooledTransactions.code -> handlePooledTransactions(PooledTransactions.read(message))
      else -> {
        logger.warn("Unknown message type {} with request identifier {}", messageType, requestIdentifier)
        service.disconnect(connection, DisconnectReason.SUBPROTOCOL_REASON)
      }
    }
  }

  private suspend fun handlePooledTransactions(read: PooledTransactions) {
    controller.addNewPooledTransactions(read.transactions)
  }

  private suspend fun handleGetPooledTransactions(connection: WireConnection, requestIdentifier: Bytes, read: GetPooledTransactions) {
    val tx = controller.findPooledTransactions(read.hashes)
    logger.debug("Responding to GetPooledTransactions with {} transactions", tx.size)
    service.send(
      EthSubprotocol.ETH66,
      MessageType.PooledTransactions.code,
      connection,
      RLP.encodeList {
        it.writeValue(requestIdentifier)
        it.writeRLP(PooledTransactions(tx).toBytes())
      }
    )
  }

  private suspend fun handleTransactions(transactions: Transactions) {
    controller.addNewTransactions(transactions.transactions)
  }

  private suspend fun handleNodeData(connection: WireConnection, requestIdentifier: Bytes, read: NodeData) {
    controller.addNewNodeData(connection, requestIdentifier, read.elements)
  }

  private suspend fun handleStatus(connection: WireConnection, status: StatusMessage) {
    logger.debug("Received status message {}", status)
    val peerInfo = pendingStatus.remove(connection.uri())
    if (!status.networkID.equals(blockchainInfo.networkID()) ||
      !status.genesisHash.equals(blockchainInfo.genesisHash()) ||
      peerInfo == null
    ) {
      peerInfo?.cancel()
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
          EthSubprotocol.ETH66,
          MessageType.GetPooledTransactions.code,
          connection,
          RLP.encodeList {
            it.writeValue(UInt64.random().toBytes())
            it.writeRLP(message.toBytes())
          }
        )
        missingTx = ArrayList()
        message = GetPooledTransactions(missingTx)
      }
    }
    if (!missingTx.isEmpty()) {
      service.send(
        EthSubprotocol.ETH66,
        MessageType.GetPooledTransactions.code,
        connection,
        RLP.encodeList {
          it.writeValue(UInt64.random().toBytes())
          it.writeRLP(message.toBytes())
        }
      )
    }
  }

  private suspend fun handleReceipts(connection: WireConnection, requestIdentifier: Bytes, receipts: Receipts) {
    controller.addNewTransactionReceipts(connection, requestIdentifier, receipts.transactionReceipts)
  }

  private suspend fun handleGetReceipts(connection: WireConnection, requestIdentifier: Bytes, getReceipts: GetReceipts) {
    val receipts = controller.findTransactionReceipts(getReceipts.hashes)
    service.send(
      EthSubprotocol.ETH66,
      MessageType.Receipts.code,
      connection,
      RLP.encodeList {
        it.writeValue(requestIdentifier)
        it.writeRLP(Receipts(receipts).toBytes())
      }
    )
  }

  private suspend fun handleGetNodeData(connection: WireConnection, requestIdentifier: Bytes, nodeData: GetNodeData) {
    val data = controller.findNodeData(nodeData.hashes)
    service.send(
      EthSubprotocol.ETH66,
      MessageType.NodeData.code,
      connection,
      RLP.encodeList {
        it.writeValue(requestIdentifier)
        it.writeRLP(NodeData(data).toBytes())
      }
    )
  }

  private suspend fun handleNewBlock(read: NewBlock) {
    controller.addNewBlock(read.block)
  }

  private suspend fun handleBlockBodies(connection: WireConnection, requestIdentifier: Bytes, message: BlockBodies) {
    controller.addNewBlockBodies(connection, requestIdentifier, message.bodies)
  }

  private suspend fun handleGetBlockBodies(connection: WireConnection, requestIdentifier: Bytes, message: GetBlockBodies) {
    val bodies = BlockBodies(controller.findBlockBodies(message.hashes))
    service.send(
      EthSubprotocol.ETH66,
      MessageType.BlockBodies.code,
      connection,
      RLP.encodeList {
        it.writeValue(requestIdentifier)
        it.writeRLP(bodies.toBytes())
      }
    )
  }

  private suspend fun handleHeaders(connection: WireConnection, requestIdentifier: Bytes, headers: BlockHeaders) {
    controller.addNewBlockHeaders(connection, requestIdentifier, headers.headers)
  }

  private suspend fun handleGetBlockHeaders(connection: WireConnection, requestIdentifier: Bytes, blockHeaderRequest: GetBlockHeaders) {
    val headers = controller.findHeaders(
      blockHeaderRequest.block,
      blockHeaderRequest.maxHeaders,
      blockHeaderRequest.skip,
      blockHeaderRequest.reverse
    )
    service.send(
      EthSubprotocol.ETH66, MessageType.BlockHeaders.code, connection,
      RLP.encodeList {
        it.writeValue(requestIdentifier)
        it.writeRLP(BlockHeaders(headers).toBytes())
      }
    )
  }

  private suspend fun handleNewBlockHashes(message: NewBlockHashes) {
    controller.addNewBlockHashes(message.hashes)
  }

  override fun handleNewPeerConnection(connection: WireConnection): AsyncCompletion {
    val newPeer = PeerInfo()
    pendingStatus[connection.uri()] = newPeer
    val ethSubProtocol = connection.agreedSubprotocols().firstOrNull() { it.name() == EthSubprotocol.ETH65.name() }
    if (ethSubProtocol == null) {
      newPeer.cancel()
      return newPeer.ready
    }
    service.send(
      ethSubProtocol, MessageType.Status.code, connection,
      RLP.encodeList {
        it.writeValue(UInt64.random().toBytes())
        it.writeRLP(
          StatusMessage(
            ethSubProtocol.version(),
            blockchainInfo.networkID(), blockchainInfo.totalDifficulty(),
            blockchainInfo.bestHash(), blockchainInfo.genesisHash(), blockchainInfo.getLatestForkHash(),
            blockchainInfo.getLatestFork()
          ).toBytes()
        )
      }
    )

    return newPeer.ready
  }

  override fun stop() = asyncCompletion {
  }
}
