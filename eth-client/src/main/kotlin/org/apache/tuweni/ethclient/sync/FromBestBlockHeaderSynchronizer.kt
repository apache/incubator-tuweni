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
  to: UInt256?,
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
