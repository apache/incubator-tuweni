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
package org.apache.tuweni.ethclient

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.apache.tuweni.devp2p.eth.EthRequestsManager
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.repository.BlockchainRepository
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

const val HEADER_REQUEST_SIZE = 1024L

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
  private val adapter: WireConnectionPeerRepositoryAdapter
) : Synchronizer(executor, coroutineContext, repository, client, peerRepository) {

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
      val bestHash = ethereumConnection.status()!!.bestHash
      if (!repository.hasBlockHeader(bestHash)) {
        client.requestBlockHeaders(
          Hash.fromBytes(bestHash),
          HEADER_REQUEST_SIZE,
          0L,
          true,
          adapter.get(ethereumConnection)
        ).thenAccept(::addHeaders)
      }
    }
  }
}
