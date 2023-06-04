// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethclient

import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.peer.repository.memory.MemoryPeerRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(BouncyCastleExtension::class)
class MemoryEthereumPeerRepositoryTest {

  @Test
  fun testEmptyByDefault() {
    val repo = MemoryPeerRepository()
    Assertions.assertNull(repo.randomPeer())
  }

  @Test
  fun testStorePeerAndIdentity() {
    val repo = MemoryPeerRepository()
    val identity = repo.storeIdentity("0.0.0.0", 12345, SECP256K1.KeyPair.random().publicKey())
    val peer = repo.storePeer(identity, Instant.now(), Instant.now())
    repo.addConnection(peer, identity)
    Assertions.assertEquals(1, peer.connections().size)
    Assertions.assertEquals(1, identity.connections().size)
    Assertions.assertEquals(1, identity.activePeers().size)
    Assertions.assertEquals(peer, identity.activePeers().get(0))

    repo.markConnectionInactive(peer, identity)
    Assertions.assertEquals(0, identity.activePeers().size)
  }

  @Test
  fun testLastContacted() {
    val repo = MemoryPeerRepository()
    val identity = repo.storeIdentity("0.0.0.0", 12345, SECP256K1.KeyPair.random().publicKey())
    val fiveSecondsAgo = Instant.now().minus(5, ChronoUnit.SECONDS)
    val peer = repo.storePeer(identity, fiveSecondsAgo, Instant.now())
    repo.addConnection(peer, identity)
    Assertions.assertTrue(peer.lastContacted()!!.isAfter(fiveSecondsAgo))
  }
}
