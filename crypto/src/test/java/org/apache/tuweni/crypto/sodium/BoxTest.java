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
package org.apache.tuweni.crypto.sodium;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BoxTest {

  private static Box.Seed seed;
  private static Box.Nonce nonce;

  @BeforeAll
  static void setup() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
    nonce = Box.Nonce.random();
    // @formatter:off
    seed = Box.Seed.fromBytes(new byte[] {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f
    });
    // @formatter:on
  }

  @BeforeEach
  void incrementNonce() {
    nonce = nonce.increment();
  }

  @Test
  void checkCombinedEncryptDecrypt() {
    Box.KeyPair aliceKeyPair = Box.KeyPair.random();
    Box.KeyPair bobKeyPair = Box.KeyPair.fromSeed(seed);

    byte[] message = "This is a test message".getBytes(UTF_8);

    byte[] cipherText = Box.encrypt(message, aliceKeyPair.publicKey(), bobKeyPair.secretKey(), nonce);
    byte[] clearText = Box.decrypt(cipherText, bobKeyPair.publicKey(), aliceKeyPair.secretKey(), nonce);

    assertNotNull(clearText);
    assertArrayEquals(message, clearText);

    clearText = Box.decrypt(cipherText, bobKeyPair.publicKey(), aliceKeyPair.secretKey(), nonce.increment());
    assertNull(clearText);

    Box.KeyPair otherKeyPair = Box.KeyPair.random();
    clearText = Box.decrypt(cipherText, otherKeyPair.publicKey(), bobKeyPair.secretKey(), nonce);
    assertNull(clearText);
  }

  @Test
  void checkCombinedPrecomputedEncryptDecrypt() {
    Box.KeyPair aliceKeyPair = Box.KeyPair.random();
    Box.KeyPair bobKeyPair = Box.KeyPair.random();

    byte[] message = "This is a test message".getBytes(UTF_8);
    byte[] cipherText;

    try (Box precomputed = Box.forKeys(aliceKeyPair.publicKey(), bobKeyPair.secretKey())) {
      cipherText = precomputed.encrypt(message, nonce);
    }

    byte[] clearText = Box.decrypt(cipherText, bobKeyPair.publicKey(), aliceKeyPair.secretKey(), nonce);

    assertNotNull(clearText);
    assertArrayEquals(message, clearText);

    try (Box precomputed = Box.forKeys(bobKeyPair.publicKey(), aliceKeyPair.secretKey())) {
      clearText = precomputed.decrypt(cipherText, nonce);

      assertNotNull(clearText);
      assertArrayEquals(message, clearText);

      assertNull(precomputed.decrypt(cipherText, nonce.increment()));
    }

    Box.KeyPair otherKeyPair = Box.KeyPair.random();
    try (Box precomputed = Box.forKeys(otherKeyPair.publicKey(), bobKeyPair.secretKey())) {
      assertNull(precomputed.decrypt(cipherText, nonce));
    }
  }

  @Test
  void checkDetachedEncryptDecrypt() {
    Box.KeyPair aliceKeyPair = Box.KeyPair.random();
    Box.KeyPair bobKeyPair = Box.KeyPair.random();

    byte[] message = "This is a test message".getBytes(UTF_8);

    DetachedEncryptionResult result =
        Box.encryptDetached(message, aliceKeyPair.publicKey(), bobKeyPair.secretKey(), nonce);
    byte[] clearText = Box.decryptDetached(
        result.cipherTextArray(),
        result.macArray(),
        bobKeyPair.publicKey(),
        aliceKeyPair.secretKey(),
        nonce);

    assertNotNull(clearText);
    assertArrayEquals(message, clearText);

    clearText = Box.decryptDetached(
        result.cipherTextArray(),
        result.macArray(),
        bobKeyPair.publicKey(),
        aliceKeyPair.secretKey(),
        nonce.increment());
    assertNull(clearText);

    Box.KeyPair otherKeyPair = Box.KeyPair.random();
    clearText = Box.decryptDetached(
        result.cipherTextArray(),
        result.macArray(),
        otherKeyPair.publicKey(),
        bobKeyPair.secretKey(),
        nonce);
    assertNull(clearText);
  }

  @Test
  void checkDetachedPrecomputedEncryptDecrypt() {
    Box.KeyPair aliceKeyPair = Box.KeyPair.random();
    Box.KeyPair bobKeyPair = Box.KeyPair.random();

    byte[] message = "This is a test message".getBytes(UTF_8);
    DetachedEncryptionResult result;

    try (Box precomputed = Box.forKeys(aliceKeyPair.publicKey(), bobKeyPair.secretKey())) {
      result = precomputed.encryptDetached(message, nonce);
    }

    byte[] clearText = Box.decryptDetached(
        result.cipherTextArray(),
        result.macArray(),
        bobKeyPair.publicKey(),
        aliceKeyPair.secretKey(),
        nonce);

    assertNotNull(clearText);
    assertArrayEquals(message, clearText);

    try (Box precomputed = Box.forKeys(bobKeyPair.publicKey(), aliceKeyPair.secretKey())) {
      clearText = precomputed.decryptDetached(result.cipherTextArray(), result.macArray(), nonce);

      assertNotNull(clearText);
      assertArrayEquals(message, clearText);

      assertNull(precomputed.decryptDetached(result.cipherTextArray(), result.macArray(), nonce.increment()));
    }

    Box.KeyPair otherKeyPair = Box.KeyPair.random();
    try (Box precomputed = Box.forKeys(otherKeyPair.publicKey(), bobKeyPair.secretKey())) {
      assertNull(precomputed.decryptDetached(result.cipherTextArray(), result.macArray(), nonce));
    }
  }

  @Test
  void checkBoxKeyPairForSignatureKeyPair() {
    Signature.KeyPair signKeyPair = Signature.KeyPair.random();
    Box.KeyPair boxKeyPair = Box.KeyPair.forSignatureKeyPair(signKeyPair);
    assertNotNull(boxKeyPair);
  }

  @Test
  void checkBoxKeysForSignatureKeys() {
    Signature.KeyPair keyPair = Signature.KeyPair.random();
    Box.PublicKey boxPubKey = Box.PublicKey.forSignaturePublicKey(keyPair.publicKey());
    Box.SecretKey boxSecretKey = Box.SecretKey.forSignatureSecretKey(keyPair.secretKey());
    assertEquals(boxPubKey, Box.KeyPair.forSecretKey(boxSecretKey).publicKey());

    Box.KeyPair boxKeyPair = Box.KeyPair.forSignatureKeyPair(keyPair);
    assertEquals(boxKeyPair, Box.KeyPair.forSecretKey(boxSecretKey));
  }
}
