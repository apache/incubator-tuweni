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
import org.apache.tuweni.devp2p.eth.EthRequestsManager
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.ethclient.EthereumPeerRepository
import org.apache.tuweni.units.bigints.UInt256
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class CanonicalSynchronizer(
  executor: ExecutorService = Executors.newFixedThreadPool(1),
  coroutineContext: CoroutineContext = executor.asCoroutineDispatcher(),
  repository: BlockchainRepository,
  client: EthRequestsManager,
  peerRepository: EthereumPeerRepository,
  from: UInt256?,
  to: UInt256?,
  private val fromRepository: BlockchainRepository
) : Synchronizer(executor, coroutineContext, repository, client, peerRepository, from, to) {

  private var listenerId: String? = null
  override fun start() {
    listenerId = fromRepository.addBlockchainHeadListener(this::newBlockchainHead)
  }

  private fun newBlockchainHead(block: Block) {
    TODO("Not yet implemented" + block)
  }

  override fun stop() {
    listenerId?.let {
      fromRepository.removeBlockchainHeadListener(it)
    }
  }
}
