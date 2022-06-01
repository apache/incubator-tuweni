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
package org.apache.tuweni.ethclient.sync

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.apache.tuweni.devp2p.eth.EthRequestsManager
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.ethclient.EthereumConnection
import org.apache.tuweni.ethclient.EthereumPeerRepository
import org.apache.tuweni.ethclient.WireConnectionPeerRepositoryAdapter
import org.apache.tuweni.units.bigints.UInt256
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

const val HEADER_REQUEST_SIZE = 192L

/**
 * Synchronizer responsible for pulling blocks until such time the highest known block is met, or close enough.
 *
 * Listens to new status messages, and syncs backwards from them.
 *
 */
class PeerStatusEthSynchronizer(
  executor: ExecutorService = Executors.newSingleThreadExecutor(),
  coroutineContext: CoroutineContext = executor.asCoroutineDispatcher(),
  repository: BlockchainRepository,
  client: EthRequestsManager,
  peerRepository: EthereumPeerRepository,
  private val adapter: WireConnectionPeerRepositoryAdapter,
  from: UInt256? = UInt256.ZERO,
  to: UInt256?
) : Synchronizer(executor, coroutineContext, repository, client, peerRepository, from, to) {

  var listenerId: String? = null

  override fun start() {
    listenerId = peerRepository.addStatusListener(::listenToStatus)
  }

  override fun stop() {
    listenerId?.let {
      peerRepository.removeStatusListener(it)
    }
    executor.shutdown()
  }

  private fun listenToStatus(ethereumConnection: EthereumConnection) {
    launch {
      logger.info("Triggering status synchronizer for ${ethereumConnection.identity()}")
      val bestHash = ethereumConnection.status()!!.bestHash
      if (!repository.hasBlockHeader(bestHash)) {
        logger.info("Requesting new best block header based on status $bestHash")
        client.requestBlockHeaders(
          Hash.fromBytes(bestHash),
          HEADER_REQUEST_SIZE,
          0L,
          true,
          adapter.get(ethereumConnection)
        ).thenAccept {
          addHeaders(it)
          launch {
            repeatAskingForHeaders(ethereumConnection, it.last())
          }
        }
      }
    }
  }

  private suspend fun repeatAskingForHeaders(ethereumConnection: EthereumConnection, header: BlockHeader) {
    if (from?.greaterOrEqualThan(header.number) == true) {
      logger.info("Status synchronizer done with ${ethereumConnection.identity()}")
      return
    }
    if (repository.hasBlockHeader(header.hash)) {
      logger.info("Status synchronizer hitting known header ${header.number}, stop there.")
      return
    }
    logger.info("Requesting headers for  ${ethereumConnection.identity()} at block ${header.number}")
    client.requestBlockHeaders(
      Hash.fromBytes(header.hash),
      HEADER_REQUEST_SIZE,
      0L,
      true,
      adapter.get(ethereumConnection)
    ).thenAccept {
      addHeaders(it)
      launch {
        repeatAskingForHeaders(ethereumConnection, it.last())
      }
    }
  }
}
