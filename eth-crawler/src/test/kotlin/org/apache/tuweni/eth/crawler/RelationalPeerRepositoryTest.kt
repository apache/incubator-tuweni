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
package org.apache.tuweni.eth.crawler

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.Endpoint
import org.apache.tuweni.junit.BouncyCastleExtension
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@Disabled("cannot work in CI")
@ExtendWith(BouncyCastleExtension::class)
class RelationalPeerRepositoryTest {

  var dataSource: DataSource? = null

  val meter = SdkMeterProvider.builder().build()["test"]

  @BeforeEach
  fun before() {
    val provider = EmbeddedPostgres.builder().start()
    dataSource = provider.postgresDatabase
    val flyway = Flyway.configure().dataSource(dataSource).load()
    flyway.migrate()
  }

  @AfterEach
  fun after() {
    dataSource = null
  }

  @Test
  fun testGetNewPeer() = runBlocking {
    val repository = RelationalPeerRepository(dataSource!!)
    val peer = repository.get("localhost", 30303, SECP256K1.KeyPair.random().publicKey())
    assertNotNull(peer)
  }

  @Test
  fun testUpdateEndpoint() = runBlocking {
    val repository = RelationalPeerRepository(dataSource!!)
    val repository2 = RelationalPeerRepository(dataSource!!)
    val peer = repository.get("localhost", 30303, SECP256K1.KeyPair.random().publicKey())
    assertNotNull(peer)
    val lastSeen = 32L
    val e = peer.updateEndpoint(Endpoint("example.com", 30302), lastSeen, System.currentTimeMillis())
    assertNotNull(e)
    val retrieved = repository2.get("example.com", 30302, peer.nodeId)
    assertEquals(lastSeen, retrieved.lastSeen)
  }
}
