// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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

const val DELAY: Long = 1000
const val HEADER_PARENT_HEADER_REQUEST_SIZE: Long = 64

/**
 * This synchronizer is requesting the parent headers of an unknown block header, until this is resolved.
 *
 * To maximize chances of getting new headers, the synchronizer will ask for headers with different skip levels,
 * triggering this synchronizer again and parallelizing requests.
 *
 */
class FromUnknownParentSynchronizer(
  executor: ExecutorService = Executors.newSingleThreadExecutor(),
  coroutineContext: CoroutineContext = executor.asCoroutineDispatcher(),
  repository: BlockchainRepository,
  client: EthRequestsManager,
  peerRepository: EthereumPeerRepository,
  from: UInt256?,
  to: UInt256?,
) : Synchronizer(executor, coroutineContext, repository, client, peerRepository, from, to) {

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
    if (header.number.isZero) {
      return
    }
    if ((null != from && header.number < from) || (null != to && header.number > to)) {
      return
    }
    val parentHash = header.parentHash ?: return
    launch {
      delay(DELAY)
      if (!repository.hasBlockHeader(parentHash)) {
        logger.info("Requesting parent headers from unknown parent header hash $parentHash")
        client.requestBlockHeaders(parentHash, HEADER_PARENT_HEADER_REQUEST_SIZE, 1L, true).thenAccept(::addHeaders)
        client.requestBlockHeaders(header.hash, HEADER_PARENT_HEADER_REQUEST_SIZE, 1L, true).thenAccept(::addHeaders)
        client.requestBlockHeaders(parentHash, HEADER_PARENT_HEADER_REQUEST_SIZE, 5L, true).thenAccept(::addHeaders)
      }
    }
  }
}
