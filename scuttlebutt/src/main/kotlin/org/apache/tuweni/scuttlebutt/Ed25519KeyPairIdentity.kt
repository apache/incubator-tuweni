// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.crypto.sodium.Signature
import java.util.Objects

/**
 * A complete Scuttlebutt identity backed by a Ed25519 key pair.
 *
 */
internal class Ed25519KeyPairIdentity(private val keyPair: Signature.KeyPair) :
  Identity {
  override fun sign(message: Bytes): Bytes {
    return Signature.signDetached(message, keyPair.secretKey())
  }

  override fun verify(signature: Bytes, message: Bytes): Boolean {
    return Signature.verifyDetached(message, signature, keyPair.publicKey())
  }

  override fun publicKeyAsBase64String(): String {
    return keyPair.publicKey().bytes().toBase64String()
  }

  override fun curve(): Identity.Curve {
    return Identity.Curve.Ed25519
  }

  override fun ed25519PublicKey(): Signature.PublicKey? {
    return keyPair.publicKey()
  }

  override fun secp256k1PublicKey(): SECP256K1.PublicKey {
    throw UnsupportedOperationException()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val identity = other as Ed25519KeyPairIdentity
    return keyPair.equals(identity.keyPair)
  }

  override fun hashCode(): Int {
    return Objects.hash(keyPair)
  }

  override fun toString(): String {
    return toCanonicalForm()
  }
}
