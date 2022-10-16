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
package org.apache.tueni.scuttlebutt.discovery

import io.vertx.core.Vertx
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.crypto.sodium.Sodium
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.apache.tuweni.scuttlebutt.Identity
import org.apache.tuweni.scuttlebutt.discovery.LocalIdentity
import org.apache.tuweni.scuttlebutt.discovery.ScuttlebuttLocalDiscoveryService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
internal class ScuttlebuttLocalDiscoveryServiceTest {
  companion object {
    @JvmStatic
    @BeforeAll
    fun checkAvailable() {
      Assumptions.assumeTrue(Sodium.isAvailable(), "Sodium native library is not available")
    }
  }

  @Test
  @Throws(Exception::class)
  fun startStop(@VertxInstance vertx: Vertx) = runBlocking {
    val service = ScuttlebuttLocalDiscoveryService(vertx, 0, 0, "127.0.0.1", "233.0.10.0")
    service.start()
    service.stop()
  }

  @Test
  @Throws(Exception::class)
  fun startStart(@VertxInstance vertx: Vertx) = runBlocking {
    val service = ScuttlebuttLocalDiscoveryService(vertx, 0, 0, "127.0.0.1", "233.0.10.0")
    service.start()
    service.start()
    service.stop()
  }

  @Test
  @Throws(Exception::class)
  fun invalidMulticastAddress(@VertxInstance vertx: Vertx) {
    Assertions.assertThrows(
      IllegalArgumentException::class.java
    ) {
      ScuttlebuttLocalDiscoveryService(
        vertx,
        8008,
        0,
        "127.0.0.1",
        "10.0.0.0"
      )
    }
  }

  @Test
  @Throws(Exception::class)
  fun stopFirst(@VertxInstance vertx: Vertx) = runBlocking {
    val service = ScuttlebuttLocalDiscoveryService(vertx, 0, 0, "127.0.0.1", "233.0.10.0")
    service.stop()
    service.start()
    service.stop()
  }

  @Test
  @Throws(Exception::class)
  fun broadcastAndListen(@VertxInstance vertx: Vertx?) = runBlocking {
    val service = ScuttlebuttLocalDiscoveryService(vertx!!, 18008, 18009, "127.0.0.1", "127.0.0.1", false)
    val service2 = ScuttlebuttLocalDiscoveryService(vertx, 18009, 18008, "127.0.0.1", "127.0.0.1", false)
    try {
      service2.start()
      val ref = AtomicReference<LocalIdentity?>()
      service2.addListener { newValue: LocalIdentity? ->
        ref.set(
          newValue
        )
      }
      val localId = LocalIdentity("10.0.0.1", 10000, Identity.random())
      service.addIdentityToBroadcastList(localId)
      service.start()
      service.broadcast()
      delay(1000)
      Assertions.assertNotNull(ref.get())
      Assertions.assertEquals(localId, ref.get())
    } finally {
      listOf(
        async {
          service2.stop()
        },
        async {
          service.stop()
        }
      ).awaitAll()
    }
  }
}
