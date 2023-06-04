// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethclient

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.peer.repository.memory.MemoryPeerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class DNSClientRunTest {
  @Test
  fun testStartAndStop(@VertxInstance vertx: Vertx) {
    val client = DNSClient(
      vertx,
      DNSConfigurationImpl("default", "foo", "example.com", 1000, null),
      MapKeyValueStore.open(),
      MemoryPeerRepository()
    )
    runBlocking {
      client.start()
      client.stop()
    }
  }

  @Test
  fun changeSeq(@VertxInstance vertx: Vertx) {
    val client = DNSClient(
      vertx,
      DNSConfigurationImpl("default", "foo", "example.com", 1000, null),
      MapKeyValueStore.open(),
      MemoryPeerRepository()
    )
    runBlocking {
      client.seq(42L)
      assertEquals(42L, client.seq())
    }
  }
}
