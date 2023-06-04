// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SecretBoxTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void checkCombinedEncryptDecrypt() {
    SecretBox.Key key = SecretBox.Key.random();
    SecretBox.Nonce nonce = SecretBox.Nonce.random().increment();

    byte[] message = "This is a test message".getBytes(UTF_8);

    byte[] cipherText = SecretBox.encrypt(message, key, nonce);
    byte[] clearText = SecretBox.decrypt(cipherText, key, nonce);

    assertNotNull(clearText);
    assertArrayEquals(message, clearText);

    assertNull(SecretBox.decrypt(cipherText, key, nonce.increment()));
    SecretBox.Key otherKey = SecretBox.Key.random();
    assertNull(SecretBox.decrypt(cipherText, otherKey, nonce));
  }

  @Test
  void checkCombinedEncryptDecryptEmptyMessage() {
    SecretBox.Key key = SecretBox.Key.random();
    SecretBox.Nonce nonce = SecretBox.Nonce.random().increment();

    byte[] cipherText = SecretBox.encrypt(new byte[0], key, nonce);
    byte[] clearText = SecretBox.decrypt(cipherText, key, nonce);

    assertNotNull(clearText);
    assertEquals(0, clearText.length);
  }

  @Test
  void checkDetachedEncryptDecrypt() {
    SecretBox.Key key = SecretBox.Key.random();
    SecretBox.Nonce nonce = SecretBox.Nonce.random().increment();

    byte[] message = "This is a test message".getBytes(UTF_8);

    DetachedEncryptionResult result = SecretBox.encryptDetached(message, key, nonce);
    byte[] clearText =
        SecretBox.decryptDetached(result.cipherTextArray(), result.macArray(), key, nonce);

    assertNotNull(clearText);
    assertArrayEquals(message, clearText);

    assertNull(
        SecretBox.decryptDetached(
            result.cipherTextArray(), result.macArray(), key, nonce.increment()));
    SecretBox.Key otherKey = SecretBox.Key.random();
    assertNull(
        SecretBox.decryptDetached(result.cipherTextArray(), result.macArray(), otherKey, nonce));
  }

  @Test
  void checkDetachedEncryptDecryptEmptyMessage() {
    SecretBox.Key key = SecretBox.Key.random();
    SecretBox.Nonce nonce = SecretBox.Nonce.random().increment();

    DetachedEncryptionResult result = SecretBox.encryptDetached(new byte[0], key, nonce);
    byte[] clearText =
        SecretBox.decryptDetached(result.cipherTextArray(), result.macArray(), key, nonce);

    assertNotNull(clearText);
    assertEquals(0, clearText.length);
  }

  @Test
  void checkCombinedEncryptDecryptWithPassword() {
    String password = "a random password";

    byte[] message = "This is a test message".getBytes(UTF_8);

    byte[] cipherText =
        SecretBox.encrypt(
            message,
            password,
            PasswordHash.interactiveOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            PasswordHash.Algorithm.recommended());
    byte[] clearText =
        SecretBox.decrypt(
            cipherText,
            password,
            PasswordHash.interactiveOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            PasswordHash.Algorithm.recommended());

    assertNotNull(clearText);
    assertArrayEquals(message, clearText);

    String otherPassword = "a different password";
    assertNull(
        SecretBox.decrypt(
            cipherText,
            otherPassword,
            PasswordHash.interactiveOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            PasswordHash.Algorithm.recommended()));
  }

  @Test
  void checkCombinedEncryptDecryptEmptyMessageWithPassword() {
    String password = "a random password";

    byte[] cipherText =
        SecretBox.encrypt(
            new byte[0],
            password,
            PasswordHash.interactiveOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            PasswordHash.Algorithm.recommended());
    byte[] clearText =
        SecretBox.decrypt(
            cipherText,
            password,
            PasswordHash.interactiveOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            PasswordHash.Algorithm.recommended());

    assertNotNull(clearText);
    assertEquals(0, clearText.length);
  }

  @Test
  void checkDetachedEncryptDecryptWithPassword() {
    String password = "a random password";

    byte[] message = "This is a test message".getBytes(UTF_8);

    DetachedEncryptionResult result =
        SecretBox.encryptDetached(
            message,
            password,
            PasswordHash.interactiveOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            PasswordHash.Algorithm.recommended());
    byte[] clearText =
        SecretBox.decryptDetached(
            result.cipherTextArray(),
            result.macArray(),
            password,
            PasswordHash.interactiveOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            PasswordHash.Algorithm.recommended());

    assertNotNull(clearText);
    assertArrayEquals(message, clearText);

    String otherPassword = "a different password";
    assertNull(
        SecretBox.decryptDetached(
            result.cipherTextArray(),
            result.macArray(),
            otherPassword,
            PasswordHash.interactiveOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            PasswordHash.Algorithm.recommended()));
  }

  @Test
  void checkDetachedEncryptDecryptEmptyMessageWithPassword() {
    String password = "a random password";

    DetachedEncryptionResult result =
        SecretBox.encryptDetached(
            new byte[0],
            password,
            PasswordHash.interactiveOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            PasswordHash.Algorithm.recommended());
    byte[] clearText =
        SecretBox.decryptDetached(
            result.cipherTextArray(),
            result.macArray(),
            password,
            PasswordHash.interactiveOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            PasswordHash.Algorithm.recommended());

    assertNotNull(clearText);
    assertEquals(0, clearText.length);
  }
}
