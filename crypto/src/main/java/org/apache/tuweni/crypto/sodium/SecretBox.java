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
import static java.util.Objects.requireNonNull;

import org.apache.tuweni.bytes.Bytes;

import java.util.Arrays;
import javax.security.auth.Destroyable;

import jnr.ffi.Pointer;
import org.jetbrains.annotations.Nullable;

// Documentation copied under the ISC License, from
// https://github.com/jedisct1/libsodium-doc/blob/424b7480562c2e063bc8c52c452ef891621c8480/secret-key_cryptography/authenticated_encryption.md

/**
 * Secret-key authenticated encryption.
 *
 * <p>
 * Encrypts a message with a key and a nonce to keep it confidential, and computes an authentication tag. The tag is
 * used to make sure that the message hasn't been tampered with before decrypting it.
 *
 * <p>
 * A single key is used both to encrypt/sign and verify/decrypt messages. For this reason, it is critical to keep the
 * key confidential.
 *
 * <p>
 * The nonce doesn't have to be confidential, but it should never ever be reused with the same key. The easiest way to
 * generate a nonce is to use randombytes_buf().
 *
 * <p>
 * Messages encrypted are assumed to be independent. If multiple messages are sent using this API and random nonces,
 * there will be no way to detect if a message has been received twice, or if messages have been reordered.
 *
 * <p>
 * This class depends upon the JNR-FFI library being available on the classpath, along with its dependencies. See
 * https://github.com/jnr/jnr-ffi. JNR-FFI can be included using the gradle dependency 'com.github.jnr:jnr-ffi'.
 */
public final class SecretBox {
  private SecretBox() {}

  /**
   * A SecretBox key.
   */
  public static final class Key implements Destroyable {
    final Allocated value;

    private Key(Pointer ptr, int length) {
      this.value = new Allocated(ptr, length);
    }

    private Key(Allocated value) {
      this.value = value;
    }

    public static Key fromHash(GenericHash.Hash hash) {
      return new Key(hash.value);
    }

    public static Key fromHash(SHA256Hash.Hash hash) {
      return new Key(hash.value);
    }

    @Override
    public void destroy() {
      value.destroy();
    }

    @Override
    public boolean isDestroyed() {
      return value.isDestroyed();
    }

    /**
     * Create a {@link Key} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the key.
     * @return A key, based on the supplied bytes.
     */
    public static Key fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link Key} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the key.
     * @return A key, based on the supplied bytes.
     */
    public static Key fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_secretbox_keybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_secretbox_keybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Key::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_secretbox_keybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_secretbox_keybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
    }

    /**
     * Generate a new key using a random generator.
     *
     * @return A randomly generated key.
     */
    public static Key random() {
      int length = length();
      Pointer ptr = Sodium.malloc(length);
      try {
        // When support for 10.0.11 is dropped, use this instead
        //Sodium.crypto_secretbox_keygen(ptr);
        Sodium.randombytes_buf(ptr, length);
        return new Key(ptr, length);
      } catch (Throwable e) {
        Sodium.sodium_free(ptr);
        throw e;
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Key)) {
        return false;
      }
      Key other = (Key) obj;
      return other.value.equals(value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    /**
     * Obtain the bytes of this key.
     *
     * WARNING: This will cause the key to be copied into heap memory.
     *
     * @return The bytes of this key.
     */
    public Bytes bytes() {
      return value.bytes();
    }

    /**
     * Obtain the bytes of this key.
     *
     * WARNING: This will cause the key to be copied into heap memory. The returned array should be overwritten when no
     * longer required.
     *
     * @return The bytes of this key.
     */
    public byte[] bytesArray() {
      return value.bytesArray();
    }
  }

  /**
   * A SecretBox nonce.
   */
  public static final class Nonce implements Destroyable {
    final Allocated value;

    private Nonce(Pointer ptr, int length) {
      this.value = new Allocated(ptr, length);
    }

    /**
     * Create a {@link Nonce} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the nonce.
     * @return A nonce, based on these bytes.
     */
    public static Nonce fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link Nonce} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the nonce.
     * @return A nonce, based on these bytes.
     */
    public static Nonce fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_secretbox_noncebytes()) {
        throw new IllegalArgumentException(
            "nonce must be " + Sodium.crypto_secretbox_noncebytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Nonce::new);
    }

    /**
     * Obtain the length of the nonce in bytes (24).
     *
     * @return The length of the nonce in bytes (24).
     */
    public static int length() {
      long noncebytes = Sodium.crypto_secretbox_noncebytes();
      if (noncebytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_secretbox_noncebytes: " + noncebytes + " is too large");
      }
      return (int) noncebytes;
    }

    /**
     * Create a zero {@link Nonce}.
     *
     * @return A zero nonce.
     */
    public static Nonce zero() {
      int length = length();
      Pointer ptr = Sodium.malloc(length);
      try {
        Sodium.sodium_memzero(ptr, length);
        return new Nonce(ptr, length);
      } catch (Throwable e) {
        Sodium.sodium_free(ptr);
        throw e;
      }
    }

    @Override
    public void destroy() {
      this.value.destroy();
    }

    /**
     * Generate a random {@link Nonce}.
     *
     * @return A randomly generated nonce.
     */
    public static Nonce random() {
      return Sodium.randomBytes(length(), Nonce::new);
    }

    /**
     * Increment this nonce.
     *
     * <p>
     * Note that this is not synchronized. If multiple threads are creating encrypted messages and incrementing this
     * nonce, then external synchronization is required to ensure no two encrypt operations use the same nonce.
     *
     * @return A new {@link Nonce}.
     */
    public Nonce increment() {
      return Sodium.dupAndIncrement(value.pointer(), length(), Nonce::new);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Nonce)) {
        return false;
      }
      Nonce other = (Nonce) obj;
      return other.value.equals(value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    /**
     * Provides the bytes of this nonce.
     * 
     * @return The bytes of this nonce.
     */
    public Bytes bytes() {
      return value.bytes();
    }

    /**
     * Provides the bytes of this nonce.
     * 
     * @return The bytes of this nonce.
     */
    public byte[] bytesArray() {
      return value.bytesArray();
    }
  }

  /**
   * Encrypt a message with a key.
   *
   * @param message The message to encrypt.
   * @param key The key to use for encryption.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public static Bytes encrypt(Bytes message, Key key, Nonce nonce) {
    return Bytes.wrap(encrypt(message.toArrayUnsafe(), key, nonce));
  }

  /**
   * Encrypt a message with a key.
   *
   * @param message The message to encrypt.
   * @param key The key to use for encryption.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public static Allocated encrypt(Allocated message, Key key, Nonce nonce) {
    int macbytes = macLength();
    Allocated cipherText = Allocated.allocate(macbytes + message.length());
    int rc = Sodium
        .crypto_secretbox_easy(
            cipherText.pointer(),
            message.pointer(),
            message.length(),
            nonce.value.pointer(),
            key.value.pointer());
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_easy: failed with result " + rc);
    }

    return cipherText;
  }

  /**
   * Encrypt a message with a key.
   *
   * @param message The message to encrypt.
   * @param key The key to use for encryption.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public static byte[] encrypt(byte[] message, Key key, Nonce nonce) {
    if (key.isDestroyed()) {
      throw new IllegalArgumentException("Key has been destroyed");
    }
    int macbytes = macLength();

    byte[] cipherText = new byte[macbytes + message.length];
    int rc =
        Sodium.crypto_secretbox_easy(cipherText, message, message.length, nonce.value.pointer(), key.value.pointer());
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_easy: failed with result " + rc);
    }

    return cipherText;
  }

  /**
   * Encrypt a message with a key, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param key The key to use for encryption.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(Bytes message, Key key, Nonce nonce) {
    return encryptDetached(message.toArrayUnsafe(), key, nonce);
  }

  /**
   * Encrypt a message with a key, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param key The key to use for encryption.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(byte[] message, Key key, Nonce nonce) {
    if (key.isDestroyed()) {
      throw new IllegalArgumentException("Key has been destroyed");
    }
    int macbytes = macLength();

    byte[] cipherText = new byte[message.length];
    byte[] mac = new byte[macbytes];
    int rc = Sodium
        .crypto_secretbox_detached(
            cipherText,
            mac,
            message,
            message.length,
            nonce.value.pointer(),
            key.value.pointer());
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_detached: failed with result " + rc);
    }

    return new DefaultDetachedEncryptionResult(cipherText, mac);
  }

  /**
   * Decrypt a message using a key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decrypt(Bytes cipherText, Key key, Nonce nonce) {
    byte[] bytes = decrypt(cipherText.toArrayUnsafe(), key, nonce);
    return (bytes != null) ? Bytes.wrap(bytes) : null;
  }

  /**
   * Decrypt a message using a key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Allocated decrypt(Allocated cipherText, Key key, Nonce nonce) {
    if (key.isDestroyed()) {
      throw new IllegalArgumentException("Key has been destroyed");
    }
    int macLength = macLength();
    if (macLength > cipherText.length()) {
      throw new IllegalArgumentException("cipherText is too short");
    }

    Allocated clearText = Allocated.allocate(cipherText.length() - macLength);
    int rc = Sodium
        .crypto_secretbox_open_easy(
            clearText.pointer(),
            cipherText.pointer(),
            cipherText.length(),
            nonce.value.pointer(),
            key.value.pointer());
    if (rc == -1) {
      return null;
    }
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_open_easy: failed with result " + rc);
    }
    return clearText;
  }

  /**
   * Decrypt a message using a key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decrypt(byte[] cipherText, Key key, Nonce nonce) {
    if (key.isDestroyed()) {
      throw new IllegalArgumentException("Key has been destroyed");
    }
    int macLength = macLength();
    if (macLength > cipherText.length) {
      throw new IllegalArgumentException("cipherText is too short");
    }

    byte[] clearText = new byte[cipherText.length - macLength];
    int rc = Sodium
        .crypto_secretbox_open_easy(
            clearText,
            cipherText,
            cipherText.length,
            nonce.value.pointer(),
            key.value.pointer());
    if (rc == -1) {
      return null;
    }
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_open_easy: failed with result " + rc);
    }
    return clearText;
  }

  /**
   * Decrypt a message using a key and a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decryptDetached(Bytes cipherText, Bytes mac, Key key, Nonce nonce) {
    byte[] bytes = decryptDetached(cipherText.toArrayUnsafe(), mac.toArrayUnsafe(), key, nonce);
    return (bytes != null) ? Bytes.wrap(bytes) : null;
  }

  /**
   * Decrypt a message using a key and a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decryptDetached(byte[] cipherText, byte[] mac, Key key, Nonce nonce) {
    if (key.isDestroyed()) {
      throw new IllegalArgumentException("Key has been destroyed");
    }
    int macLength = macLength();
    if (macLength != mac.length) {
      throw new IllegalArgumentException("mac must be " + macLength + " bytes, got " + mac.length);
    }

    byte[] clearText = new byte[cipherText.length];
    int rc = Sodium
        .crypto_secretbox_open_detached(
            clearText,
            cipherText,
            mac,
            cipherText.length,
            nonce.value.pointer(),
            key.value.pointer());
    if (rc == -1) {
      return null;
    }
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_open_detached: failed with result " + rc);
    }
    return clearText;
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation (with the currently
   * recommended algorithm and limits on operations and memory that are suitable for most use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @return The encrypted data.
   */
  public static Bytes encrypt(Bytes message, String password) {
    return encrypt(
        message,
        password,
        PasswordHash.moderateOpsLimit(),
        PasswordHash.moderateMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation (with the currently
   * recommended algorithm and limits on operations and memory that are suitable for most use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @return The encrypted data.
   */
  public static byte[] encrypt(byte[] message, String password) {
    return encrypt(
        message,
        password,
        PasswordHash.moderateOpsLimit(),
        PasswordHash.moderateMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation (with limits on operations and
   * memory that are suitable for most use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param algorithm The algorithm to use.
   * @return The encrypted data.
   */
  public static Bytes encrypt(Bytes message, String password, PasswordHash.Algorithm algorithm) {
    return encrypt(message, password, PasswordHash.moderateOpsLimit(), PasswordHash.moderateMemLimit(), algorithm);
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation (with limits on operations and
   * memory that are suitable for most use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param algorithm The algorithm to use.
   * @return The encrypted data.
   */
  public static byte[] encrypt(byte[] message, String password, PasswordHash.Algorithm algorithm) {
    return encrypt(message, password, PasswordHash.moderateOpsLimit(), PasswordHash.moderateMemLimit(), algorithm);
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation (with the currently
   * recommended algorithm and limits on operations and memory that are suitable for interactive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @return The encrypted data.
   */
  public static Bytes encryptInteractive(Bytes message, String password) {
    return encrypt(
        message,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation (with the currently
   * recommended algorithm and limits on operations and memory that are suitable for interactive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @return The encrypted data.
   */
  public static byte[] encryptInteractive(byte[] message, String password) {
    return encrypt(
        message,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation (with limits on operations and
   * memory that are suitable for interactive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param algorithm The algorithm to use.
   * @return The encrypted data.
   */
  public static Bytes encryptInteractive(Bytes message, String password, PasswordHash.Algorithm algorithm) {
    return encrypt(
        message,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        algorithm);
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation (with limits on operations and
   * memory that are suitable for interactive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param algorithm The algorithm to use.
   * @return The encrypted data.
   */
  public static byte[] encryptInteractive(byte[] message, String password, PasswordHash.Algorithm algorithm) {
    return encrypt(
        message,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        algorithm);
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation (with the currently
   * recommended algorithm and limits on operations and memory that are suitable for sensitive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @return The encrypted data.
   */
  public static Bytes encryptSensitive(Bytes message, String password) {
    return encrypt(
        message,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation (with the currently
   * recommended algorithm and limits on operations and memory that are suitable for sensitive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @return The encrypted data.
   */
  public static byte[] encryptSensitive(byte[] message, String password) {
    return encrypt(
        message,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation (with limits on operations and
   * memory that are suitable for sensitive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param algorithm The algorithm to use.
   * @return The encrypted data.
   */
  public static Bytes encryptSensitive(Bytes message, String password, PasswordHash.Algorithm algorithm) {
    return encrypt(message, password, PasswordHash.sensitiveOpsLimit(), PasswordHash.sensitiveMemLimit(), algorithm);
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation (with limits on operations and
   * memory that are suitable for sensitive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param algorithm The algorithm to use.
   * @return The encrypted data.
   */
  public static byte[] encryptSensitive(byte[] message, String password, PasswordHash.Algorithm algorithm) {
    return encrypt(message, password, PasswordHash.sensitiveOpsLimit(), PasswordHash.sensitiveMemLimit(), algorithm);
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation.
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param opsLimit The operations limit, which must be in the range {@link PasswordHash#minOpsLimit()} to
   *        {@link PasswordHash#maxOpsLimit()}.
   * @param memLimit The memory limit, which must be in the range {@link PasswordHash#minMemLimit()} to
   *        {@link PasswordHash#maxMemLimit()}.
   * @param algorithm The algorithm to use.
   * @return The encrypted data.
   */
  public static Bytes encrypt(
      Bytes message,
      String password,
      long opsLimit,
      long memLimit,
      PasswordHash.Algorithm algorithm) {
    return Bytes.wrap(encrypt(message.toArrayUnsafe(), password, opsLimit, memLimit, algorithm));
  }

  /**
   * Encrypt a message with a password, using {@link PasswordHash} for the key generation.
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param opsLimit The operations limit, which must be in the range {@link PasswordHash#minOpsLimit()} to
   *        {@link PasswordHash#maxOpsLimit()}.
   * @param memLimit The memory limit, which must be in the range {@link PasswordHash#minMemLimit()} to
   *        {@link PasswordHash#maxMemLimit()}.
   * @param algorithm The algorithm to use.
   * @return The encrypted data.
   * @throws UnsupportedOperationException If the specified algorithm is not supported by the currently loaded sodium
   *         native library.
   */
  public static byte[] encrypt(
      byte[] message,
      String password,
      long opsLimit,
      long memLimit,
      PasswordHash.Algorithm algorithm) {
    requireNonNull(message);
    requireNonNull(password);
    if (!algorithm.isSupported()) {
      throw new UnsupportedOperationException(
          algorithm.name() + " is not supported by the currently loaded sodium native library");
    }

    int macLength = macLength();

    byte[] cipherText = new byte[macLength + message.length];
    Nonce nonce = Nonce.random();
    Key key = deriveKeyFromPassword(password, nonce, opsLimit, memLimit, algorithm);
    assert !key.isDestroyed();

    int rc;
    try {
      rc = Sodium
          .crypto_secretbox_easy(cipherText, message, message.length, nonce.value.pointer(), key.value.pointer());
    } finally {
      key.destroy();
    }
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_easy: failed with result " + rc);
    }
    return prependNonce(nonce, cipherText);
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation (with the currently recommended algorithm and limits on operations and memory that are
   * suitable for most use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(Bytes message, String password) {
    return encryptDetached(
        message,
        password,
        PasswordHash.moderateOpsLimit(),
        PasswordHash.moderateMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation (with the currently recommended algorithm and limits on operations and memory that are
   * suitable for most use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(byte[] message, String password) {
    return encryptDetached(
        message,
        password,
        PasswordHash.moderateOpsLimit(),
        PasswordHash.moderateMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation (with limits on operations and memory that are suitable for most use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param algorithm The algorithm to use.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(
      Bytes message,
      String password,
      PasswordHash.Algorithm algorithm) {
    return encryptDetached(
        message,
        password,
        PasswordHash.moderateOpsLimit(),
        PasswordHash.moderateMemLimit(),
        algorithm);
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation (with limits on operations and memory that are suitable for most use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param algorithm The algorithm to use.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(
      byte[] message,
      String password,
      PasswordHash.Algorithm algorithm) {
    return encryptDetached(
        message,
        password,
        PasswordHash.moderateOpsLimit(),
        PasswordHash.moderateMemLimit(),
        algorithm);
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation (with the currently recommended algorithm and limits on operations and memory that are
   * suitable for interactive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptInteractiveDetached(Bytes message, String password) {
    return encryptDetached(
        message,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation (with the currently recommended algorithm and limits on operations and memory that are
   * suitable for interactive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptInteractiveDetached(byte[] message, String password) {
    return encryptDetached(
        message,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation (with limits on operations and memory that are suitable for interactive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param algorithm The algorithm to use.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptInteractiveDetached(
      Bytes message,
      String password,
      PasswordHash.Algorithm algorithm) {
    return encryptDetached(
        message,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        algorithm);
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation (with limits on operations and memory that are suitable for interactive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param algorithm The algorithm to use.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptInteractiveDetached(
      byte[] message,
      String password,
      PasswordHash.Algorithm algorithm) {
    return encryptDetached(
        message,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        algorithm);
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation (with the currently recommended algorithm and limits on operations and memory that are
   * suitable for sensitive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptSensitiveDetached(Bytes message, String password) {
    return encryptDetached(
        message,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation (with the currently recommended algorithm and limits on operations and memory that are
   * suitable for sensitive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptSensitiveDetached(byte[] message, String password) {
    return encryptDetached(
        message,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation (with limits on operations and memory that are suitable for sensitive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param algorithm The algorithm to use.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptSensitiveDetached(
      Bytes message,
      String password,
      PasswordHash.Algorithm algorithm) {
    return encryptDetached(
        message,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        algorithm);
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation (with limits on operations and memory that are suitable for sensitive use-cases).
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param algorithm The algorithm to use.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptSensitiveDetached(
      byte[] message,
      String password,
      PasswordHash.Algorithm algorithm) {
    return encryptDetached(
        message,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        algorithm);
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param opsLimit The operations limit, which must be in the range {@link PasswordHash#minOpsLimit()} to
   *        {@link PasswordHash#maxOpsLimit()}.
   * @param memLimit The memory limit, which must be in the range {@link PasswordHash#minMemLimit()} to
   *        {@link PasswordHash#maxMemLimit()}.
   * @param algorithm The algorithm to use.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(
      Bytes message,
      String password,
      long opsLimit,
      long memLimit,
      PasswordHash.Algorithm algorithm) {
    return encryptDetached(message.toArrayUnsafe(), password, opsLimit, memLimit, algorithm);
  }

  /**
   * Encrypt a message with a password, generating a detached message authentication code, using {@link PasswordHash}
   * for the key generation.
   *
   * @param message The message to encrypt.
   * @param password The password to use for encryption.
   * @param opsLimit The operations limit, which must be in the range {@link PasswordHash#minOpsLimit()} to
   *        {@link PasswordHash#maxOpsLimit()}.
   * @param memLimit The memory limit, which must be in the range {@link PasswordHash#minMemLimit()} to
   *        {@link PasswordHash#maxMemLimit()}.
   * @param algorithm The algorithm to use.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(
      byte[] message,
      String password,
      long opsLimit,
      long memLimit,
      PasswordHash.Algorithm algorithm) {
    requireNonNull(message);
    requireNonNull(password);
    if (!algorithm.isSupported()) {
      throw new UnsupportedOperationException(
          algorithm.name() + " is not supported by the currently loaded sodium native library");
    }
    int macLength = macLength();

    byte[] cipherText = new byte[message.length];
    byte[] mac = new byte[macLength];
    Nonce nonce = Nonce.random();
    Key key = deriveKeyFromPassword(password, nonce, opsLimit, memLimit, algorithm);
    assert !key.isDestroyed();

    int rc;
    try {
      rc = Sodium
          .crypto_secretbox_detached(
              cipherText,
              mac,
              message,
              message.length,
              nonce.value.pointer(),
              key.value.pointer());
    } finally {
      key.destroy();
    }
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_detached: failed with result " + rc);
    }
    return new DefaultDetachedEncryptionResult(cipherText, prependNonce(nonce, mac));
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation (with the currently
   * recommended algorithm and limits on operations and memory that are suitable for most use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decrypt(Bytes cipherText, String password) {
    return decrypt(
        cipherText,
        password,
        PasswordHash.moderateOpsLimit(),
        PasswordHash.moderateMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation (with the currently
   * recommended algorithm and limits on operations and memory that are suitable for most use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decrypt(byte[] cipherText, String password) {
    return decrypt(
        cipherText,
        password,
        PasswordHash.moderateOpsLimit(),
        PasswordHash.moderateMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation (with limits on operations
   * and memory that are suitable for most use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decrypt(Bytes cipherText, String password, PasswordHash.Algorithm algorithm) {
    return decrypt(cipherText, password, PasswordHash.moderateOpsLimit(), PasswordHash.moderateMemLimit(), algorithm);
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation (with limits on operations
   * and memory that are suitable for most use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decrypt(byte[] cipherText, String password, PasswordHash.Algorithm algorithm) {
    return decrypt(cipherText, password, PasswordHash.moderateOpsLimit(), PasswordHash.moderateMemLimit(), algorithm);
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation (with the currently
   * recommended algorithm and limits on operations and memory that are suitable for interactive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decryptInteractive(Bytes cipherText, String password) {
    return decrypt(
        cipherText,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation (with the currently
   * recommended algorithm and limits on operations and memory that are suitable for interactive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decryptInteractive(byte[] cipherText, String password) {
    return decrypt(
        cipherText,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation (with limits on operations
   * and memory that are suitable for interactive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decryptInteractive(Bytes cipherText, String password, PasswordHash.Algorithm algorithm) {
    return decrypt(
        cipherText,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        algorithm);
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation (with limits on operations
   * and memory that are suitable for interactive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decryptInteractive(byte[] cipherText, String password, PasswordHash.Algorithm algorithm) {
    return decrypt(
        cipherText,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        algorithm);
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation (with the currently
   * recommended algorithm and limits on operations and memory that are suitable for sensitive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decryptSensitive(Bytes cipherText, String password) {
    return decrypt(
        cipherText,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation (with the currently
   * recommended algorithm and limits on operations and memory that are suitable for sensitive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decryptSensitive(byte[] cipherText, String password) {
    return decrypt(
        cipherText,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation (with limits on operations
   * and memory that are suitable for sensitive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decryptSensitive(Bytes cipherText, String password, PasswordHash.Algorithm algorithm) {
    return decrypt(cipherText, password, PasswordHash.sensitiveOpsLimit(), PasswordHash.sensitiveMemLimit(), algorithm);
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation (with limits on operations
   * and memory that are suitable for sensitive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decryptSensitive(byte[] cipherText, String password, PasswordHash.Algorithm algorithm) {
    return decrypt(cipherText, password, PasswordHash.sensitiveOpsLimit(), PasswordHash.sensitiveMemLimit(), algorithm);
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation.
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @param opsLimit The opsLimit that was used for encryption.
   * @param memLimit The memLimit that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decrypt(
      Bytes cipherText,
      String password,
      long opsLimit,
      long memLimit,
      PasswordHash.Algorithm algorithm) {
    byte[] bytes = decrypt(cipherText.toArrayUnsafe(), password, opsLimit, memLimit, algorithm);
    return (bytes != null) ? Bytes.wrap(bytes) : null;
  }

  /**
   * Decrypt a message using a password, using {@link PasswordHash} for the key generation.
   *
   * @param cipherText The cipher text to decrypt.
   * @param password The password that was used for encryption.
   * @param opsLimit The opsLimit that was used for encryption.
   * @param memLimit The memLimit that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   * @throws UnsupportedOperationException If the specified algorithm is not supported by the currently loaded sodium
   *         native library.
   */
  @Nullable
  public static byte[] decrypt(
      byte[] cipherText,
      String password,
      long opsLimit,
      long memLimit,
      PasswordHash.Algorithm algorithm) {
    requireNonNull(cipherText);
    requireNonNull(password);
    if (!algorithm.isSupported()) {
      throw new UnsupportedOperationException(
          algorithm.name() + " is not supported by the currently loaded sodium native library");
    }

    int noncebytes = Nonce.length();
    int macLength = macLength();
    if ((noncebytes + macLength) > cipherText.length) {
      throw new IllegalArgumentException("cipherText is too short");
    }

    byte[] clearText = new byte[cipherText.length - noncebytes - macLength];
    Nonce nonce = Nonce.fromBytes(Arrays.copyOf(cipherText, noncebytes));
    Key key = deriveKeyFromPassword(password, nonce, opsLimit, memLimit, algorithm);
    assert !key.isDestroyed();

    int rc;
    try {
      rc = Sodium
          .crypto_secretbox_open_easy(
              clearText,
              Arrays.copyOfRange(cipherText, noncebytes, cipherText.length),
              cipherText.length - noncebytes,
              nonce.value.pointer(),
              key.value.pointer());
    } finally {
      key.destroy();
    }
    if (rc == -1) {
      return null;
    }
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_open_easy: failed with result " + rc);
    }
    return clearText;
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation (with the currently recommended algorithm and limits on operations and memory that are suitable for
   * most use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decryptDetached(Bytes cipherText, Bytes mac, String password) {
    return decryptDetached(
        cipherText,
        mac,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation (with the currently recommended algorithm and limits on operations and memory that are suitable for
   * most use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decryptDetached(byte[] cipherText, byte[] mac, String password) {
    return decryptDetached(
        cipherText,
        mac,
        password,
        PasswordHash.moderateOpsLimit(),
        PasswordHash.moderateMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation (with limits on operations and memory that are suitable for most use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decryptDetached(Bytes cipherText, Bytes mac, String password, PasswordHash.Algorithm algorithm) {
    return decryptDetached(
        cipherText,
        mac,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        algorithm);
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation (with limits on operations and memory that are suitable for most use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decryptDetached(
      byte[] cipherText,
      byte[] mac,
      String password,
      PasswordHash.Algorithm algorithm) {
    return decryptDetached(
        cipherText,
        mac,
        password,
        PasswordHash.moderateOpsLimit(),
        PasswordHash.moderateMemLimit(),
        algorithm);
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation (with the currently recommended algorithm and limits on operations and memory that are suitable for
   * interactive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decryptInteractiveDetached(Bytes cipherText, Bytes mac, String password) {
    return decryptDetached(
        cipherText,
        mac,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation (with the currently recommended algorithm and limits on operations and memory that are suitable for
   * interactive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decryptInteractiveDetached(byte[] cipherText, byte[] mac, String password) {
    return decryptDetached(
        cipherText,
        mac,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation (with limits on operations and memory that are suitable for interactive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decryptInteractiveDetached(
      Bytes cipherText,
      Bytes mac,
      String password,
      PasswordHash.Algorithm algorithm) {
    return decryptDetached(
        cipherText,
        mac,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        algorithm);
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation (with limits on operations and memory that are suitable for interactive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decryptInteractiveDetached(
      byte[] cipherText,
      byte[] mac,
      String password,
      PasswordHash.Algorithm algorithm) {
    return decryptDetached(
        cipherText,
        mac,
        password,
        PasswordHash.interactiveOpsLimit(),
        PasswordHash.interactiveMemLimit(),
        algorithm);
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation (with the currently recommended algorithm and limits on operations and memory that are suitable for
   * sensitive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decryptSensitiveDetached(Bytes cipherText, Bytes mac, String password) {
    return decryptDetached(
        cipherText,
        mac,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation (with the currently recommended algorithm and limits on operations and memory that are suitable for
   * sensitive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decryptSensitiveDetached(byte[] cipherText, byte[] mac, String password) {
    return decryptDetached(
        cipherText,
        mac,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        PasswordHash.Algorithm.recommended());
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation (with limits on operations and memory that are suitable for sensitive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decryptSensitiveDetached(
      Bytes cipherText,
      Bytes mac,
      String password,
      PasswordHash.Algorithm algorithm) {
    return decryptDetached(
        cipherText,
        mac,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        algorithm);
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation (with limits on operations and memory that are suitable for sensitive use-cases).
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decryptSensitiveDetached(
      byte[] cipherText,
      byte[] mac,
      String password,
      PasswordHash.Algorithm algorithm) {
    return decryptDetached(
        cipherText,
        mac,
        password,
        PasswordHash.sensitiveOpsLimit(),
        PasswordHash.sensitiveMemLimit(),
        algorithm);
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @param opsLimit The opsLimit that was used for encryption.
   * @param memLimit The memLimit that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decryptDetached(
      Bytes cipherText,
      Bytes mac,
      String password,
      long opsLimit,
      long memLimit,
      PasswordHash.Algorithm algorithm) {
    byte[] bytes =
        decryptDetached(cipherText.toArrayUnsafe(), mac.toArrayUnsafe(), password, opsLimit, memLimit, algorithm);
    return (bytes != null) ? Bytes.wrap(bytes) : null;
  }

  /**
   * Decrypt a message using a password and a detached message authentication code, using {@link PasswordHash} for the
   * key generation.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param password The password that was used for encryption.
   * @param opsLimit The opsLimit that was used for encryption.
   * @param memLimit The memLimit that was used for encryption.
   * @param algorithm The algorithm that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decryptDetached(
      byte[] cipherText,
      byte[] mac,
      String password,
      long opsLimit,
      long memLimit,
      PasswordHash.Algorithm algorithm) {
    requireNonNull(cipherText);
    requireNonNull(mac);
    requireNonNull(password);
    if (!algorithm.isSupported()) {
      throw new UnsupportedOperationException(
          algorithm.name() + " is not supported by the currently loaded sodium native library");
    }

    int noncebytes = Nonce.length();
    int macLength = macLength();
    if ((noncebytes + macLength) != mac.length) {
      throw new IllegalArgumentException("mac must be " + (noncebytes + macLength) + " bytes, got " + mac.length);
    }

    byte[] clearText = new byte[cipherText.length];
    Nonce nonce = Nonce.fromBytes(Arrays.copyOf(mac, noncebytes));
    Key key = deriveKeyFromPassword(password, nonce, opsLimit, memLimit, algorithm);
    assert !key.isDestroyed();

    int rc;
    try {
      rc = Sodium
          .crypto_secretbox_open_detached(
              clearText,
              cipherText,
              Arrays.copyOfRange(mac, noncebytes, mac.length),
              cipherText.length,
              nonce.value.pointer(),
              key.value.pointer());
    } finally {
      key.destroy();
    }
    if (rc == -1) {
      return null;
    }
    if (rc != 0) {
      throw new SodiumException("crypto_secretbox_open_detached: failed with result " + rc);
    }

    return clearText;
  }

  private static int macLength() {
    long macbytes = Sodium.crypto_secretbox_macbytes();
    if (macbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_secretbox_macbytes: " + macbytes + " is too large");
    }
    return (int) macbytes;
  }

  private static Key deriveKeyFromPassword(
      String password,
      Nonce nonce,
      long opsLimit,
      long memLimit,
      PasswordHash.Algorithm algorithm) {
    assert Nonce.length() >= PasswordHash.Salt.length()
        : "SecretBox.Nonce has insufficient length for deriving a PasswordHash.Salt ("
            + Nonce.length()
            + " < "
            + PasswordHash.Salt.length()
            + ")";
    PasswordHash.Salt salt =
        PasswordHash.Salt.fromBytes(Arrays.copyOfRange(nonce.bytesArray(), 0, PasswordHash.Salt.length()));
    byte[] passwordBytes = password.getBytes(UTF_8);
    try {
      byte[] keyBytes = PasswordHash.hash(passwordBytes, Key.length(), salt, opsLimit, memLimit, algorithm);
      try {
        return Key.fromBytes(keyBytes);
      } finally {
        Arrays.fill(keyBytes, (byte) 0);
      }
    } finally {
      Arrays.fill(passwordBytes, (byte) 0);
    }
  }

  private static byte[] prependNonce(Nonce nonce, byte[] bytes) {
    int nonceLength = Nonce.length();
    byte[] data = new byte[nonceLength + bytes.length];
    nonce.value.pointer().get(0, data, 0, nonceLength);
    System.arraycopy(bytes, 0, data, nonceLength, bytes.length);
    return data;
  }
}
