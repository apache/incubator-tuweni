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
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.repository.BlockchainRepository
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

const val DELAY: Long = 1000
const val HEADER_PARENT_HEADER_REQUEST_SIZE: Long = 64

class FromUnknownParentSynchronizer(
  executor: ExecutorService = Executors.newFixedThreadPool(1),
  coroutineContext: CoroutineContext = executor.asCoroutineDispatcher(),
  repository: BlockchainRepository,
  client: EthClient,
  peerRepository: EthereumPeerRepository
) : Synchronizer(executor, coroutineContext, repository, client, peerRepository) {

  var listenerId: String? = null

  override fun start() {
    listenerId = repository.addBlockHeaderListener(::listenToBlockHeaders)
  }

  override fun stop() {
    listenerId?.let {
      repository.removeBlockHeaderListener(it)
    }
    executor.shutdown()
  }

  private fun listenToBlockHeaders(header: BlockHeader) {
    val parentHash = header.parentHash ?: return
    launch {
      delay(DELAY)
      if (!repository.hasBlockHeader(parentHash)) {
        client.requestBlockHeaders(parentHash, HEADER_PARENT_HEADER_REQUEST_SIZE, 1L, true).thenAccept(::addHeaders)
        client.requestBlockHeaders(header.hash, HEADER_PARENT_HEADER_REQUEST_SIZE, 1L, true).thenAccept(::addHeaders)
        client.requestBlockHeaders(parentHash, HEADER_PARENT_HEADER_REQUEST_SIZE, 5L, true).thenAccept(::addHeaders)
      }
    }
  }
}
