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
