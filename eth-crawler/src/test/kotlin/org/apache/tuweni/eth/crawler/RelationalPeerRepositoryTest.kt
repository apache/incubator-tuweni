// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
