// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethclient

import org.apache.tuweni.devp2p.eth.ConnectionSelectionStrategy
import org.apache.tuweni.rlpx.wire.WireConnection

class ConnectionManagementStrategy(
  val maxConnections: Int = 25,
  val peerRepositoryAdapter: WireConnectionPeerRepositoryAdapter,
) : ConnectionSelectionStrategy {
  override fun selectConnection(): WireConnection {
    // TODO find better way to pick ideal peer.
    val conn = peerRepositoryAdapter.peerRepository.activeConnections().findFirst()
    return conn.map { peerRepositoryAdapter.get(it) }.get()
  }
}
