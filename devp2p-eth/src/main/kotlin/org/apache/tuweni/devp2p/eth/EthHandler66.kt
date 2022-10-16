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
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.ExpiringMap
import org.apache.tuweni.concurrent.coroutines.asyncCompletion
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.DisconnectReason
import org.apache.tuweni.rlpx.wire.SubProtocolHandler
import org.apache.tuweni.rlpx.wire.WireConnection
import org.apache.tuweni.units.bigints.UInt64
import org.slf4j.LoggerFactory
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

internal class EthHandler66(
  override val coroutineContext: CoroutineContext = Dispatchers.Default,
  private val blockchainInfo: BlockchainInformation,
  private val service: RLPxService,
  private val controller: EthController
) : SubProtocolHandler, CoroutineScope {

  private val ethHandler = EthHandler(coroutineContext, blockchainInfo, service, controller)
  private val pendingStatus = ExpiringMap<String, PeerInfo>(60000)

  companion object {
    val logger = LoggerFactory.getLogger(EthHandler66::class.java)!!
    val MAX_NEW_POOLED_TX_HASHES = 4096
    val MAX_POOLED_TX = 256
  }

  override fun handle(connection: WireConnection, messageType: Int, message: Bytes): AsyncCompletion {
    if (connection.agreedSubprotocolVersion(EthSubprotocol.ETH66.name()) != EthSubprotocol.ETH66) {
      return ethHandler.handle(connection, messageType, message)
    }
    return asyncCompletion {
      logger.debug("Receiving message of type {}", messageType)
      val pair = RLP.decodeList(message) {
        Pair(it.readValue(), it.readRemaining())
      }
      val requestIdentifier = pair.first
      val payload = pair.second

      when (messageType) {
        MessageType.Status.code -> handleStatus(connection, StatusMessage.read(message))
        MessageType.NewBlockHashes.code -> handleNewBlockHashes(NewBlockHashes.read(message))
        MessageType.Transactions.code -> handleTransactions(Transactions.read(message))
        MessageType.GetBlockHeaders.code -> handleGetBlockHeaders(
          connection,
          requestIdentifier,
          GetBlockHeaders.read(payload)
        )
        MessageType.BlockHeaders.code -> handleHeaders(connection, requestIdentifier, BlockHeaders.read(payload))
        MessageType.GetBlockBodies.code -> handleGetBlockBodies(
          connection,
          requestIdentifier,
          GetBlockBodies.read(payload)
        )
        MessageType.BlockBodies.code -> handleBlockBodies(connection, requestIdentifier, BlockBodies.read(payload))
        MessageType.NewBlock.code -> handleNewBlock(NewBlock.read(message))
        MessageType.GetNodeData.code -> handleGetNodeData(connection, requestIdentifier, GetNodeData.read(payload))
        MessageType.NodeData.code -> handleNodeData(connection, requestIdentifier, NodeData.read(payload))
        MessageType.GetReceipts.code -> handleGetReceipts(connection, requestIdentifier, GetReceipts.read(payload))
        MessageType.Receipts.code -> handleReceipts(connection, requestIdentifier, Receipts.read(payload))
        MessageType.NewPooledTransactionHashes.code -> handleNewPooledTransactionHashes(
          connection,
          NewPooledTransactionHashes.read(message)
        )
        MessageType.GetPooledTransactions.code -> handleGetPooledTransactions(
          connection,
          requestIdentifier,
          GetPooledTransactions.read(payload)
        )
        MessageType.PooledTransactions.code -> handlePooledTransactions(PooledTransactions.read(payload))
        else -> {
          logger.warn("Unknown message type {} with request identifier {}", messageType, requestIdentifier)
          service.disconnect(connection, DisconnectReason.SUBPROTOCOL_REASON)
        }
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
    var peerInfo = pendingStatus.remove(connection.uri())
    if (peerInfo == null) {
      logger.info("Unexpected status message ${connection.uri()}")
      val newPeerInfo = PeerInfo()
      pendingStatus.put(connection.uri(), newPeerInfo)
      peerInfo = newPeerInfo
    }

    var disconnect = false
    if (status.networkID != blockchainInfo.networkID()) {
      logger.info("Peer with different networkId ${status.networkID} (expected ${blockchainInfo.networkID()})")
      disconnect = true
    }
    if (!status.genesisHash.equals(blockchainInfo.genesisHash())) {
      EthHandler.logger.info("Peer with different genesisHash ${status.genesisHash} (expected ${blockchainInfo.genesisHash()})")
      disconnect = true
    }

    if (disconnect) {
      service.disconnect(connection, DisconnectReason.SUBPROTOCOL_REASON)
    }
    peerInfo.complete()
    controller.receiveStatus(connection, status.toStatus())
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
    if (message.hashes.isEmpty()) {
      service.disconnect(connection, DisconnectReason.SUBPROTOCOL_REASON)
      return
    }
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
      EthSubprotocol.ETH66,
      MessageType.BlockHeaders.code,
      connection,
      RLP.encodeList {
        it.writeValue(requestIdentifier)
        it.writeRLP(BlockHeaders(headers).toBytes())
      }
    )
  }

  private suspend fun handleNewBlockHashes(message: NewBlockHashes) {
    controller.addNewBlockHashes(message.hashes)
  }

  override fun handleNewPeerConnection(connection: WireConnection): AsyncCompletion = runBlocking {
    if (connection.agreedSubprotocolVersion(EthSubprotocol.ETH66.name()) != EthSubprotocol.ETH66) {
      logger.debug("Downgrade connection from eth/66 to eth65")
      return@runBlocking ethHandler.handleNewPeerConnection(connection)
    }
    val newPeer = pendingStatus.computeIfAbsent(connection.uri()) {
      logger.debug("Register a new peer ${connection.uri()}")
      PeerInfo()
    }
    val ethSubProtocol = connection.agreedSubprotocolVersion(EthSubprotocol.ETH66.name())
    if (ethSubProtocol == null) {
      newPeer.cancel()
      return@runBlocking newPeer.ready
    }
    val forkId =
      blockchainInfo.getLastestApplicableFork(controller.repository.retrieveChainHeadHeader().number.toLong())
    service.send(
      ethSubProtocol,
      MessageType.Status.code,
      connection,
      StatusMessage(
        ethSubProtocol.version(),
        blockchainInfo.networkID(),
        blockchainInfo.totalDifficulty(),
        blockchainInfo.bestHash(),
        blockchainInfo.genesisHash(),
        forkId.hash,
        forkId.next
      ).toBytes()
    )

    return@runBlocking newPeer.ready
  }

  override fun stop() = asyncCompletion {
    ethHandler.stop().await()
  }
}
