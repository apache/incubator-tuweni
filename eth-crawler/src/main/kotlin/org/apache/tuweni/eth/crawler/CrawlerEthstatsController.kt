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
