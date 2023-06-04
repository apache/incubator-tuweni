// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class XChaCha20Poly1305Test {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
    assumeTrue(
        XChaCha20Poly1305.isAvailable(),
        "XChaCha20Poly1305 support is not available (requires >= 10.0.12");
  }

  @Test
  void checkCombinedEncryptDecrypt() {
    XChaCha20Poly1305.Key key = XChaCha20Poly1305.Key.random();
    XChaCha20Poly1305.Nonce nonce = XChaCha20Poly1305.Nonce.random().increment();

    byte[] message = "This is a test message".getBytes(UTF_8);
    byte[] data = "123456".getBytes(UTF_8);

    byte[] cipherText = XChaCha20Poly1305.encrypt(message, data, key, nonce);
    byte[] clearText = XChaCha20Poly1305.decrypt(cipherText, data, key, nonce);

    assertNotNull(clearText);
    assertArrayEquals(message, clearText);

    assertNull(XChaCha20Poly1305.decrypt(cipherText, data, key, nonce.increment()));
  }

  @Test
  void checkDetachedEncryptDecrypt() {
    XChaCha20Poly1305.Key key = XChaCha20Poly1305.Key.random();
    XChaCha20Poly1305.Nonce nonce = XChaCha20Poly1305.Nonce.random().increment();

    byte[] message = "This is a test message".getBytes(UTF_8);
    byte[] data = "123456".getBytes(UTF_8);

    DetachedEncryptionResult result = XChaCha20Poly1305.encryptDetached(message, data, key, nonce);
    byte[] clearText =
        XChaCha20Poly1305.decryptDetached(
            result.cipherTextArray(), result.macArray(), data, key, nonce);

    assertNotNull(clearText);
    assertArrayEquals(message, clearText);

    clearText =
        XChaCha20Poly1305.decryptDetached(
            result.cipherTextArray(), result.macArray(), data, key, nonce.increment());
    assertNull(clearText);
  }

  @Test
  void checkStreamEncryptDecrypt() {
    assumeTrue(XChaCha20Poly1305.isSecretStreamAvailable());

    XChaCha20Poly1305.Key key = XChaCha20Poly1305.Key.random();

    byte[] message1 = "This is the first message".getBytes(UTF_8);
    byte[] message2 = "This is the second message".getBytes(UTF_8);
    byte[] message3 = "This is the third message".getBytes(UTF_8);

    SecretEncryptionStream ses = XChaCha20Poly1305.openEncryptionStream(key);
    byte[] header = ses.headerArray();
    byte[] cipher1 = ses.push(message1);
    byte[] cipher2 = ses.push(message2);
    byte[] cipher3 = ses.pushLast(message3);

    SecretDecryptionStream sds = XChaCha20Poly1305.openDecryptionStream(key, header);
    assertArrayEquals(message1, sds.pull(cipher1));
    assertFalse(sds.isComplete());
    assertArrayEquals(message2, sds.pull(cipher2));
    assertFalse(sds.isComplete());
    assertArrayEquals(message3, sds.pull(cipher3));
    assertTrue(sds.isComplete());
  }
}
