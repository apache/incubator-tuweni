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

import static java.util.Objects.requireNonNull;

import org.apache.tuweni.bytes.Bytes;

import java.util.Arrays;
import javax.security.auth.Destroyable;

import jnr.ffi.Pointer;
import jnr.ffi.byref.LongLongByReference;
import org.jetbrains.annotations.Nullable;

// Documentation copied under the ISC License, from
// https://github.com/jedisct1/libsodium-doc/blob/424b7480562c2e063bc8c52c452ef891621c8480/secret-key_cryptography/aes-256-gcm.md

/**
 * Authenticated Encryption with Additional Data using AES-GCM.
 *
 * <p>
 * WARNING: Despite being the most popular AEAD construction due to its use in TLS, safely using AES-GCM in a different
 * context is tricky.
 *
 * <p>
 * No more than ~350 GB of input data should be encrypted with a given key. This is for ~16 KB messages -- Actual
 * figures vary according to message sizes.
 *
 * <p>
 * In addition, nonces are short and repeated nonces would totally destroy the security of this scheme. Nonces should
 * thus come from atomic counters, which can be difficult to set up in a distributed environment.
 *
 * <p>
 * Unless you absolutely need AES-GCM, use {@link XChaCha20Poly1305} instead. It doesn't have any of these limitations.
 * Or, if you don't need to authenticate additional data, just stick to
 * {@link Sodium#crypto_box(byte[], byte[], long, byte[], byte[], byte[])}.
 *
 * <p>
 * This class depends upon the JNR-FFI library being available on the classpath, along with its dependencies. See
 * https://github.com/jnr/jnr-ffi. JNR-FFI can be included using the gradle dependency 'com.github.jnr:jnr-ffi'.
 */
public final class AES256GCM implements AutoCloseable {

  private static final byte[] EMPTY_BYTES = new byte[0];

  /**
   * Check if Sodium and the AES256-GCM algorithm is available.
   *
   * @return {@code true} if Sodium and the AES256-GCM algorithm is available.
   */
  public static boolean isAvailable() {
    try {
      return Sodium.crypto_aead_aes256gcm_is_available() != 0;
    } catch (LinkageError e) {
      return false;
    }
  }

  private static void assertAvailable() {
    if (!isAvailable()) {
      throw new UnsupportedOperationException("Sodium AES256-GCM is not available");
    }
  }

  /**
   * An AES256-GSM key.
   */
  public static final class Key implements Destroyable {
    final Allocated value;

    private Key(Pointer ptr, int length) {
      this.value = new Allocated(ptr, length);
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
     * @throws UnsupportedOperationException If AES256-GSM support is not available.
     */
    public static Key fromBytes(byte[] bytes) {
      assertAvailable();
      if (bytes.length != Sodium.crypto_aead_aes256gcm_keybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_aead_aes256gcm_keybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Key::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     * @throws UnsupportedOperationException If AES256-GSM support is not available.
     */
    public static int length() {
      assertAvailable();
      long keybytes = Sodium.crypto_aead_aes256gcm_keybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_aead_aes256gcm_keybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
    }

    /**
     * Generate a new key using a random generator.
     *
     * @return A randomly generated key.
     * @throws UnsupportedOperationException If AES256-GSM support is not available.
     */
    public static Key random() {
      assertAvailable();
      int length = length();
      Pointer ptr = Sodium.malloc(length);
      try {
        // When support for 10.0.11 is dropped, use this instead
        //Sodium.crypto_aead_aes256gcm_keygen(ptr);
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
   * An AES256-GSM nonce.
   */
  public static final class Nonce {
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
     * @throws UnsupportedOperationException If AES256-GSM support is not available.
     */
    public static Nonce fromBytes(byte[] bytes) {
      assertAvailable();
      if (bytes.length != Sodium.crypto_aead_aes256gcm_npubbytes()) {
        throw new IllegalArgumentException(
            "nonce must be " + Sodium.crypto_aead_aes256gcm_npubbytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Nonce::new);
    }

    /**
     * Obtain the length of the nonce in bytes (12).
     *
     * @return The length of the nonce in bytes (12).
     * @throws UnsupportedOperationException If AES256-GSM support is not available.
     */
    public static int length() {
      assertAvailable();
      long npubbytes = Sodium.crypto_aead_aes256gcm_npubbytes();
      if (npubbytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_aead_aes256gcm_npubbytes: " + npubbytes + " is too large");
      }
      return (int) npubbytes;
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
      return Sodium.dupAndIncrement(value.pointer(), value.length(), Nonce::new);
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

  private Pointer ctx;

  private AES256GCM(Key key) {
    if (key.isDestroyed()) {
      throw new IllegalArgumentException("Key has been destroyed");
    }
    ctx = Sodium.malloc(Sodium.crypto_aead_aes256gcm_statebytes());
    try {
      int rc = Sodium.crypto_aead_aes256gcm_beforenm(ctx, key.value.pointer());
      if (rc != 0) {
        throw new SodiumException("crypto_aead_aes256gcm_beforenm: failed with result " + rc);
      }
    } catch (Throwable e) {
      Sodium.sodium_free(ctx);
      ctx = null;
      throw e;
    }
  }

  /**
   * Pre-compute the expansion for the key.
   *
   * <p>
   * Note that the returned instance of {@link AES256GCM} should be closed using {@link #close()} (or
   * try-with-resources) to ensure timely release of the expanded key, which is held in native memory.
   *
   * @param key The key to precompute an expansion for.
   * @return A {@link AES256GCM} instance.
   * @throws UnsupportedOperationException If AES256-GSM support is not available.
   */
  public static AES256GCM forKey(Key key) {
    requireNonNull(key);
    assertAvailable();
    return new AES256GCM(key);
  }

  /**
   * Encrypt a message for a given key.
   *
   * @param message The message to encrypt.
   * @param key The key to encrypt for.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public static Bytes encrypt(Bytes message, Key key, Nonce nonce) {
    return Bytes.wrap(encrypt(message.toArrayUnsafe(), key, nonce));
  }

  /**
   * Encrypt a message for a given key.
   *
   * @param message The message to encrypt.
   * @param key The key to encrypt for.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public static byte[] encrypt(byte[] message, Key key, Nonce nonce) {
    return encrypt(message, EMPTY_BYTES, key, nonce);
  }

  /**
   * Encrypt a message for a given key.
   *
   * @param message The message to encrypt.
   * @param data Extra non-confidential data that will be included with the encrypted payload.
   * @param key The key to encrypt for.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public static Bytes encrypt(Bytes message, Bytes data, Key key, Nonce nonce) {
    return Bytes.wrap(encrypt(message.toArrayUnsafe(), data.toArrayUnsafe(), key, nonce));
  }

  /**
   * Encrypt a message for a given key.
   *
   * @param message The message to encrypt.
   * @param data Extra non-confidential data that will be included with the encrypted payload.
   * @param key The key to encrypt for.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   * @throws UnsupportedOperationException If AES256-GSM support is not available.
   */
  public static byte[] encrypt(byte[] message, byte[] data, Key key, Nonce nonce) {
    assertAvailable();
    if (key.isDestroyed()) {
      throw new IllegalArgumentException("Key has been destroyed");
    }

    byte[] cipherText = new byte[maxCombinedCypherTextLength(message)];

    LongLongByReference cipherTextLen = new LongLongByReference();
    int rc = Sodium
        .crypto_aead_aes256gcm_encrypt(
            cipherText,
            cipherTextLen,
            message,
            message.length,
            data,
            data.length,
            null,
            nonce.value.pointer(),
            key.value.pointer());
    if (rc != 0) {
      throw new SodiumException("crypto_aead_aes256gcm_encrypt: failed with result " + rc);
    }

    return maybeSliceResult(cipherText, cipherTextLen, "crypto_aead_aes256gcm_encrypt");
  }

  /**
   * Encrypt a message.
   *
   * @param message The message to encrypt.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public Bytes encrypt(Bytes message, Nonce nonce) {
    return Bytes.wrap(encrypt(message.toArrayUnsafe(), nonce));
  }

  /**
   * Encrypt a message.
   *
   * @param message The message to encrypt.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public byte[] encrypt(byte[] message, Nonce nonce) {
    return encrypt(message, EMPTY_BYTES, nonce);
  }

  /**
   * Encrypt a message.
   *
   * @param message The message to encrypt.
   * @param data Extra non-confidential data that will be included with the encrypted payload.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public Bytes encrypt(Bytes message, Bytes data, Nonce nonce) {
    return Bytes.wrap(encrypt(message.toArrayUnsafe(), data.toArrayUnsafe(), nonce));
  }

  /**
   * Encrypt a message.
   *
   * @param message The message to encrypt.
   * @param data Extra non-confidential data that will be included with the encrypted payload.
   * @param nonce A unique nonce.
   * @return The encrypted data.
   */
  public byte[] encrypt(byte[] message, byte[] data, Nonce nonce) {
    assertOpen();

    byte[] cipherText = new byte[maxCombinedCypherTextLength(message)];

    LongLongByReference cipherTextLen = new LongLongByReference();
    int rc = Sodium
        .crypto_aead_aes256gcm_encrypt_afternm(
            cipherText,
            cipherTextLen,
            message,
            message.length,
            data,
            data.length,
            null,
            nonce.value.pointer(),
            ctx);
    if (rc != 0) {
      throw new SodiumException("crypto_aead_aes256gcm_encrypt_afternm: failed with result " + rc);
    }

    return maybeSliceResult(cipherText, cipherTextLen, "crypto_aead_aes256gcm_encrypt_afternm");
  }

  private static int maxCombinedCypherTextLength(byte[] message) {
    long abytes = Sodium.crypto_aead_aes256gcm_abytes();
    if (abytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_aead_aes256gcm_abytes: " + abytes + " is too large");
    }
    return (int) abytes + message.length;
  }

  /**
   * Encrypt a message for a given key, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param key The key to encrypt for.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(Bytes message, Key key, Nonce nonce) {
    return encryptDetached(message.toArrayUnsafe(), key, nonce);
  }

  /**
   * Encrypt a message for a given key, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param key The key to encrypt for.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(byte[] message, Key key, Nonce nonce) {
    return encryptDetached(message, EMPTY_BYTES, key, nonce);
  }

  /**
   * Encrypt a message for a given key, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param data Extra non-confidential data that will be included with the encrypted payload.
   * @param key The key to encrypt for.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public static DetachedEncryptionResult encryptDetached(Bytes message, Bytes data, Key key, Nonce nonce) {
    return encryptDetached(message.toArrayUnsafe(), data.toArrayUnsafe(), key, nonce);
  }

  /**
   * Encrypt a message for a given key, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param data Extra non-confidential data that will be included with the encrypted payload.
   * @param key The key to encrypt for.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   * @throws UnsupportedOperationException If AES256-GSM support is not available.
   */
  public static DetachedEncryptionResult encryptDetached(byte[] message, byte[] data, Key key, Nonce nonce) {
    assertAvailable();
    if (key.isDestroyed()) {
      throw new IllegalArgumentException("Key has been destroyed");
    }

    byte[] cipherText = new byte[message.length];
    long abytes = Sodium.crypto_aead_aes256gcm_abytes();
    if (abytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_aead_aes256gcm_abytes: " + abytes + " is too large");
    }
    byte[] mac = new byte[(int) abytes];

    LongLongByReference macLen = new LongLongByReference();
    int rc = Sodium
        .crypto_aead_aes256gcm_encrypt_detached(
            cipherText,
            mac,
            macLen,
            message,
            message.length,
            data,
            data.length,
            null,
            nonce.value.pointer(),
            key.value.pointer());
    if (rc != 0) {
      throw new SodiumException("crypto_aead_aes256gcm_encrypt_detached: failed with result " + rc);
    }

    return new DefaultDetachedEncryptionResult(
        cipherText,
        maybeSliceResult(mac, macLen, "crypto_aead_aes256gcm_encrypt_detached"));
  }

  /**
   * Encrypt a message, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public DetachedEncryptionResult encryptDetached(Bytes message, Nonce nonce) {
    return encryptDetached(message.toArrayUnsafe(), nonce);
  }

  /**
   * Encrypt a message, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public DetachedEncryptionResult encryptDetached(byte[] message, Nonce nonce) {
    return encryptDetached(message, EMPTY_BYTES, nonce);
  }

  /**
   * Encrypt a message, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param data Extra non-confidential data that will be included with the encrypted payload.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public DetachedEncryptionResult encryptDetached(Bytes message, Bytes data, Nonce nonce) {
    return encryptDetached(message.toArrayUnsafe(), data.toArrayUnsafe(), nonce);
  }

  /**
   * Encrypt a message, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param data Extra non-confidential data that will be included with the encrypted payload.
   * @param nonce A unique nonce.
   * @return The encrypted data and message authentication code.
   */
  public DetachedEncryptionResult encryptDetached(byte[] message, byte[] data, Nonce nonce) {
    assertOpen();

    byte[] cipherText = new byte[message.length];
    long abytes = Sodium.crypto_aead_aes256gcm_abytes();
    if (abytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_aead_aes256gcm_abytes: " + abytes + " is too large");
    }
    byte[] mac = new byte[(int) abytes];

    LongLongByReference macLen = new LongLongByReference();
    int rc = Sodium
        .crypto_aead_aes256gcm_encrypt_detached_afternm(
            cipherText,
            mac,
            macLen,
            message,
            message.length,
            data,
            data.length,
            null,
            nonce.value.pointer(),
            ctx);
    if (rc != 0) {
      throw new SodiumException("crypto_aead_aes256gcm_encrypt_detached_afternm: failed with result " + rc);
    }

    return new DefaultDetachedEncryptionResult(
        cipherText,
        maybeSliceResult(mac, macLen, "crypto_aead_aes256gcm_encrypt_detached_afternm"));
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param key The key to use for decryption.
   * @param nonce The nonce to use when decrypting.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decrypt(Bytes cipherText, Key key, Nonce nonce) {
    byte[] bytes = decrypt(cipherText.toArrayUnsafe(), key, nonce);
    return (bytes != null) ? Bytes.wrap(bytes) : null;
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param key The key to use for decryption.
   * @param nonce The nonce to use when decrypting.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decrypt(byte[] cipherText, Key key, Nonce nonce) {
    return decrypt(cipherText, EMPTY_BYTES, key, nonce);
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param data Extra non-confidential data that is included within the encrypted payload.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decrypt(Bytes cipherText, Bytes data, Key key, Nonce nonce) {
    byte[] bytes = decrypt(cipherText.toArrayUnsafe(), data.toArrayUnsafe(), key, nonce);
    return (bytes != null) ? Bytes.wrap(bytes) : null;
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param data Extra non-confidential data that is included within the encrypted payload.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   * @throws UnsupportedOperationException If AES256-GSM support is not available.
   */
  @Nullable
  public static byte[] decrypt(byte[] cipherText, byte[] data, Key key, Nonce nonce) {
    assertAvailable();
    if (key.isDestroyed()) {
      throw new IllegalArgumentException("Key has been destroyed");
    }

    byte[] clearText = new byte[maxClearTextLength(cipherText)];

    LongLongByReference clearTextLen = new LongLongByReference();
    int rc = Sodium
        .crypto_aead_aes256gcm_decrypt(
            clearText,
            clearTextLen,
            null,
            cipherText,
            cipherText.length,
            data,
            data.length,
            nonce.value.pointer(),
            key.value.pointer());
    if (rc == -1) {
      return null;
    }
    if (rc != 0) {
      throw new SodiumException("crypto_aead_aes256gcm_encrypt: failed with result " + rc);
    }

    return maybeSliceResult(clearText, clearTextLen, "crypto_aead_aes256gcm_decrypt");
  }

  /**
   * Decrypt a message.
   *
   * @param cipherText The cipher text to decrypt.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public Bytes decrypt(Bytes cipherText, Nonce nonce) {
    byte[] bytes = decrypt(cipherText.toArrayUnsafe(), nonce);
    return (bytes != null) ? Bytes.wrap(bytes) : null;
  }

  /**
   * Decrypt a message.
   *
   * @param cipherText The cipher text to decrypt.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public byte[] decrypt(byte[] cipherText, Nonce nonce) {
    return decrypt(cipherText, EMPTY_BYTES, nonce);
  }

  /**
   * Decrypt a message.
   *
   * @param cipherText The cipher text to decrypt.
   * @param data Extra non-confidential data that is included within the encrypted payload.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public Bytes decrypt(Bytes cipherText, Bytes data, Nonce nonce) {
    byte[] bytes = decrypt(cipherText.toArrayUnsafe(), data.toArrayUnsafe(), nonce);
    return (bytes != null) ? Bytes.wrap(bytes) : null;
  }

  /**
   * Decrypt a message.
   *
   * @param cipherText The cipher text to decrypt.
   * @param data Extra non-confidential data that is included within the encrypted payload.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public byte[] decrypt(byte[] cipherText, byte[] data, Nonce nonce) {
    assertOpen();

    byte[] clearText = new byte[maxClearTextLength(cipherText)];

    LongLongByReference clearTextLen = new LongLongByReference();
    int rc = Sodium
        .crypto_aead_aes256gcm_decrypt_afternm(
            clearText,
            clearTextLen,
            null,
            cipherText,
            cipherText.length,
            data,
            data.length,
            nonce.value.pointer(),
            ctx);
    if (rc == -1) {
      return null;
    }
    if (rc != 0) {
      throw new SodiumException("crypto_aead_aes256gcm_decrypt_afternm: failed with result " + rc);
    }

    return maybeSliceResult(clearText, clearTextLen, "crypto_aead_aes256gcm_decrypt_afternm");
  }

  private static int maxClearTextLength(byte[] cipherText) {
    long abytes = Sodium.crypto_aead_aes256gcm_abytes();
    if (abytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_aead_aes256gcm_abytes: " + abytes + " is too large");
    }
    if (abytes > cipherText.length) {
      throw new IllegalArgumentException("cipherText is too short");
    }
    return cipherText.length - ((int) abytes);
  }

  /**
   * Decrypt a message using a given key and a detached message authentication code.
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
   * Decrypt a message using a given key and a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static byte[] decryptDetached(byte[] cipherText, byte[] mac, Key key, Nonce nonce) {
    return decryptDetached(cipherText, mac, EMPTY_BYTES, key, nonce);
  }

  /**
   * Decrypt a message using a given key and a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param data Extra non-confidential data that is included within the encrypted payload.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public static Bytes decryptDetached(Bytes cipherText, Bytes mac, Bytes data, Key key, Nonce nonce) {
    byte[] bytes = decryptDetached(cipherText.toArrayUnsafe(), mac.toArrayUnsafe(), data.toArrayUnsafe(), key, nonce);
    return (bytes != null) ? Bytes.wrap(bytes) : null;
  }

  /**
   * Decrypt a message using a given key and a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param data Extra non-confidential data that is included within the encrypted payload.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   * @throws UnsupportedOperationException If AES256-GSM support is not available.
   */
  @Nullable
  public static byte[] decryptDetached(byte[] cipherText, byte[] mac, byte[] data, Key key, Nonce nonce) {
    assertAvailable();
    if (key.isDestroyed()) {
      throw new IllegalArgumentException("Key has been destroyed");
    }

    long abytes = Sodium.crypto_aead_aes256gcm_abytes();
    if (abytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_aead_aes256gcm_abytes: " + abytes + " is too large");
    }
    if (mac.length != abytes) {
      throw new IllegalArgumentException("mac must be " + abytes + " bytes, got " + mac.length);
    }

    byte[] clearText = new byte[cipherText.length];
    int rc = Sodium
        .crypto_aead_aes256gcm_decrypt_detached(
            clearText,
            null,
            cipherText,
            cipherText.length,
            mac,
            data,
            data.length,
            nonce.value.pointer(),
            key.value.pointer());
    if (rc == -1) {
      return null;
    }
    if (rc != 0) {
      throw new SodiumException("crypto_aead_aes256gcm_encrypt: failed with result " + rc);
    }

    return clearText;
  }

  /**
   * Decrypt a message using a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public Bytes decryptDetached(Bytes cipherText, Bytes mac, Nonce nonce) {
    byte[] bytes = decryptDetached(cipherText.toArrayUnsafe(), mac.toArrayUnsafe(), nonce);
    return (bytes != null) ? Bytes.wrap(bytes) : null;
  }

  /**
   * Decrypt a message using a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public byte[] decryptDetached(byte[] cipherText, byte[] mac, Nonce nonce) {
    return decryptDetached(cipherText, mac, EMPTY_BYTES, nonce);
  }

  /**
   * Decrypt a message using a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param data Extra non-confidential data that is included within the encrypted payload.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   */
  @Nullable
  public Bytes decryptDetached(Bytes cipherText, Bytes mac, Bytes data, Nonce nonce) {
    byte[] bytes = decryptDetached(cipherText.toArrayUnsafe(), mac.toArrayUnsafe(), data.toArrayUnsafe(), nonce);
    return (bytes != null) ? Bytes.wrap(bytes) : null;
  }

  /**
   * Decrypt a message using a detached message authentication code.
   *
   * @param cipherText The cipher text to decrypt.
   * @param mac The message authentication code.
   * @param data Extra non-confidential data that is included within the encrypted payload.
   * @param nonce The nonce that was used for encryption.
   * @return The decrypted data, or {@code null} if verification failed.
   * @throws UnsupportedOperationException If AES256-GSM support is not available.
   */
  @Nullable
  public byte[] decryptDetached(byte[] cipherText, byte[] mac, byte[] data, Nonce nonce) {
    assertAvailable();

    long abytes = Sodium.crypto_aead_aes256gcm_abytes();
    if (abytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_aead_aes256gcm_abytes: " + abytes + " is too large");
    }
    if (mac.length != abytes) {
      throw new IllegalArgumentException("mac must be " + abytes + " bytes, got " + mac.length);
    }

    byte[] clearText = new byte[cipherText.length];
    int rc = Sodium
        .crypto_aead_aes256gcm_decrypt_detached_afternm(
            clearText,
            null,
            cipherText,
            cipherText.length,
            mac,
            data,
            data.length,
            nonce.value.pointer(),
            ctx);
    if (rc == -1) {
      return null;
    }
    if (rc != 0) {
      throw new SodiumException("crypto_aead_aes256gcm_decrypt_detached_afternm: failed with result " + rc);
    }

    return clearText;
  }

  private void assertOpen() {
    if (ctx == null) {
      throw new IllegalStateException(getClass().getName() + ": already closed");
    }
  }

  private static byte[] maybeSliceResult(byte[] bytes, LongLongByReference actualLength, String methodName) {
    if (actualLength.longValue() == bytes.length) {
      return bytes;
    }
    if (actualLength.longValue() > Integer.MAX_VALUE) {
      throw new SodiumException(methodName + ": result of length " + actualLength.longValue() + " is too large");
    }
    return Arrays.copyOfRange(bytes, 0, actualLength.intValue());
  }

  @Override
  public void close() {
    if (ctx != null) {
      Sodium.sodium_free(ctx);
      ctx = null;
    }
  }

  @Override
  protected void finalize() {
    close();
  }
}
