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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.tuweni.devp2p.eth.EthRequestsManager
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.ethclient.EthereumPeerRepository
import org.apache.tuweni.units.bigints.UInt256
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

const val BEST_PEER_DELAY: Long = 5000
const val HEADERS_RESPONSE_TIMEOUT: Long = 10000

/**
 * This synchronizer strategy will use the best known header, and keep asking new block headers
 * from there, until the response comes back with just one header.
 */
class FromBestBlockHeaderSynchronizer(
  executor: ExecutorService = Executors.newSingleThreadExecutor(),
  coroutineContext: CoroutineContext = executor.asCoroutineDispatcher(),
  repository: BlockchainRepository,
  client: EthRequestsManager,
  peerRepository: EthereumPeerRepository,
  from: UInt256?,
  to: UInt256?
) : Synchronizer(executor, coroutineContext, repository, client, peerRepository, from, to) {

  override fun start() {
    launch {
      repository.indexing = false
      delay(BEST_PEER_DELAY)
      askNextBestHeaders(repository.retrieveChainHeadHeader())
    }
  }

  override fun stop() {
    executor.shutdown()
  }

  private fun askNextBestHeaders(header: BlockHeader) {
    if ((null != from && header.number < from) || (null != to && header.number > to)) {
      return
    }
    launch {
      if (peerRepository.activeConnections().count() == 0L) {
        askNextBestHeaders(header)
        return@launch
      }
      logger.info("Request headers away from best known header ${header.number.toLong()} ${header.hash}")
      var replied = false
      client.requestBlockHeaders(header.hash, HEADER_REQUEST_SIZE, 0L, false).thenAccept { headers ->
        replied = true
        addHeaders(headers)
        if (headers.size > 1) {
          askNextBestHeaders(headers.last())
        } else {
          logger.info("Done requesting headers")
          launch {
            repository.indexing = true
            repository.reIndexTotalDifficulty()
          }
        }
      }
      delay(HEADERS_RESPONSE_TIMEOUT)
      if (!replied) {
        askNextBestHeaders(header)
      }
    }
  }
}
