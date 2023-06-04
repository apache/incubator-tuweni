// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.v5

import io.vertx.core.Vertx
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.io.Base64URLSafe
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetSocketAddress

/**
 * Test a developer can run from their machine to contact a remote server.
 */
@Disabled
@ExtendWith(BouncyCastleExtension::class, VertxExtension::class)
class MedallaTest {

  @Test
  fun testConnect(@VertxInstance vertx: Vertx) = runBlocking {
    val enrRec =
      "enr:-LK4QC3FCb7-JTNRiWAezECk_QUJc9c2IkJA1-EAmqAA5wmdbPWsAeRpnMXKRJqOYG0TE99ycB1nOb9y26mjb" +
        "_UoHS4Bh2F0dG5ldHOIAAAAAAAAAACEZXRoMpDnp11aAAAAAf__________gmlkgnY0gmlwhDMPYfCJc2VjcDI1N" +
        "msxoQOmDQryZJApMwIT-dQAbxjvxLbPzyKn9GFk5dqam4MDTYN0Y3CCIyiDdWRwgiMo"

    val service = DiscoveryService.open(
      vertx,
      SECP256K1.KeyPair.random(),
      localPort = 0,
      bindAddress = InetSocketAddress("0.0.0.0", 10000),
      bootstrapENRList = listOf(enrRec)
    )
    service.start().join()

    kotlinx.coroutines.delay(10000)
    (1..8).forEach {
      service.requestNodes(it)
    }
  }

  @Test
  fun testServer(@VertxInstance vertx: Vertx) = runBlocking {
    val keyPair = SECP256K1.KeyPair.random()
    val service = DiscoveryService.open(
      vertx,
      keyPair,
      localPort = 10000,
      bindAddress = InetSocketAddress("192.168.88.236", 10000),
      bootstrapENRList = emptyList()
    )
    service.start().join()
    println(Base64URLSafe.encode(service.enr().toRLP()))
    delay(500000)
  }
}
