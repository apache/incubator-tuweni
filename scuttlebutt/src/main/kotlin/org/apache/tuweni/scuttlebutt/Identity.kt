/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.scuttlebutt

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.crypto.sodium.Signature

/**
 * A Scuttlebutt identity, backed by a public key.
 *
 * Currently supported: Ed25519 and SECP256K1.
 */
interface Identity {
  /**
   * Curves supported by those identities.
   */
  enum class Curve(val curveName: String) {
    /**
     * Edwards curve
     */
    Ed25519("ed25519"),

    /**
     * SECP256K1 curve
     */
    SECP256K1("secp256k1");
  }

  /**
   * Hashes data using the secret key of the identity.
   *
   * @param message the message to sign
   * @return the signature
   * @throws UnsupportedOperationException if the identity doesn't contain a secret key
   */
  fun sign(message: Bytes): Bytes

  /**
   * Verifies a signature matches a message according to the public key of the identity.
   *
   * @param signature the signature to test
   * @param message the data that was signed by the signature
   * @return true if the signature matches the message according to the public key of the identity
   */
  fun verify(signature: Bytes, message: Bytes): Boolean

  /**
   * Provides the base64 encoded representation of the public key of the identity
   *
   * @return the base64 encoded representation of the public key of the identity
   */
  fun publicKeyAsBase64String(): String

  /**
   * Provides the curve associated with this identity
   *
   * @return the curve associated with this identity
   */
  fun curve(): Curve

  /**
   * Provides the name of the curve associated with this identity
   *
   * @return the name of the curve associated with this identity
   */
  fun curveName(): String {
    return curve().curveName
  }

  /**
   * Provides the identity's associated Ed25519 public key.
   *
   * @return the identity's associated Ed25519 public key
   * @throws UnsupportedOperationException if the identity does not use the Ed25519 algorithm.
   */
  fun ed25519PublicKey(): Signature.PublicKey?

  /**
   * Provides the identity's associated SECP256K1 public key.
   *
   * @return the identity's associated SECP256K1 public key
   * @throws UnsupportedOperationException if the identity does not use the SECP256K1 algorithm.
   */
  fun secp256k1PublicKey(): SECP256K1.PublicKey

  /**
   * Encodes the identity into a canonical Scuttlebutt identity string
   *
   * @return the identity, as a Scuttlebutt identity string representation
   */
  fun toCanonicalForm(): String {
    return "@" + publicKeyAsBase64String() + "." + curveName()
  }

  companion object {
    /**
     * Creates a new Ed25519 identity backed by this key pair.
     *
     * @param keyPair the key pair of the identity
     * @return a new Scuttlebutt identity
     */
    @JvmStatic
    fun fromKeyPair(keyPair: Signature.KeyPair): Identity {
      return Ed25519KeyPairIdentity(keyPair)
    }

    /**
     * Creates a new SECP256K1 identity backed by this key pair.
     *
     * @param keyPair the key pair of the identity
     * @return a new Scuttlebutt identity
     */
    @JvmStatic
    fun fromKeyPair(keyPair: SECP256K1.KeyPair): Identity {
      return SECP256K1KeyPairIdentity(keyPair)
    }

    /**
     * Creates a new Ed25519 identity backed by this secret key.
     *
     * @param secretKey the secret key of the identity
     * @return a new Scuttlebutt identity
     */
    @JvmStatic
    fun fromSecretKey(secretKey: Signature.SecretKey): Identity {
      return fromKeyPair(
        Signature.KeyPair.forSecretKey(
          secretKey
        )
      )
    }

    /**
     * Creates a new SECP256K1 identity backed by this secret key.
     *
     * @param secretKey the secret key of the identity
     * @return a new Scuttlebutt identity
     */
    @JvmStatic
    fun fromSecretKey(secretKey: SECP256K1.SecretKey): Identity {
      return fromKeyPair(SECP256K1.KeyPair.fromSecretKey(secretKey))
    }

    /**
     * Creates a new random Ed25519 identity.
     *
     * @return a new Scuttlebutt identity
     */
    @JvmStatic
    fun random(): Identity {
      return randomEd25519()
    }

    /**
     * Creates a new random Ed25519 identity.
     *
     * @return a new Scuttlebutt identity
     */
    @JvmStatic
    fun randomEd25519(): Identity {
      return Ed25519KeyPairIdentity(Signature.KeyPair.random())
    }

    /**
     * Creates a new random secp251k1 identity.
     *
     * @return a new Scuttlebutt identity
     */
    @JvmStatic
    fun randomSECP256K1(): Identity {
      return SECP256K1KeyPairIdentity(SECP256K1.KeyPair.random())
    }

    /**
     * Creates a new SECP256K1 identity backed by this public key.
     *
     * @param publicKey the public key of the identity
     * @return a new Scuttlebutt identity
     */
    @JvmStatic
    fun fromPublicKey(publicKey: SECP256K1.PublicKey): Identity {
      return SECP256K1PublicKeyIdentity(publicKey)
    }

    /**
     * Creates a new Ed25519 identity backed by this public key.
     *
     * @param publicKey the public key of the identity
     * @return a new Scuttlebutt identity
     */
    @JvmStatic
    fun fromPublicKey(publicKey: Signature.PublicKey): Identity {
      return Ed25519PublicKeyIdentity(publicKey)
    }
  }
}
