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
