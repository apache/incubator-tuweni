// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.discovery

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
@ExtendWith(VertxExtension::class)
class DNSResolverTest {

  @Test
  fun testBadHost(@VertxInstance vertx: Vertx) = runBlocking {
    val resolver = DNSResolver(dnsServer = "127.0.0.2", vertx = vertx)
    val record = resolver.resolveRecordRaw("longstring.example.com")
    assertNull(record)
  }

  @Test
  fun testNoTXTEntry(@VertxInstance vertx: Vertx) = runBlocking {
    val resolver = DNSResolver(vertx = vertx)
    val record = resolver.resolveRecordRaw("foo.tuweni.apache.org")
    assertNull(record)
  }
}
