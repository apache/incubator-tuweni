// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.tuweni.bytes.Bytes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SignatureTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void testEqualityAndRecovery() {
    Signature.KeyPair kp = Signature.KeyPair.random();
    Signature.KeyPair otherKp = Signature.KeyPair.forSecretKey(kp.secretKey());
    assertEquals(kp, otherKp);
  }

  @Test
  void checkDetachedSignVerify() {
    Signature.KeyPair kp = Signature.KeyPair.random();
    Bytes signature = Signature.signDetached(Bytes.fromHexString("deadbeef"), kp.secretKey());
    boolean result =
        Signature.verifyDetached(Bytes.fromHexString("deadbeef"), signature, kp.publicKey());
    assertTrue(result);
  }

  @Test
  void checkSignAndVerify() {
    Signature.KeyPair keyPair = Signature.KeyPair.random();
    Bytes signed = Signature.sign(Bytes.fromHexString("deadbeef"), keyPair.secretKey());
    Bytes messageBytes = Signature.verify(signed, keyPair.publicKey());
    assertEquals(Bytes.fromHexString("deadbeef"), messageBytes);
  }

  @Test
  void testDestroyPublicKey() {
    Signature.KeyPair keyPair = Signature.KeyPair.random();
    Signature.PublicKey sigPubKey = Signature.PublicKey.fromBytes(keyPair.publicKey().bytes());
    sigPubKey.destroy();
    assertTrue(sigPubKey.isDestroyed());
    assertFalse(keyPair.publicKey().isDestroyed());
  }
}
