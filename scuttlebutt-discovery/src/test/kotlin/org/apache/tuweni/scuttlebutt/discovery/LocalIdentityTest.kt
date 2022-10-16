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
package org.apache.tuweni.scuttlebutt.discovery

import org.apache.tuweni.crypto.sodium.Sodium
import org.apache.tuweni.scuttlebutt.Identity
import org.apache.tuweni.scuttlebutt.discovery.LocalIdentity.Companion.fromString
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class LocalIdentityTest {

  companion object {
    @JvmStatic
    @BeforeAll
    fun checkAvailable() {
      Assumptions.assumeTrue(Sodium.isAvailable(), "Sodium native library is not available")
    }
  }

  @Test
  fun localIdentityInvalidPort() {
    Assertions.assertThrows(
      IllegalArgumentException::class.java
    ) {
      LocalIdentity(
        "0.0.0.0",
        450000,
        Identity.random()
      )
    }
  }

  @Test
  fun localIdentityCanonicalForm() {
    val id = Identity.random()
    val localId = LocalIdentity("0.0.0.0", 45000, id)
    Assertions.assertEquals("net:0.0.0.0:45000~shs:" + id.publicKeyAsBase64String(), localId.toCanonicalForm())
    Assertions.assertEquals(localId.toCanonicalForm(), localId.toString())
  }

  @Test
  fun localIdentityFromString() {
    val id = Identity.random()
    val localId = LocalIdentity("0.0.0.0", 45000, id)
    val fromString = fromString("net:0.0.0.0:45000~shs:" + id.publicKeyAsBase64String())
    Assertions.assertEquals(fromString, localId)
  }

  @Test
  fun malformedIdentity() {
    val id = Identity.random()
    Assertions.assertNull(fromString("nt:0.0.0.0:45000~shs:" + id.publicKeyAsBase64String()))
    Assertions.assertNull(fromString("net:0.0.0.0:45000~ss:" + id.publicKeyAsBase64String()))
    Assertions.assertNull(fromString("nt:0.0.0.0:45000~shs:" + id.publicKeyAsBase64String().substring(12)))
  }
}
