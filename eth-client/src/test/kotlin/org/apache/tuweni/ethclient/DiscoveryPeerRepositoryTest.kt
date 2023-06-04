// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethclient

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.peer.repository.memory.MemoryPeerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI

@ExtendWith(BouncyCastleExtension::class)
class DiscoveryPeerRepositoryTest {

  @Test
  fun testPeerAddAndGetPeer() {
    val repo = DiscoveryPeerRepository(MemoryPeerRepository())
    val key = SECP256K1.KeyPair.random().publicKey()
    runBlocking {
      val peer = repo.get("enode://${key.toHexString()}@127.0.0.1:3000")
      assertEquals(3000, peer.endpoint.udpPort)
      assertEquals("127.0.0.1", peer.endpoint.address)
      assertEquals(key, peer.nodeId)
    }
  }

  @Test
  fun testGetEquals() {
    val repo = DiscoveryPeerRepository(MemoryPeerRepository())
    val key = SECP256K1.KeyPair.random().publicKey()
    val peer = repo.getAsync("enode://${key.toHexString()}@127.0.0.1:3000").get()!!
    runBlocking {
      val peer2 = repo.get("enode://${key.toHexString()}@127.0.0.1:3000")
      val peer3 = repo.get(URI("enode://${key.toHexString()}@127.0.0.1:3000"))
      val peer4 = repo.get("127.0.0.1", 3000, key)
      assertEquals(peer, peer2)
      assertEquals(peer, peer3)
      assertEquals(peer, peer4)
    }
  }
}
