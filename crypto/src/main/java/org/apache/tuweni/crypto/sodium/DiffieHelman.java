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

/**
 * Sodium provides an API to perform scalar multiplication of elliptic curve points.
 * <p>
 * This can be used as a building block to construct key exchange mechanisms, or more generally to compute a public key
 * from a secret key.
 * <p>
 * On current libsodium versions, you generally want to use the crypto_kx API for key exchange instead.
 * 
 * @see KeyExchange
 */
public final class DiffieHelman {

  /**
   * A Diffie-Helman public key.
   */
  public static final class PublicKey {
    final Allocated value;

    private PublicKey(Pointer ptr, int length) {
      this.value = new Allocated(ptr, length);
    }

    /**
     * Creates a new public key based on a signature public key.
     *
     * @param publicKey the signature public key to copy
     * @return A public key.
     */
    public static PublicKey forBoxPublicKey(Box.PublicKey publicKey) {
      return new PublicKey(Sodium.dup(publicKey.value.pointer(), length()), length());
    }

    /**
     * Creates a new public key based on a signature public key.
     *
     * @param publicKey the signature public key to copy
     * @return A public key.
     */
    public static PublicKey forSignaturePublicKey(Signature.PublicKey publicKey) {
      return forBoxPublicKey(Box.PublicKey.forSignaturePublicKey(publicKey));
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
      if (bytes.length != Sodium.crypto_box_publickeybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_box_publickeybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, PublicKey::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_scalarmult_bytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_scalarmult_bytes: " + keybytes + " is too large");
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
     * Provides the bytes of this key
     * 
     * @return The bytes of this key.
     */
    public Bytes bytes() {
      return value.bytes();
    }

    /**
     * Provides the bytes of this key
     * 
     * @return The bytes of this key.
     */
    public byte[] bytesArray() {
      return value.bytesArray();
    }
  }

  /**
   * A Diffie-Helman secret key.
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
     * Creates a new secret key based on a box secret key.
     *
     * @param secretKey the box secret key to copy
     * @return A secret key.
     */
    public static SecretKey forBoxSecretKey(Box.SecretKey secretKey) {
      return new SecretKey(Sodium.dup(secretKey.value.pointer(), length()), length());
    }

    /**
     * Creates a new secret key based on a signature secret key.
     *
     * @param secretKey the signature secret key to copy
     * @return A secret key.
     */
    public static SecretKey forSignatureSecretKey(Signature.SecretKey secretKey) {
      return forBoxSecretKey(Box.SecretKey.forSignatureSecretKey(secretKey));
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
      if (bytes.length != Sodium.crypto_scalarmult_scalarbytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_scalarmult_scalarbytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, SecretKey::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_scalarmult_scalarbytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_scalarmult_scalarbytes: " + keybytes + " is too large");
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
   * A Diffie-Helman key pair.
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
     * Create a {@link KeyPair} from an array of secret key bytes.
     *
     * @param secretKey The secret key.
     * @return A {@link KeyPair}.
     */
    public static KeyPair forSecretKey(SecretKey secretKey) {
      if (secretKey.isDestroyed()) {
        throw new IllegalStateException("SecretKey has been destroyed");
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
      return Sodium.randomBytes(SecretKey.length(), (ptr, len) -> forSecretKey(new SecretKey(ptr, len)));
    }

    /**
     * Provides the public key
     * 
     * @return The public key of the key pair.
     */
    public PublicKey publicKey() {
      return publicKey;
    }

    /**
     * Provides the secret key
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
   * A Diffie-Helman shared secret.
   */
  public static final class Secret implements Destroyable {
    final Allocated value;

    private Secret(Pointer ptr, int length) {
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
     * Compute a shared {@link Secret} from a secret key and a public key.
     *
     * @param secretKey the user's secret key
     * @param publicKey another user's public key
     * @return A shared {@link Secret}.
     */
    public static Secret forKeys(SecretKey secretKey, PublicKey publicKey) {
      if (secretKey.isDestroyed()) {
        throw new IllegalStateException("SecretKey has been destroyed");
      }
      return Sodium
          .scalarMult(
              secretKey.value.pointer(),
              secretKey.value.length(),
              publicKey.value.pointer(),
              publicKey.value.length(),
              (ptr, len) -> {
                int secretLength = Secret.length();
                if (len != secretLength) {
                  throw new IllegalStateException(
                      "Secret length " + secretLength + " is not same as generated key length " + len);
                }
                return new Secret(ptr, secretLength);
              });
    }

    /**
     * Create a {@link Secret} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the secret key.
     * @return A secret key.
     */
    public static Secret fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link Secret} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the secret key.
     * @return A secret key.
     */
    public static Secret fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_scalarmult_bytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_scalarmult_bytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Secret::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_scalarmult_bytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_scalarmult_bytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Secret)) {
        return false;
      }
      Secret other = (Secret) obj;
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
}
