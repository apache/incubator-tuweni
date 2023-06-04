// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AES256GCMTest {

  private static AES256GCM.Nonce nonce;

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
    assumeTrue(AES256GCM.isAvailable(), "AES256-GSM support is not available");
    nonce = AES256GCM.Nonce.random();
  }

  @BeforeEach
  void incrementNonce() {
    nonce = nonce.increment();
  }

  @Test
  void checkCombinedEncryptDecrypt() {
    AES256GCM.Key key = AES256GCM.Key.random();

    byte[] message = "This is a test message".getBytes(UTF_8);
    byte[] data = "123456".getBytes(UTF_8);

    byte[] cipherText = AES256GCM.encrypt(message, data, key, nonce);
    byte[] clearText = AES256GCM.decrypt(cipherText, data, key, nonce);

    assertNotNull(clearText);
    assertArrayEquals(message, clearText);

    assertNull(AES256GCM.decrypt(cipherText, data, key, nonce.increment()));
  }

  @Test
  void checkCombinedPrecomputedEncryptDecrypt() {
    try (AES256GCM precomputed = AES256GCM.forKey(AES256GCM.Key.random())) {
      byte[] message = "This is a test message".getBytes(UTF_8);
      byte[] data = "123456".getBytes(UTF_8);

      byte[] cipherText = precomputed.encrypt(message, data, nonce);
      byte[] clearText = precomputed.decrypt(cipherText, data, nonce);

      assertNotNull(clearText);
      assertArrayEquals(message, clearText);

      assertNull(precomputed.decrypt(cipherText, data, nonce.increment()));
    }
  }

  @Test
  void checkDetachedEncryptDecrypt() {
    AES256GCM.Key key = AES256GCM.Key.random();

    byte[] message = "This is a test message".getBytes(UTF_8);
    byte[] data = "123456".getBytes(UTF_8);

    DetachedEncryptionResult result = AES256GCM.encryptDetached(message, data, key, nonce);
    byte[] clearText =
        AES256GCM.decryptDetached(result.cipherTextArray(), result.macArray(), data, key, nonce);

    assertNotNull(clearText);
    assertArrayEquals(message, clearText);

    clearText =
        AES256GCM.decryptDetached(
            result.cipherTextArray(), result.macArray(), data, key, nonce.increment());
    assertNull(clearText);
  }

  @Test
  void checkDetachedPrecomputedEncryptDecrypt() {
    try (AES256GCM precomputed = AES256GCM.forKey(AES256GCM.Key.random())) {
      byte[] message = "This is a test message".getBytes(UTF_8);
      byte[] data = "123456".getBytes(UTF_8);

      DetachedEncryptionResult result = precomputed.encryptDetached(message, data, nonce);
      byte[] clearText =
          precomputed.decryptDetached(result.cipherTextArray(), result.macArray(), data, nonce);

      assertNotNull(clearText);
      assertArrayEquals(message, clearText);

      clearText =
          precomputed.decryptDetached(
              result.cipherTextArray(), result.macArray(), data, nonce.increment());
      assertNull(clearText);
    }
  }
}
