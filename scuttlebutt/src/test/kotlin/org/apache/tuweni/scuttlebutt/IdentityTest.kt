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

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.crypto.sodium.Sodium
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.scuttlebutt.Identity.Companion.fromKeyPair
import org.apache.tuweni.scuttlebutt.Identity.Companion.fromPublicKey
import org.apache.tuweni.scuttlebutt.Identity.Companion.random
import org.apache.tuweni.scuttlebutt.Identity.Companion.randomEd25519
import org.apache.tuweni.scuttlebutt.Identity.Companion.randomSECP256K1
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class)
internal class IdentityTest {
  @Test
  fun testRandom() {
    val random1 = random()
    val random2 = random()
    Assertions.assertNotEquals(random1, random2)
    Assertions.assertNotEquals(random1.hashCode(), random2.hashCode())
  }

  @Test
  fun testRandomSECP256K1() {
    val random1 = randomSECP256K1()
    val random2 = randomSECP256K1()
    Assertions.assertNotEquals(random1, random2)
    Assertions.assertNotEquals(random1.hashCode(), random2.hashCode())
  }

  @Test
  fun testEquality() {
    val kp = Signature.KeyPair.random()
    val id1 = fromKeyPair(kp)
    val id2 = fromKeyPair(kp)
    Assertions.assertEquals(id1, id2)
  }

  @Test
  fun testEqualitySECP256K1() {
    val kp = SECP256K1.KeyPair.random()
    val id1 = fromKeyPair(kp)
    val id2 = fromKeyPair(kp)
    Assertions.assertEquals(id1, id2)
  }

  @Test
  fun testHashCode() {
    val kp = Signature.KeyPair.random()
    val id1 = fromKeyPair(kp)
    val id2 = fromKeyPair(kp)
    Assertions.assertEquals(id1.hashCode(), id2.hashCode())
  }

  @Test
  fun testHashCodeSECP256K1() {
    val kp = SECP256K1.KeyPair.random()
    val id1 = fromKeyPair(kp)
    val id2 = fromKeyPair(kp)
    Assertions.assertEquals(id1.hashCode(), id2.hashCode())
  }

  @Test
  fun testToString() {
    val kp = Signature.KeyPair.random()
    val id = fromKeyPair(kp)
    val builder = StringBuilder()
    builder.append("@")
    builder.append(kp.publicKey().bytes().toBase64String())
    builder.append(".ed25519")
    Assertions.assertEquals(builder.toString(), id.toString())
  }

  @Test
  fun testToStringSECP256K1() {
    val kp = SECP256K1.KeyPair.random()
    val id = fromKeyPair(kp)
    val builder = StringBuilder()
    builder.append("@")
    builder.append(kp.publicKey().bytes().toBase64String())
    builder.append(".secp256k1")
    Assertions.assertEquals(builder.toString(), id.toString())
  }

  @Test
  fun signAndVerify() {
    val kp = Signature.KeyPair
      .fromSeed(
        Signature.Seed
          .fromBytes(Bytes.fromHexString("deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"))
      )
    val message = Bytes.fromHexString("deadbeef")
    val id = fromKeyPair(kp)
    val signature = id.sign(message)
    val verified = id.verify(signature, message)
    Assertions.assertTrue(verified)
    Assertions.assertFalse(id.verify(signature, Bytes.fromHexString("dea3beef")))
  }

  @Test
  fun signAndVerifySECP256K1() {
    val kp = SECP256K1.KeyPair.random()
    val message = Bytes.fromHexString("deadbeef")
    val id = fromKeyPair(kp)
    val signature = id.sign(message)
    val verified = id.verify(signature, message)
    Assertions.assertTrue(verified)
    Assertions.assertFalse(id.verify(signature, Bytes.fromHexString("dea3beef")))
  }

  @Test
  fun testKeyPairAndPublicKey() {
    val kp = Signature.KeyPair.random()
    val id = fromKeyPair(kp)
    val idWithPk = fromPublicKey(kp.publicKey())
    Assertions.assertEquals(id.toCanonicalForm(), idWithPk.toCanonicalForm())
    Assertions.assertEquals(id.toString(), idWithPk.toString())
    val message = Bytes.fromHexString("deadbeef")
    val signature = id.sign(message)
    Assertions.assertTrue(idWithPk.verify(signature, message))
    Assertions.assertEquals(kp.publicKey(), id.ed25519PublicKey())
  }

  @Test
  fun testKeyPairAndPublicKeySECP256K1() {
    val kp = SECP256K1.KeyPair.random()
    val id = fromKeyPair(kp)
    val idWithPk = fromPublicKey(kp.publicKey())
    Assertions.assertEquals(id.toCanonicalForm(), idWithPk.toCanonicalForm())
    Assertions.assertEquals(id.toString(), idWithPk.toString())
    val message = Bytes.fromHexString("deadbeef")
    val signature = id.sign(message)
    Assertions.assertTrue(idWithPk.verify(signature, message))
    Assertions.assertEquals(kp.publicKey(), id.secp256k1PublicKey())
  }

  @Test
  fun curveUnsupported() {
    Assertions.assertThrows(
      UnsupportedOperationException::class.java
    ) {
      random().secp256k1PublicKey()
    }
    Assertions.assertThrows(
      UnsupportedOperationException::class.java
    ) {
      randomSECP256K1().ed25519PublicKey()
    }
  }

  @Test
  fun testRandomEd25519() {
    val random1 = randomEd25519()
    val random2 = randomEd25519()
    Assertions.assertNotEquals(random1, random2)
    Assertions.assertNotEquals(random1.hashCode(), random2.hashCode())
  }

  @Test
  fun testPublicKeyHashCodeAndEquals() {
    val kp = Signature.KeyPair.random()
    val id1 = fromKeyPair(kp)
    val id2 = fromKeyPair(kp)
    Assertions.assertEquals(
      fromPublicKey(id1.ed25519PublicKey()!!),
      fromPublicKey(
        id2.ed25519PublicKey()!!
      )
    )
    Assertions.assertEquals(
      fromPublicKey(id1.ed25519PublicKey()!!).hashCode(),
      fromPublicKey(id2.ed25519PublicKey()!!).hashCode()
    )
  }

  companion object {
    @BeforeAll
    fun checkAvailable() {
      Assumptions.assumeTrue(Sodium.isAvailable(), "Sodium native library is not available")
    }
  }
}
