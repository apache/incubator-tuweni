// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.crypto.sodium.Signature
import java.util.Objects

/**
 * Ed25519 Scuttlebutt identity backed by a public key.
 *
 * This representation doesn't support signing messages.
 */
internal class Ed25519PublicKeyIdentity(private val publicKey: Signature.PublicKey) :
  Identity {
  override fun sign(message: Bytes): Bytes {
    throw UnsupportedOperationException("Cannot sign messages with a public key identity")
  }

  override fun verify(signature: Bytes, message: Bytes): Boolean {
    return Signature.verifyDetached(message, signature, publicKey)
  }

  override fun publicKeyAsBase64String(): String {
    return publicKey.bytes().toBase64String()
  }

  override fun curve(): Identity.Curve {
    return Identity.Curve.Ed25519
  }

  override fun ed25519PublicKey(): Signature.PublicKey? {
    return publicKey
  }

  override fun secp256k1PublicKey(): SECP256K1.PublicKey {
    throw UnsupportedOperationException()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val identity = other as Ed25519PublicKeyIdentity
    return publicKey.equals(identity.publicKey)
  }

  override fun hashCode(): Int {
    return Objects.hash(publicKey)
  }

  override fun toString(): String {
    return toCanonicalForm()
  }
}
