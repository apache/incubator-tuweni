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

import java.util.Objects;
import javax.security.auth.Destroyable;

import jnr.ffi.Pointer;
import org.jetbrains.annotations.Nullable;

/**
 * Key exchange.
 *
 * <p>
 * Allows two parties to securely compute a set of shared keys using their peer's public key and their own secret key.
 */
public final class KeyExchange {

  /**
   * A KeyExchange public key.
   */
  public static final class PublicKey {
    final Allocated value;

    private PublicKey(Pointer ptr, int length) {
      this.value = new Allocated(ptr, length);
    }

    /**
     * Create a {@link PublicKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the public key.
     * @return A public key.
     */
    public static PublicKey fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link PublicKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the public key.
     * @return A public key.
     */
    public static PublicKey fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_kx_publickeybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_kx_publickeybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, PublicKey::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_kx_publickeybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_kx_publickeybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof PublicKey)) {
        return false;
      }
      PublicKey other = (PublicKey) obj;
      return other.value.equals(value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    /**
     * Provides the bytes of this key.
     * 
     * @return The bytes of this key.
     */
    public Bytes bytes() {
      return value.bytes();
    }

    /**
     * Provides the bytes of this key.
     * 
     * @return The bytes of this key.
     */
    public byte[] bytesArray() {
      return value.bytesArray();
    }
  }

  /**
   * A KeyExchange secret key.
   */
  public static final class SecretKey implements Destroyable {
    final Allocated value;

    private SecretKey(Pointer ptr, int length) {
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
     * Create a {@link SecretKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the secret key.
     * @return A secret key.
     */
    public static SecretKey fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link SecretKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the secret key.
     * @return A secret key.
     */
    public static SecretKey fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_kx_secretkeybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_kx_secretkeybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, SecretKey::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_kx_secretkeybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_kx_secretkeybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof SecretKey)) {
        return false;
      }
      SecretKey other = (SecretKey) obj;
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
   * A KeyExchange key pair seed.
   */
  public static final class Seed {
    final Allocated value;

    private Seed(Pointer ptr, int length) {
      this.value = new Allocated(ptr, length);
    }

    /**
     * Create a {@link Seed} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the seed.
     * @return A seed.
     */
    public static Seed fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link Seed} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the seed.
     * @return A seed.
     */
    public static Seed fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_kx_seedbytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_kx_seedbytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Seed::new);
    }

    /**
     * Obtain the length of the seed in bytes (32).
     *
     * @return The length of the seed in bytes (32).
     */
    public static int length() {
      long seedbytes = Sodium.crypto_kx_seedbytes();
      if (seedbytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_kx_seedbytes: " + seedbytes + " is too large");
      }
      return (int) seedbytes;
    }

    /**
     * Generate a new {@link Seed} using a random generator.
     *
     * @return A randomly generated seed.
     */
    public static Seed random() {
      return Sodium.randomBytes(length(), Seed::new);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Seed)) {
        return false;
      }
      Seed other = (Seed) obj;
      return other.value.equals(value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    /**
     * Provides the bytes of this seed.
     * 
     * @return The bytes of this seed.
     */
    public Bytes bytes() {
      return value.bytes();
    }

    /**
     * Provides the bytes of this seed.
     * 
     * @return The bytes of this seed.
     */
    public byte[] bytesArray() {
      return value.bytesArray();
    }
  }

  /**
   * A KeyExchange key pair.
   */
  public static final class KeyPair {

    private final PublicKey publicKey;
    private final SecretKey secretKey;

    /**
     * Create a {@link KeyPair} from pair of keys.
     *
     * @param publicKey The bytes for the public key.
     * @param secretKey The bytes for the secret key.
     */
    public KeyPair(PublicKey publicKey, SecretKey secretKey) {
      this.publicKey = publicKey;
      this.secretKey = secretKey;
    }

    /**
     * Create a {@link KeyPair} from a secret key.
     *
     * @param secretKey The secret key.
     * @return A {@link KeyPair}.
     */
    public static KeyPair forSecretKey(SecretKey secretKey) {
      if (secretKey.isDestroyed()) {
        throw new IllegalArgumentException("SecretKey has been destroyed");
      }
      return Sodium.scalarMultBase(secretKey.value.pointer(), SecretKey.length(), (ptr, len) -> {
        int publicKeyLength = PublicKey.length();
        if (len != publicKeyLength) {
          throw new IllegalStateException(
              "Public key length " + publicKeyLength + " is not same as generated key length " + len);
        }
        return new KeyPair(new PublicKey(ptr, publicKeyLength), secretKey);
      });
    }

    /**
     * Generate a new key using a random generator.
     *
     * @return A randomly generated key pair.
     */
    public static KeyPair random() {
      int publicKeyLength = PublicKey.length();
      Pointer publicKey = Sodium.malloc(publicKeyLength);
      Pointer secretKey = null;
      try {
        int secretKeyLength = SecretKey.length();
        secretKey = Sodium.malloc(secretKeyLength);
        int rc = Sodium.crypto_kx_keypair(publicKey, secretKey);
        if (rc != 0) {
          throw new SodiumException("crypto_kx_keypair: failed with result " + rc);
        }
        PublicKey pk = new PublicKey(publicKey, publicKeyLength);
        publicKey = null;
        SecretKey sk = new SecretKey(secretKey, secretKeyLength);
        secretKey = null;
        return new KeyPair(pk, sk);
      } catch (Throwable e) {
        if (publicKey != null) {
          Sodium.sodium_free(publicKey);
        }
        if (secretKey != null) {
          Sodium.sodium_free(secretKey);
        }
        throw e;
      }
    }

    /**
     * Generate a new key using a seed.
     *
     * @param seed A seed.
     * @return The generated key pair.
     */
    public static KeyPair fromSeed(Seed seed) {
      int publicKeyLength = PublicKey.length();
      Pointer publicKey = Sodium.malloc(publicKeyLength);
      Pointer secretKey = null;
      try {
        int secretKeyLength = SecretKey.length();
        secretKey = Sodium.malloc(secretKeyLength);
        int rc = Sodium.crypto_kx_seed_keypair(publicKey, secretKey, seed.value.pointer());
        if (rc != 0) {
          throw new SodiumException("crypto_kx_seed_keypair: failed with result " + rc);
        }
        PublicKey pk = new PublicKey(publicKey, publicKeyLength);
        publicKey = null;
        SecretKey sk = new SecretKey(secretKey, secretKeyLength);
        secretKey = null;
        return new KeyPair(pk, sk);
      } catch (Throwable e) {
        if (publicKey != null) {
          Sodium.sodium_free(publicKey);
        }
        if (secretKey != null) {
          Sodium.sodium_free(secretKey);
        }
        throw e;
      }
    }

    /**
     * Provides the public key of the key pair.
     * 
     * @return The public key of the key pair.
     */
    public PublicKey publicKey() {
      return publicKey;
    }

    /**
     * Provides the secret key of the key pair.
     * 
     * @return The secret key of the key pair.
     */
    public SecretKey secretKey() {
      return secretKey;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof KeyPair)) {
        return false;
      }
      KeyPair other = (KeyPair) obj;
      return this.publicKey.equals(other.publicKey) && this.secretKey.equals(other.secretKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(publicKey, secretKey);
    }
  }

  /**
   * A KeyExchange session key.
   */
  public static final class SessionKey implements Destroyable {
    @Nullable
    private Pointer ptr;
    private final int length;

    private SessionKey(Pointer ptr, int length) {
      this.ptr = ptr;
      this.length = length;
    }

    @Override
    protected void finalize() {
      destroy();
    }

    @Override
    public void destroy() {
      if (ptr != null) {
        Pointer p = ptr;
        ptr = null;
        Sodium.sodium_free(p);
      }
    }

    @Override
    public boolean isDestroyed() {
      return ptr == null;
    }

    /**
     * Create a {@link SessionKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the public key.
     * @return A public key.
     */
    public static SessionKey fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link SessionKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the public key.
     * @return A public key.
     */
    public static SessionKey fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_kx_sessionkeybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_kx_sessionkeybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, SessionKey::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_kx_sessionkeybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_kx_sessionkeybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof SessionKey)) {
        return false;
      }
      if (this.ptr == null) {
        throw new IllegalStateException("SessionKey has been destroyed");
      }
      SessionKey other = (SessionKey) obj;
      return other.ptr != null && Sodium.sodium_memcmp(this.ptr, other.ptr, length) == 0;
    }

    @Override
    public int hashCode() {
      if (this.ptr == null) {
        throw new IllegalStateException("SessionKey has been destroyed");
      }
      return Sodium.hashCode(ptr, length);
    }

    /**
     * Obtain the bytes of this key.
     *
     * WARNING: This will cause the key to be copied into heap memory.
     *
     * @return The bytes of this key.
     */
    public Bytes bytes() {
      return Bytes.wrap(bytesArray());
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
      if (this.ptr == null) {
        throw new IllegalStateException("SessionKey has been destroyed");
      }
      return Sodium.reify(ptr, length);
    }
  }

  /**
   * A KeyExchange session key pair.
   */
  public static final class SessionKeyPair {
    private final SessionKey rxKey;
    private final SessionKey txKey;

    /**
     * Create a {@link KeyPair} from pair of keys.
     *
     * @param rxKey The bytes for the secret key.
     * @param txKey The bytes for the public key.
     */
    public SessionKeyPair(SessionKey rxKey, SessionKey txKey) {
      this.rxKey = rxKey;
      this.txKey = txKey;
    }

    /**
     * Provides the session key that will be used to receive data.
     * 
     * @return The session key that will be used to receive data.
     */
    public SessionKey rx() {
      return rxKey;
    }

    /**
     * Provides the session key that will be used to send data.
     * 
     * @return The session key that will be used to send data.
     */
    public SessionKey tx() {
      return txKey;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof SessionKeyPair)) {
        return false;
      }
      SessionKeyPair other = (SessionKeyPair) obj;
      return this.rxKey.equals(other.rxKey) && this.txKey.equals(other.txKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(rxKey, txKey);
    }
  }

  /**
   * Computer a pair of session keys for use by a client.
   *
   * @param clientKeys The client key pair.
   * @param serverKey The server public key.
   * @return A pair of session keys.
   */
  public static SessionKeyPair client(KeyPair clientKeys, PublicKey serverKey) {
    if (clientKeys.secretKey.isDestroyed()) {
      throw new IllegalStateException("SecretKey has been destroyed");
    }
    long sessionkeybytes = Sodium.crypto_kx_sessionkeybytes();
    if (sessionkeybytes > Integer.MAX_VALUE) {
      throw new SodiumException("crypto_kx_sessionkeybytes: " + sessionkeybytes + " is too large");
    }
    Pointer rxPtr = null;
    Pointer txPtr = null;
    try {
      rxPtr = Sodium.malloc(sessionkeybytes);
      txPtr = Sodium.malloc(sessionkeybytes);
      int rc = Sodium
          .crypto_kx_client_session_keys(
              rxPtr,
              txPtr,
              clientKeys.publicKey.value.pointer(),
              clientKeys.secretKey.value.pointer(),
              serverKey.value.pointer());
      if (rc != 0) {
        throw new SodiumException("crypto_kx_client_session_keys: failed with result " + rc);
      }
      SessionKey rxKey = new SessionKey(rxPtr, (int) sessionkeybytes);
      rxPtr = null;
      SessionKey txKey = new SessionKey(txPtr, (int) sessionkeybytes);
      txPtr = null;
      return new SessionKeyPair(rxKey, txKey);
    } catch (Throwable e) {
      if (rxPtr != null) {
        Sodium.sodium_free(rxPtr);
      }
      if (txPtr != null) {
        Sodium.sodium_free(txPtr);
      }
      throw e;
    }
  }

  /**
   * Computer a pair of session keys for use by a client.
   *
   * @param serverKeys The server key pair.
   * @param clientKey The client public key.
   * @return A pair of session keys.
   */
  public static SessionKeyPair server(KeyPair serverKeys, PublicKey clientKey) {
    if (serverKeys.secretKey.isDestroyed()) {
      throw new IllegalArgumentException("SecretKey has been destroyed");
    }
    long sessionkeybytes = Sodium.crypto_kx_sessionkeybytes();
    if (sessionkeybytes > Integer.MAX_VALUE) {
      throw new SodiumException("crypto_kx_sessionkeybytes: " + sessionkeybytes + " is too large");
    }
    Pointer rxPtr = null;
    Pointer txPtr = null;
    try {
      rxPtr = Sodium.malloc(sessionkeybytes);
      txPtr = Sodium.malloc(sessionkeybytes);
      int rc = Sodium
          .crypto_kx_server_session_keys(
              rxPtr,
              txPtr,
              serverKeys.publicKey.value.pointer(),
              serverKeys.secretKey.value.pointer(),
              clientKey.value.pointer());
      if (rc != 0) {
        throw new SodiumException("crypto_kx_client_session_keys: failed with result " + rc);
      }
      SessionKey rxKey = new SessionKey(rxPtr, (int) sessionkeybytes);
      rxPtr = null;
      SessionKey txKey = new SessionKey(txPtr, (int) sessionkeybytes);
      txPtr = null;
      return new SessionKeyPair(rxKey, txKey);
    } catch (Throwable e) {
      if (rxPtr != null) {
        Sodium.sodium_free(rxPtr);
      }
      if (txPtr != null) {
        Sodium.sodium_free(txPtr);
      }
      throw e;
    }
  }
}
