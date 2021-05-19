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
package org.apache.tuweni.ethstats

/**
 * Controller receiving information of all clients connected to the ethstats server.
 */
interface EthStatsServerController {

  /**
   * Reads node information. This is sent upon connection from the peer.
   * @param remoteAddress the address of the peer
   * @param id the id the of the peer
   * @param nodeInfo information about the node
   */
  fun readNodeInfo(remoteAddress: String, id: String, nodeInfo: NodeInfo) {
  }

  /**
   * Reads latency from the peer to the server, in milliseconds.
   * @param remoteAddress the address of the peer
   * @param id the id the of the peer
   * @param latency the latency in milliseconds
   */
  fun readLatency(remoteAddress: String, id: String, latency: Long) {
  }

  /**
   * Handles the disconnection of the peer.
   * @param remoteAddress the address of the peer
   * @param id the id the of the peer
   */
  fun readDisconnect(remoteAddress: String, id: String) {
  }

  /**
   * Reads node statistics.
   * @param remoteAddress the address of the peer
   * @param id the id the of the peer
   * @param nodeStats node statistics
   */
  fun readNodeStats(remoteAddress: String, id: String, nodeStats: NodeStats) {
  }

  /**
   * Reads the number of pending transactions from the peer
   * @param remoteAddress the address of the peer
   * @param id the id the of the peer
   * @param pendingTx the number of pending transactions
   */
  fun readPendingTx(remoteAddress: String, id: String, pendingTx: Long) {
  }

  /**
   * Reads block information from the peer.
   * @param remoteAddress the address of the peer
   * @param id the id the of the peer
   * @param block new block statistics
   */
  fun readBlock(remoteAddress: String, id: String, block: BlockStats) {
  }
}
