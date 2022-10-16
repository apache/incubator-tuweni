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
package org.apache.tuweni.scuttlebutt

import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.crypto.sodium.Sodium
import org.apache.tuweni.scuttlebutt.Identity.Companion.random
import org.apache.tuweni.scuttlebutt.Invite.Companion.fromCanonicalForm
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class InviteTest() {

  companion object {
    @BeforeAll
    @JvmStatic
    fun checkAvailable() {
      Assumptions.assumeTrue(Sodium.isAvailable(), "Sodium native library is not available")
    }
  }

  @Test
  fun invalidPort() {
    Assertions.assertThrows(
      IllegalArgumentException::class.java
    ) {
      Invite(
        "localhost",
        -1,
        random(),
        Signature.Seed.random()
      )
    }
  }

  @Test
  fun testToString() {
    val identity = random()
    val seed = Signature.Seed.random()
    val invite = Invite("localhost", 8008, identity, seed)
    Assertions.assertEquals(
      "localhost:8008:" +
        "@" +
        identity.publicKeyAsBase64String() +
        "." +
        identity.curveName() +
        "~" +
        seed.bytes().toBase64String(),
      invite.toString()
    )
  }

  @Test
  fun testParseFromCanonicalValid() {
    val testInvite =
      "fake.address.com:8009:@MS/HpeAess0EGruiZjfnc+x+FkPq7qoMqSD4SdvTCtM=.ed25519~IJubWEcZM6usWncF/Lu26CyI3ZiovcHjh9+kBI1hiKI="
    try {
      val invite = fromCanonicalForm(testInvite)
      Assertions.assertEquals(invite.host, "fake.address.com")
      Assertions.assertEquals(invite.port, 8009)
      Assertions.assertEquals(invite.identity.publicKeyAsBase64String(), "MS/HpeAess0EGruiZjfnc+x+FkPq7qoMqSD4SdvTCtM=")
      Assertions.assertEquals(invite.seedKey.bytes().toBase64String(), "IJubWEcZM6usWncF/Lu26CyI3ZiovcHjh9+kBI1hiKI=")
      Assertions.assertEquals(invite.toCanonicalForm(), testInvite)
    } catch (malformedInviteCodeException: MalformedInviteCodeException) {
      Assertions.fail<Any>("Exception while parsing into canonical form: " + malformedInviteCodeException.message)
    }
  }

  @Test
  fun testParseFromCanonicalMissingHost() {
    val testInvite =
      ":@MS/HpeAess0EGruiZjfnc+x+FkPq7qoMqSD4SdvTCtM=.ed25519~IJubWEcZM6usWncF/Lu26CyI3ZiovcHjh9+kBI1hiKI="
    try {
      fromCanonicalForm(testInvite)
      Assertions.fail<Any>("Exception expected when host missing from invite code.")
    } catch (ignored: MalformedInviteCodeException) {
    }
  }

  @Test
  fun testParseFromCanonicalMissingSeed() {
    val testInvite = "fake.address.com:8009:@MS/HpeAess0EGruiZjfnc+x+FkPq7qoMqSD4SdvTCtM=.ed25519"
    try {
      fromCanonicalForm(testInvite)
      Assertions.fail<Any>("Exception expected when seed missing from invite code.")
    } catch (ignored: MalformedInviteCodeException) {
    }
  }
}
