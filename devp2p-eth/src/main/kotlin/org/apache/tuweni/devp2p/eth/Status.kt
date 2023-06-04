// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
