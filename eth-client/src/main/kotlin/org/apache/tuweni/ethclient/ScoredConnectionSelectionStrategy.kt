// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethclient

import org.apache.tuweni.devp2p.eth.ConnectionSelectionStrategy
import org.apache.tuweni.rlpx.wire.WireConnection

class ScoredConnectionSelectionStrategy(val adapter: WireConnectionPeerRepositoryAdapter) :
  ConnectionSelectionStrategy {

  override fun selectConnection(): WireConnection {
    TODO("Not yet implemented")
  }
}
