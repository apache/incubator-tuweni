// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
