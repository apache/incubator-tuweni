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

// Documentation copied under the ISC License, from
// https://github.com/jedisct1/libsodium-doc/blob/424b7480562c2e063bc8c52c452ef891621c8480/public-key_cryptography/public-key_signatures.md

import static com.google.common.base.Preconditions.checkArgument;

import org.apache.tuweni.bytes.Bytes;

import java.util.Arrays;
import java.util.Objects;
import javax.security.auth.Destroyable;

import jnr.ffi.Pointer;
import jnr.ffi.byref.LongLongByReference;

/**
 * Public-key signatures.
 *
 * <p>
 * In this system, a signer generates a key pair:
 * <ul>
 *
 * <li>a secret key, that will be used to append a signature to any number of messages</li>
 *
 * <li>a public key, that anybody can use to verify that the signature appended to a message was actually issued by the
 * creator of the public key.</li>
 *
 * </ul>
 *
 * <p>
 * Verifiers need to already know and ultimately trust a public key before messages signed using it can be verified.
 *
 * <p>
 * Warning: this is different from authenticated encryption. Appending a signature does not change the representation of
 * the message itself.
 *
 * <p>
 * This class depends upon the JNR-FFI library being available on the classpath, along with its dependencies. See
 * https://github.com/jnr/jnr-ffi. JNR-FFI can be included using the gradle dependency 'com.github.jnr:jnr-ffi'.
 */
public final class Signature {

  /**
   * A signing public key.
   */
  public static final class PublicKey {
    final Allocated value;

    private PublicKey(Pointer ptr, int length) {
      this.value = new Allocated(ptr, length);
    }

    /**
     * Create a {@link Signature.PublicKey} from an array of bytes.
     *
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the public key.
     * @return A public key.
     */
    public static Signature.PublicKey fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link Signature.PublicKey} from an array of bytes.
     *
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the public key.
     * @return A public key.
     */
    public static Signature.PublicKey fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_sign_publickeybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_sign_publickeybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, PublicKey::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_sign_publickeybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_sign_publickeybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
    }

    /**
     * Verifies the signature of a message.
     *
     * @param message the message itself
     * @param signature the signature of the message
     * @return true if the signature matches the message according to this public key
     */
    public boolean verify(Bytes message, Bytes signature) {
      return Signature.verifyDetached(message, signature, this);
    }

    /**
     * Verifies the signature of a message.
     *
     * @param message the message itself
     * @param signature the signature of the message
     * @return true if the signature matches the message according to this public key
     */
    public boolean verify(Allocated message, Allocated signature) {
      return Signature.verifyDetached(message, signature, this);
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
      return Objects.equals(this.value, other.value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    /**
     * @return The bytes of this key.
     */
    public Bytes bytes() {
      return value.bytes();
    }

    /**
     * @return The bytes of this key.
     */
    public byte[] bytesArray() {
      return value.bytesArray();
    }
  }

  /**
   * A Signature secret key.
   */
  public static final class SecretKey implements Destroyable {
    Allocated value;

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
     * Create a {@link Signature.SecretKey} from an array of bytes.
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
     * Create a {@link Signature.SecretKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the secret key.
     * @return A secret key.
     */
    public static SecretKey fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_sign_secretkeybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_sign_secretkeybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, SecretKey::new);
    }

    public static SecretKey fromSeed(Seed seed) {
      return Sodium.dup(seed.bytes().toArray(), SecretKey::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_sign_secretkeybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_sign_secretkeybytes: " + keybytes + " is too large");
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
      return other.value.equals(this.value);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(value);
    }

    /**
     * Obtain the bytes of this key.
     *
     * WARNING: This will cause the key to be copied into heap memory.
     *
     * @return The bytes of this key.
     * @deprecated Use {@link #bytesArray()} to obtain the bytes as an array, which should be overwritten when no longer
     *             required.
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
   * A Signature key pair seed.
   */
  public static final class Seed {
    private final Allocated value;

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
      if (bytes.length != Sodium.crypto_sign_seedbytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_sign_seedbytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Seed::new);
    }

    /**
     * Obtain the length of the seed in bytes (32).
     *
     * @return The length of the seed in bytes (32).
     */
    public static int length() {
      long seedbytes = Sodium.crypto_sign_seedbytes();
      if (seedbytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_sign_seedbytes: " + seedbytes + " is too large");
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
      return Objects.hashCode(value);
    }

    /**
     * @return The bytes of this seed.
     */
    public Bytes bytes() {
      return value.bytes();
    }

    /**
     * @return The bytes of this seed.
     */
    public byte[] bytesArray() {
      return value.bytesArray();
    }
  }

  /**
   * A Signature key pair.
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
      checkArgument(!secretKey.value.isDestroyed(), "SecretKey has been destroyed");
      int publicKeyLength = PublicKey.length();
      Pointer publicKey = Sodium.malloc(publicKeyLength);
      try {
        int rc = Sodium.crypto_sign_ed25519_sk_to_pk(publicKey, secretKey.value.pointer());
        if (rc != 0) {
          throw new SodiumException("crypto_sign_ed25519_sk_to_pk: failed with result " + rc);
        }
        PublicKey pk = new PublicKey(publicKey, publicKeyLength);
        publicKey = null;
        return new KeyPair(pk, secretKey);
      } catch (Throwable e) {
        if (publicKey != null) {
          Sodium.sodium_free(publicKey);
        }
        throw e;
      }
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
        int rc = Sodium.crypto_sign_keypair(publicKey, secretKey);
        if (rc != 0) {
          throw new SodiumException("crypto_sign_keypair: failed with result " + rc);
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
        int rc = Sodium.crypto_sign_seed_keypair(publicKey, secretKey, seed.value.pointer());
        if (rc != 0) {
          throw new SodiumException("crypto_sign_seed_keypair: failed with result " + rc);
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
     * @return The public key of the key pair.
     */
    public PublicKey publicKey() {
      return publicKey;
    }

    /**
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

  private Signature() {}

  /**
   * Signs a message for a given key.
   *
   * @param message The message to sign.
   * @param secretKey The secret key to sign the message with.
   * @return The signature of the message.
   */
  public static Bytes signDetached(Bytes message, SecretKey secretKey) {
    return Bytes.wrap(signDetached(message.toArrayUnsafe(), secretKey));
  }

  /**
   * Signs a message for a given key.
   *
   * @param message The message to sign.
   * @param secretKey The secret key to sign the message with.
   * @return The signature of the message.
   */
  public static Allocated signDetached(Allocated message, SecretKey secretKey) {
    checkArgument(!secretKey.value.isDestroyed(), "SecretKey has been destroyed");
    Allocated signature = Allocated.allocate(Sodium.crypto_sign_bytes());
    int rc = Sodium.crypto_sign_detached(
        signature.pointer(),
        new LongLongByReference(Sodium.crypto_sign_bytes()),
        message.pointer(),
        (long) message.length(),
        secretKey.value.pointer());
    if (rc != 0) {
      throw new SodiumException("crypto_sign_detached: failed with result " + rc);
    }

    return signature;
  }

  /**
   * Signs a message for a given key.
   *
   * @param message The message to sign.
   * @param secretKey The secret key to sign the message with.
   * @return The signature of the message.
   */
  public static byte[] signDetached(byte[] message, SecretKey secretKey) {
    checkArgument(!secretKey.value.isDestroyed(), "SecretKey has been destroyed");
    byte[] signature = new byte[(int) Sodium.crypto_sign_bytes()];
    int rc = Sodium.crypto_sign_detached(signature, null, message, message.length, secretKey.value.pointer());
    if (rc != 0) {
      throw new SodiumException("crypto_sign_detached: failed with result " + rc);
    }

    return signature;
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param message The cipher text to decrypt.
   * @param signature The public key of the sender.
   * @param publicKey The secret key of the receiver.
   * @return whether the signature matches the message according to the public key.
   */
  public static boolean verifyDetached(Bytes message, Bytes signature, PublicKey publicKey) {
    return verifyDetached(message.toArrayUnsafe(), signature.toArrayUnsafe(), publicKey);
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param message The cipher text to decrypt.
   * @param signature The public key of the sender.
   * @param publicKey The secret key of the receiver.
   * @return whether the signature matches the message according to the public key.
   */
  public static boolean verifyDetached(Allocated message, Allocated signature, PublicKey publicKey) {
    int rc = Sodium.crypto_sign_verify_detached(
        signature.pointer(),
        message.pointer(),
        message.length(),
        publicKey.value.pointer());
    if (rc == -1) {
      return false;
    }
    if (rc != 0) {
      throw new SodiumException("crypto_sign_verify_detached: failed with result " + rc);
    }

    return true;
  }

  /**
   * Decrypt a message using a given key.
   *
   * @param message The cipher text to decrypt.
   * @param signature The public key of the sender.
   * @param publicKey The secret key of the receiver.
   * @return whether the signature matches the message according to the public key.
   */
  public static boolean verifyDetached(byte[] message, byte[] signature, PublicKey publicKey) {
    int rc = Sodium.crypto_sign_verify_detached(signature, message, message.length, publicKey.value.pointer());
    if (rc == -1) {
      return false;
    }
    if (rc != 0) {
      throw new SodiumException("crypto_sign_verify_detached: failed with result " + rc);
    }

    return true;
  }

  /**
   * Signs a message for a given key.
   *
   * @param message The message to sign.
   * @param secretKey The secret key to sign the message with.
   * @return The signature prepended to the message
   */
  public static Bytes sign(Bytes message, SecretKey secretKey) {
    return Bytes.wrap(sign(message.toArrayUnsafe(), secretKey));
  }

  /**
   * Signs a message for a given key.
   *
   * @param message The message to sign.
   * @param secretKey The secret key to sign the message with.
   * @return The signature prepended to the message
   */
  public static byte[] sign(byte[] message, SecretKey secretKey) {
    checkArgument(!secretKey.value.isDestroyed(), "SecretKey has been destroyed");
    byte[] signature = new byte[(int) Sodium.crypto_sign_bytes() + message.length];
    int rc = Sodium.crypto_sign(signature, null, message, message.length, secretKey.value.pointer());
    if (rc != 0) {
      throw new SodiumException("crypto_sign: failed with result " + rc);
    }

    return signature;
  }

  /**
   * Verifies the signature of the signed message using the public key and returns the message.
   *
   * @param signed signed message (signature + message)
   * @param publicKey pk used to verify the signature
   * @return the message
   */
  public static Bytes verify(Bytes signed, PublicKey publicKey) {
    return Bytes.wrap(verify(signed.toArrayUnsafe(), publicKey));
  }

  /**
   * Verifies the signature of the signed message using the public key and returns the message.
   *
   * @param signed signed message (signature + message)
   * @param publicKey pk used to verify the signature
   * @return the message
   */
  public static byte[] verify(byte[] signed, PublicKey publicKey) {
    byte[] message = new byte[signed.length];
    LongLongByReference messageLongReference = new LongLongByReference();
    int rc = Sodium.crypto_sign_open(message, messageLongReference, signed, signed.length, publicKey.value.pointer());
    if (rc != 0) {
      throw new SodiumException("crypto_sign_open: failed with result " + rc);
    }

    return Arrays.copyOfRange(message, 0, messageLongReference.intValue());
  }
}
