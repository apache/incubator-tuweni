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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.tuweni.devp2p.eth.EthClient
import org.apache.tuweni.eth.repository.BlockchainRepository
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

const val BEST_PEER_DELAY: Long = 5000

class FromBestBlockSynchronizer(
  executor: ExecutorService = Executors.newSingleThreadExecutor(),
  coroutineContext: CoroutineContext = executor.asCoroutineDispatcher(),
  repository: BlockchainRepository,
  client: EthClient,
  peerRepository: EthereumPeerRepository
) : Synchronizer(executor, coroutineContext, repository, client, peerRepository) {

  override fun start() {
    askNextBestHeaders()
  }

  override fun stop() {

    executor.shutdown()
  }

  private fun askNextBestHeaders() {
    launch {
      delay(BEST_PEER_DELAY)
      if (peerRepository.activeConnections().count() == 0L) {
        askNextBestHeaders()
        return@launch
      }
      val bestHeader = repository.retrieveChainHeadHeader()
      bestHeader?.let { header ->
        client.requestBlockHeaders(header.hash, HEADER_REQUEST_SIZE, 0L, false).thenAccept {
          addHeaders(it)
          if (it.isNotEmpty()) {
            askNextBestHeaders()
          }
        }
      }
    }
  }
}
