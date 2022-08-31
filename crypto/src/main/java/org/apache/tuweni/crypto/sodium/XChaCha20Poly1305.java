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

import org.apache.tuweni.bytes.Bytes;

import javax.security.auth.Destroyable;

import jnr.ffi.Pointer;
import jnr.ffi.byref.ByteByReference;
import jnr.ffi.byref.LongLongByReference;
import org.jetbrains.annotations.Nullable;

// Documentation copied under the ISC License, from
// https://github.com/jedisct1/libsodium-doc/blob/424b7480562c2e063bc8c52c452ef891621c8480/secret-key_cryptography/xchacha20-poly1305_construction.md

/**
 * Authenticated Encryption with Additional Data using XChaCha20-Poly1305.
 *
 * <p>
 * The XChaCha20-Poly1305 construction can safely encrypt a practically unlimited number of messages with the same key,
 * without any practical limit to the size of a message (up to ~ 2^64 bytes).
 *
 * <p>
 * As an alternative to counters, its large nonce size (192-bit) allows random nonces to be safely used.
 *
 * <p>
 * For this reason, and if interoperability with other libraries is not a concern, this is the recommended AEAD
 * construction.
 *
 * <p>
 * This class depends upon the JNR-FFI library being available on the classpath, along with its dependencies. See
 * https://github.com/jnr/jnr-ffi. JNR-FFI can be included using the gradle dependency 'com.github.jnr:jnr-ffi'.
 */
public final class XChaCha20Poly1305 {
  private XChaCha20Poly1305() {}

  private static final byte[] EMPTY_BYTES = new byte[0];

  /**
   * Check if Sodium and the XChaCha20Poly1305 algorithm is available.
   *
   * <p>
   * XChaCha20Poly1305 is supported in sodium native library version &gt;= 10.0.12.
   *
   * @return {@code true} if Sodium and the XChaCha20Poly1305 algorithm is available.
   */
  public static boolean isAvailable() {
    try {
      return Sodium.supportsVersion(Sodium.VERSION_10_0_12);
    } catch (UnsatisfiedLinkError e) {
      return false;
    }
  }

  private static void assertAvailable() {
    if (!isAvailable()) {
      throw new UnsupportedOperationException(
          "Sodium XChaCha20Poly1305 is not available (requires sodium native library >= 10.0.12)");
    }
  }

  /**
   * Check if Sodium and the XChaCha20Poly1305 secret stream algorithm is available.
   *
   * <p>
   * XChaCha20Poly1305 secret stream is supported in sodium native library version &gt;= 10.0.14.
   *
   * @return {@code true} if Sodium and the XChaCha20Poly1305 secret stream algorithm is available.
   */
  public static boolean isSecretStreamAvailable() {
    try {
      return Sodium.supportsVersion(Sodium.VERSION_10_0_14);
    } catch (UnsatisfiedLinkError e) {
      return false;
    }
  }

  private static void assertSecretStreamAvailable() {
    if (!isSecretStreamAvailable()) {
      throw new UnsupportedOperationException(
          "Sodium XChaCha20Poly1305 secret stream is not available (requires sodium native library >= 10.0.14)");
    }
  }

  /**
   * A XChaCha20-Poly1305 key.
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
     * @throws UnsupportedOperationException If XChaCha20Poly1305 support is not available.
     */
    public static Key fromBytes(byte[] bytes) {
      assertAvailable();
      if (bytes.length != Sodium.crypto_aead_xchacha20poly1305_ietf_keybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_aead_xchacha20poly1305_ietf_keybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Key::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     * @throws UnsupportedOperationException If XChaCha20Poly1305 support is not available.
     */
    public static int length() {
      assertAvailable();
      long keybytes = Sodium.crypto_aead_xchacha20poly1305_ietf_keybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_aead_xchacha20poly1305_ietf_keybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
    }

    /**
     * Generate a new key using a random generator.
     *
     * @return A randomly generated key.
     * @throws UnsupportedOperationException If XChaCha20Poly1305 support is not available.
     */
    public static Key random() {
      assertAvailable();
      int length = length();
      Pointer ptr = Sodium.malloc(length);
      try {
        // When support for 10.0.11 is dropped, use this instead
        //Sodium.crypto_aead_xchacha20poly1305_ietf_keygen(ptr);
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
   * A XChaCha20-Poly1305 nonce.
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
     * @throws UnsupportedOperationException If XChaCha20Poly1305 support is not available.
     */
    public static Nonce fromBytes(byte[] bytes) {
      assertAvailable();
      if (bytes.length != Sodium.crypto_aead_xchacha20poly1305_ietf_npubbytes()) {
        throw new IllegalArgumentException(
            "nonce must be " + Sodium.crypto_aead_xchacha20poly1305_ietf_npubbytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Nonce::new);
    }

    /**
     * Obtain the length of the nonce in bytes (24).
     *
     * @return The length of the nonce in bytes (24).
     * @throws UnsupportedOperationException If XChaCha20Poly1305 support is not available.
     */
    public static int length() {
      assertAvailable();
      long npubbytes = Sodium.crypto_aead_xchacha20poly1305_ietf_npubbytes();
      if (npubbytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_aead_xchacha20poly1305_ietf_npubbytes: " + npubbytes + " is too large");
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
     * @throws UnsupportedOperationException If XChaCha20Poly1305 support is not available.
     */
    public static Nonce random() {
      assertAvailable();
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
     * Provides the bytes of this nonce
     * 
     * @return The bytes of this nonce.
     */
    public Bytes bytes() {
      return value.bytes();
    }

    /**
     * Provides the bytes of this nonce
     * 
     * @return The bytes of this nonce.
     */
    public byte[] bytesArray() {
      return value.bytesArray();
    }
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
   * @throws UnsupportedOperationException If XChaCha20Poly1305 support is not available.
   */
  public static byte[] encrypt(byte[] message, byte[] data, Key key, Nonce nonce) {
    assertAvailable();
    if (key.isDestroyed()) {
      throw new IllegalArgumentException("Key has been destroyed");
    }
    byte[] cipherText = new byte[maxCypherTextLength(message)];

    LongLongByReference cipherTextLen = new LongLongByReference();
    int rc = Sodium
        .crypto_aead_xchacha20poly1305_ietf_encrypt(
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
      throw new SodiumException("crypto_aead_xchacha20poly1305_ietf_encrypt: failed with result " + rc);
    }

    return maybeSliceResult(cipherText, cipherTextLen, "crypto_aead_xchacha20poly1305_ietf_encrypt");
  }

  private static int maxCypherTextLength(byte[] message) {
    long abytes = Sodium.crypto_aead_xchacha20poly1305_ietf_abytes();
    if (abytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_aead_xchacha20poly1305_ietf_abytes: " + abytes + " is too large");
    }
    return (int) abytes + message.length;
  }

  /**
   * Encrypt a message for a given key, generating a detached message authentication code.
   *
   * @param message The message to encrypt.
   * @param key The key to encrypt for.
   * @param nonce A unique nonce.
   * @return The encrypted data.
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
   * @return The encrypted data.
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
   * @return The encrypted data.
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
   * @return The encrypted data.
   * @throws UnsupportedOperationException If XChaCha20Poly1305 support is not available.
   */
  public static DetachedEncryptionResult encryptDetached(byte[] message, byte[] data, Key key, Nonce nonce) {
    assertAvailable();
    if (key.isDestroyed()) {
      throw new IllegalArgumentException("Key has been destroyed");
    }
    byte[] cipherText = new byte[message.length];
    long abytes = Sodium.crypto_aead_xchacha20poly1305_ietf_abytes();
    if (abytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_aead_xchacha20poly1305_ietf_abytes: " + abytes + " is too large");
    }
    byte[] mac = new byte[(int) abytes];

    LongLongByReference macLen = new LongLongByReference();
    int rc = Sodium
        .crypto_aead_xchacha20poly1305_ietf_encrypt_detached(
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
      throw new SodiumException("crypto_aead_xchacha20poly1305_ietf_encrypt_detached: failed with result " + rc);
    }

    return new DefaultDetachedEncryptionResult(
        cipherText,
        maybeSliceResult(mac, macLen, "crypto_aead_xchacha20poly1305_ietf_encrypt_detached"));
  }

  private static final byte TAG_FINAL = (0x01 | 0x02);

  private static final class SSEncrypt implements SecretEncryptionStream {
    private final int abytes;
    private final byte[] header;
    @Nullable
    private Pointer state;
    private boolean complete = false;

    private SSEncrypt(Key key) {
      if (key.isDestroyed()) {
        throw new IllegalArgumentException("Key has been destroyed");
      }
      long abytes = Sodium.crypto_secretstream_xchacha20poly1305_abytes();
      if (abytes > Integer.MAX_VALUE) {
        throw new IllegalStateException("crypto_aead_xchacha20poly1305_ietf_abytes: " + abytes + " is too large");
      }
      this.abytes = (int) abytes;

      long headerbytes = Sodium.crypto_secretstream_xchacha20poly1305_headerbytes();
      if (headerbytes > Integer.MAX_VALUE) {
        throw new IllegalStateException(
            "crypto_secretstream_xchacha20poly1305_headerbytes: " + abytes + " is too large");
      }
      this.header = new byte[(int) headerbytes];

      Pointer state = Sodium.malloc(Sodium.crypto_secretstream_xchacha20poly1305_statebytes());
      try {
        int rc = Sodium.crypto_secretstream_xchacha20poly1305_init_push(state, header, key.value.pointer());
        if (rc != 0) {
          throw new SodiumException("crypto_secretstream_xchacha20poly1305_init_push: failed with result " + rc);
        }
      } catch (Throwable e) {
        Sodium.sodium_free(state);
        throw e;
      }
      this.state = state;
    }

    @Override
    protected void finalize() {
      destroy();
    }

    @Override
    public void destroy() {
      if (state != null) {
        Pointer p = state;
        state = null;
        Sodium.sodium_free(p);
      }
    }

    @Override
    public boolean isDestroyed() {
      return state == null;
    }

    @Override
    public byte[] headerArray() {
      return header;
    }

    @Override
    public byte[] push(byte[] clearText, boolean isFinal) {
      if (complete) {
        throw new IllegalStateException("stream already completed");
      }
      if (state == null) {
        throw new IllegalStateException("stream has been destroyed");
      }
      byte[] cipherText = new byte[abytes + clearText.length];
      byte tag = isFinal ? TAG_FINAL : 0;
      int rc = Sodium
          .crypto_secretstream_xchacha20poly1305_push(
              state,
              cipherText,
              null,
              clearText,
              clearText.length,
              null,
              0,
              tag);
      if (rc != 0) {
        throw new SodiumException("crypto_secretstream_xchacha20poly1305_push: failed with result " + rc);
      }
      if (isFinal) {
        complete = true;
        // destroy state before finalization, as it will not be re-used
        destroy();
      }
      return cipherText;
    }
  }

  /**
   * Open an encryption stream.
   *
   * @param key The key to encrypt for.
   * @return The input stream.
   * @throws UnsupportedOperationException If XChaCha20Poly1305 secret stream support is not available.
   */
  public static SecretEncryptionStream openEncryptionStream(Key key) {
    assertSecretStreamAvailable();
    return new SSEncrypt(key);
  }

  /**
   * Decrypt a message using a given key.
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
   * Decrypt a message using a given key.
   *
   * @param cipherText The cipher text to decrypt.
   * @param key The key to use for decryption.
   * @param nonce The nonce that was used for encryption.
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
   * @throws UnsupportedOperationException If XChaCha20Poly1305 support is not available.
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
        .crypto_aead_xchacha20poly1305_ietf_decrypt(
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
      throw new SodiumException("crypto_aead_xchacha20poly1305_ietf_decrypt: failed with result " + rc);
    }

    return maybeSliceResult(clearText, clearTextLen, "crypto_aead_xchacha20poly1305_ietf_decrypt");
  }

  private static int maxClearTextLength(byte[] cipherText) {
    long abytes = Sodium.crypto_aead_xchacha20poly1305_ietf_abytes();
    if (abytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_aead_xchacha20poly1305_ietf_abytes: " + abytes + " is too large");
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
   * @throws UnsupportedOperationException If XChaCha20Poly1305 support is not available.
   */
  @Nullable
  public static byte[] decryptDetached(byte[] cipherText, byte[] mac, byte[] data, Key key, Nonce nonce) {
    assertAvailable();
    if (key.isDestroyed()) {
      throw new IllegalArgumentException("Key has been destroyed");
    }
    long abytes = Sodium.crypto_aead_xchacha20poly1305_ietf_abytes();
    if (abytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_aead_xchacha20poly1305_ietf_abytes: " + abytes + " is too large");
    }
    if (mac.length != abytes) {
      throw new IllegalArgumentException("mac must be " + abytes + " bytes, got " + mac.length);
    }

    byte[] clearText = new byte[cipherText.length];
    int rc = Sodium
        .crypto_aead_xchacha20poly1305_ietf_decrypt_detached(
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
      throw new SodiumException("crypto_aead_xchacha20poly1305_ietf_decrypt_detached: failed with result " + rc);
    }

    return clearText;
  }

  private static final class SSDecrypt implements SecretDecryptionStream {
    private final int abytes;
    @Nullable
    private Pointer state;
    private boolean complete = false;

    private SSDecrypt(Key key, byte[] header) {
      if (key.isDestroyed()) {
        throw new IllegalArgumentException("Key has been destroyed");
      }
      if (header.length != Sodium.crypto_secretstream_xchacha20poly1305_headerbytes()) {
        throw new IllegalArgumentException(
            "header must be "
                + Sodium.crypto_secretstream_xchacha20poly1305_headerbytes()
                + " bytes, got "
                + header.length);
      }

      long abytes = Sodium.crypto_secretstream_xchacha20poly1305_abytes();
      if (abytes > Integer.MAX_VALUE) {
        throw new IllegalStateException("crypto_aead_xchacha20poly1305_ietf_abytes: " + abytes + " is too large");
      }
      this.abytes = (int) abytes;

      Pointer state = Sodium.malloc(Sodium.crypto_secretstream_xchacha20poly1305_statebytes());
      try {
        int rc = Sodium.crypto_secretstream_xchacha20poly1305_init_pull(state, header, key.value.pointer());
        if (rc != 0) {
          throw new SodiumException("crypto_secretstream_xchacha20poly1305_init_push: failed with result " + rc);
        }
      } catch (Throwable e) {
        Sodium.sodium_free(state);
        throw e;
      }
      this.state = state;
    }

    @Override
    protected void finalize() {
      destroy();
    }

    @Override
    public void destroy() {
      if (state != null) {
        Pointer p = state;
        state = null;
        Sodium.sodium_free(p);
      }
    }

    @Override
    public boolean isDestroyed() {
      return state == null;
    }

    @Override
    public byte[] pull(byte[] cipherText) {
      if (complete) {
        throw new IllegalStateException("stream already completed");
      }
      if (state == null) {
        throw new IllegalStateException("stream has been destroyed");
      }
      if (abytes > cipherText.length) {
        throw new IllegalArgumentException("cipherText is too short");
      }
      byte[] clearText = new byte[cipherText.length - abytes];
      ByteByReference tag = new ByteByReference();
      int rc = Sodium
          .crypto_secretstream_xchacha20poly1305_pull(
              state,
              clearText,
              null,
              tag,
              cipherText,
              cipherText.length,
              null,
              0);
      if (rc != 0) {
        throw new SodiumException("crypto_secretstream_xchacha20poly1305_push: failed with result " + rc);
      }
      if (tag.byteValue() == TAG_FINAL) {
        complete = true;
        // destroy state before finalization, as it will not be re-used
        destroy();
      }
      return clearText;
    }

    @Override
    public boolean isComplete() {
      return complete;
    }
  }

  /**
   * Open an decryption stream.
   *
   * @param key The key to use for decryption.
   * @param header The header for the stream.
   * @return The input stream.
   * @throws UnsupportedOperationException If XChaCha20Poly1305 secret stream support is not available.
   */
  public static SecretDecryptionStream openDecryptionStream(Key key, byte[] header) {
    assertSecretStreamAvailable();
    return new SSDecrypt(key, header);
  }

  private static byte[] maybeSliceResult(byte[] bytes, LongLongByReference actualLength, String methodName) {
    if (actualLength.longValue() == bytes.length) {
      return bytes;
    }
    if (actualLength.longValue() > Integer.MAX_VALUE) {
      throw new SodiumException(methodName + ": result of length " + actualLength.longValue() + " is too large");
    }
    byte[] result = new byte[actualLength.intValue()];
    System.arraycopy(bytes, 0, result, 0, result.length);
    return result;
  }
}
