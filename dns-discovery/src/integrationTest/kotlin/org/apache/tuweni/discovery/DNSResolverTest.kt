// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.discovery

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class, VertxExtension::class)
class DNSResolverTest {

  @Test
  fun testQueryTXT(@VertxInstance vertx: Vertx) = runBlocking {
    val resolver = DNSResolver(vertx = vertx)
    val rec = resolver.resolveRecordRaw("all.goerli.ethdisco.net")
    assertNotNull(rec)
  }

  @Disabled("too unstable on CI")
  @Test
  fun resolveAllGoerliNodes(@VertxInstance vertx: Vertx) = runBlocking {
    val resolver = DNSResolver(vertx = vertx)
    val nodes = mutableListOf<EthereumNodeRecord>()
    val visitor = object : DNSVisitor {
      override fun visit(enr: EthereumNodeRecord): Boolean {
        nodes.add(enr)
        return true
      }
    }

    resolver.visitTree(
      "enrtree://AKA3AM6LPBYEUDMVNU3BSVQJ5AD45Y7YPOHJLEF6W26QOE4VTUDPE@all.goerli.ethdisco.net",
      visitor,
    )
    assertTrue(nodes.size > 0)
  }

  @Disabled("too expensive for CI")
  @Test
  fun resolveAllMainnetNodes(@VertxInstance vertx: Vertx) = runBlocking {
    val resolver = DNSResolver(vertx = vertx)
    val nodes = mutableListOf<EthereumNodeRecord>()
    val visitor = object : DNSVisitor {
      override fun visit(enr: EthereumNodeRecord): Boolean {
        nodes.add(enr)
        return true
      }
    }

    resolver.visitTree(
      "enrtree://AKA3AM6LPBYEUDMVNU3BSVQJ5AD45Y7YPOHJLEF6W26QOE4VTUDPE@all.mainnet.ethdisco.net",
      visitor,
    )
    assertTrue(nodes.size > 0)
    println(nodes.size)
  }
}
