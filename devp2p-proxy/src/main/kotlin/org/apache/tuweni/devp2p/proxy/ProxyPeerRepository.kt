// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.proxy

internal class ProxyPeerRepository {

  val peers = mutableMapOf<String, PeerInfo>()

  fun addPeer(id: String, peer: PeerInfo) {
    peers[id] = peer
  }
}
