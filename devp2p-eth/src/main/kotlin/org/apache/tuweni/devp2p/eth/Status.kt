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
package org.apache.tuweni.devp2p.eth

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.units.bigints.UInt256

/**
 * Peer status information
 * @param protocolVersion ETH subprotocol version
 * @param networkID the network ID
 * @param totalDifficulty the total difficulty known to the peer
 * @param bestHash best block hash known to the peer
 * @param genesisHash genesis hash used by peer
 * @param forkHash the hash of the fork if known
 * @param forkBlock the fork block number if known
 */
data class Status(
  val protocolVersion: Int,
  val networkID: UInt256,
  val totalDifficulty: UInt256,
  val bestHash: Bytes32,
  val genesisHash: Bytes32,
  val forkHash: Bytes?,
  val forkBlock: Long?
)
