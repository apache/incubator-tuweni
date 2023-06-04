// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
