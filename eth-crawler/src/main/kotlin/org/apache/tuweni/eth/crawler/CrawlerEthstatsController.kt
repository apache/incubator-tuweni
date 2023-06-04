// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.crawler

import org.apache.tuweni.ethstats.BlockStats
import org.apache.tuweni.ethstats.EthStatsServerController
import org.apache.tuweni.ethstats.NodeInfo
import org.apache.tuweni.ethstats.NodeStats

class CrawlerEthstatsController(val repository: EthstatsDataRepository) : EthStatsServerController {

  override fun readNodeInfo(remoteAddress: String, id: String, nodeInfo: NodeInfo) {
    repository.storeNodeInfo(remoteAddress, id, nodeInfo)
  }

  override fun readBlock(remoteAddress: String, id: String, block: BlockStats) {
    repository.storeBlock(remoteAddress, id, block)
  }

  override fun readLatency(remoteAddress: String, id: String, latency: Long) {
    repository.storeLatency(remoteAddress, id, latency)
  }

  override fun readNodeStats(remoteAddress: String, id: String, nodeStats: NodeStats) {
    repository.storeNodeStats(remoteAddress, id, nodeStats)
  }

  override fun readPendingTx(remoteAddress: String, id: String, pendingTx: Long) {
    repository.storePendingTx(remoteAddress, id, pendingTx)
  }

  override fun readDisconnect(remoteAddress: String, id: String) {
    repository.updateDisconnect(remoteAddress, id)
  }
}
