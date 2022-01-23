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
package org.apache.tuweni.eth.crawler.rest

import org.apache.tuweni.eth.crawler.ClientIdInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClientIdInfoTest {

  @Test
  fun testNoLabel() {
    val clientInfo = ClientIdInfo("Parity-Ethereum/v2.7.2-stable-2662d19-20200206/x86_64-unknown-linux-gnu/rustc1.41.0")
    assertEquals("Parity-Ethereum", clientInfo.name)
    assertEquals("", clientInfo.label)
    assertEquals("v2.7.2-stable-2662d19-20200206", clientInfo.version)
    assertEquals("x86_64-unknown-linux-gnu", clientInfo.os)
    assertEquals("rustc1.41.0", clientInfo.compiler)
  }

  @Test
  fun testWithLabel() {
    val clientInfo = ClientIdInfo("OpenEthereum/Bob Ross/v3.0.1-stable-8ca8089-20200601/x86_64-unknown-linux-gnu/rustc1.43.1")
    assertEquals("OpenEthereum", clientInfo.name)
    assertEquals("Bob Ross", clientInfo.label)
    assertEquals("v3.0.1-stable-8ca8089-20200601", clientInfo.version)
    assertEquals("x86_64-unknown-linux-gnu", clientInfo.os)
    assertEquals("rustc1.43.1", clientInfo.compiler)
  }

  @Test
  fun testMalformed() {
    val clientInfo = ClientIdInfo("Foo Bar 1.23")
    assertEquals("Foo Bar 1.23", clientInfo.name)
    assertEquals("", clientInfo.label)
    assertEquals("", clientInfo.version)
    assertEquals("", clientInfo.os)
    assertEquals("", clientInfo.compiler)
  }
}
