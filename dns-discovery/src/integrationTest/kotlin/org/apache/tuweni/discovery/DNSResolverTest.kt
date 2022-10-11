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
      visitor
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
      visitor
    )
    assertTrue(nodes.size > 0)
    println(nodes.size)
  }
}
