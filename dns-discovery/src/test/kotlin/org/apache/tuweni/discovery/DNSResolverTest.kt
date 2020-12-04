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

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.xbill.DNS.SimpleResolver
import java.time.Duration

class DNSResolverTest {

  @Test
  fun testBadHost() {
    val dnsResolver = SimpleResolver("127.0.0.2")
    dnsResolver.timeout = Duration.ofMillis(100)
    val resolver = DNSResolver(resolver = dnsResolver)
    val record = resolver.resolveRecordRaw("longstring.example.com")
    assertNull(record)
  }

  @Test
  fun testNoTXTEntry() {
    val resolver = DNSResolver()
    val record = resolver.resolveRecordRaw("foo.tuweni.apache.org")
    assertNull(record)
  }
}
