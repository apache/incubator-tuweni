/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.scuttlebutt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.crypto.sodium.Sodium;
import org.apache.tuweni.junit.BouncyCastleExtension;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class IdentityTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void testRandom() {
    Identity random1 = Identity.random();
    Identity random2 = Identity.random();
    assertNotEquals(random1, random2);
    assertNotEquals(random1.hashCode(), random2.hashCode());
  }

  @Test
  void testRandomSECP256K1() {
    Identity random1 = Identity.randomSECP256K1();
    Identity random2 = Identity.randomSECP256K1();
    assertNotEquals(random1, random2);
    assertNotEquals(random1.hashCode(), random2.hashCode());
  }

  @Test
  void testEquality() {
    Signature.KeyPair kp = Signature.KeyPair.random();
    Identity id1 = Identity.fromKeyPair(kp);
    Identity id2 = Identity.fromKeyPair(kp);
    assertEquals(id1, id2);
  }

  @Test
  void testEqualitySECP256K1() {
    SECP256K1.KeyPair kp = SECP256K1.KeyPair.random();
    Identity id1 = Identity.fromKeyPair(kp);
    Identity id2 = Identity.fromKeyPair(kp);
    assertEquals(id1, id2);
  }

  @Test
  void testHashCode() {
    Signature.KeyPair kp = Signature.KeyPair.random();
    Identity id1 = Identity.fromKeyPair(kp);
    Identity id2 = Identity.fromKeyPair(kp);
    assertEquals(id1.hashCode(), id2.hashCode());
  }

  @Test
  void testHashCodeSECP256K1() {
    SECP256K1.KeyPair kp = SECP256K1.KeyPair.random();
    Identity id1 = Identity.fromKeyPair(kp);
    Identity id2 = Identity.fromKeyPair(kp);
    assertEquals(id1.hashCode(), id2.hashCode());
  }

  @Test
  void testToString() {
    Signature.KeyPair kp = Signature.KeyPair.random();
    Identity id = Identity.fromKeyPair(kp);
    StringBuilder builder = new StringBuilder();
    builder.append("@");
    builder.append(kp.publicKey().bytes().toBase64String());
    builder.append(".ed25519");
    assertEquals(builder.toString(), id.toString());
  }

  @Test
  void testToStringSECP256K1() {
    SECP256K1.KeyPair kp = SECP256K1.KeyPair.random();
    Identity id = Identity.fromKeyPair(kp);
    StringBuilder builder = new StringBuilder();
    builder.append("@");
    builder.append(kp.publicKey().bytes().toBase64String());
    builder.append(".secp256k1");
    assertEquals(builder.toString(), id.toString());
  }

  @Test
  void signAndVerify() {
    Signature.KeyPair kp = Signature.KeyPair.fromSeed(
        Signature.Seed
            .fromBytes(Bytes.fromHexString("deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef")));
    Bytes message = Bytes.fromHexString("deadbeef");
    Identity id = Identity.fromKeyPair(kp);
    Bytes signature = id.sign(message);
    boolean verified = id.verify(signature, message);

    assertTrue(verified);
    assertFalse(id.verify(signature, Bytes.fromHexString("dea3beef")));
  }

  @Test
  void signAndVerifySECP256K1() {
    SECP256K1.KeyPair kp = SECP256K1.KeyPair.random();
    Bytes message = Bytes.fromHexString("deadbeef");
    Identity id = Identity.fromKeyPair(kp);
    Bytes signature = id.sign(message);
    boolean verified = id.verify(signature, message);

    assertTrue(verified);
    assertFalse(id.verify(signature, Bytes.fromHexString("dea3beef")));
  }

  @Test
  void testKeyPairAndPublicKey() {
    Signature.KeyPair kp = Signature.KeyPair.random();
    Identity id = Identity.fromKeyPair(kp);
    Identity idWithPk = Identity.fromPublicKey(kp.publicKey());
    assertEquals(id.toCanonicalForm(), idWithPk.toCanonicalForm());
    assertEquals(id.toString(), idWithPk.toString());
    Bytes message = Bytes.fromHexString("deadbeef");
    Bytes signature = id.sign(message);
    assertTrue(idWithPk.verify(signature, message));
    assertEquals(kp.publicKey(), id.ed25519PublicKey());
  }

  @Test
  void testKeyPairAndPublicKeySECP256K1() {
    SECP256K1.KeyPair kp = SECP256K1.KeyPair.random();
    Identity id = Identity.fromKeyPair(kp);
    Identity idWithPk = Identity.fromPublicKey(kp.publicKey());
    assertEquals(id.toCanonicalForm(), idWithPk.toCanonicalForm());
    assertEquals(id.toString(), idWithPk.toString());
    Bytes message = Bytes.fromHexString("deadbeef");
    Bytes signature = id.sign(message);
    assertTrue(idWithPk.verify(signature, message));
    assertEquals(kp.publicKey(), id.secp256k1PublicKey());
  }

  @Test
  void curveUnsupported() {
    assertThrows(UnsupportedOperationException.class, () -> Identity.random().secp256k1PublicKey());
    assertThrows(UnsupportedOperationException.class, () -> Identity.randomSECP256K1().ed25519PublicKey());
  }


}
