// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.crypto.sodium.Signature
import java.util.Objects

internal class SECP256K1KeyPairIdentity(private val keyPair: SECP256K1.KeyPair) : Identity {
  override fun sign(message: Bytes): Bytes {
    return SECP256K1.sign(message, keyPair).bytes()
  }

  override fun verify(signature: Bytes, message: Bytes): Boolean {
    return SECP256K1.verify(message, SECP256K1.Signature.fromBytes(signature), keyPair.publicKey())
  }

  override fun publicKeyAsBase64String(): String {
    return keyPair.publicKey().bytes().toBase64String()
  }

  override fun curve(): Identity.Curve {
    return Identity.Curve.SECP256K1
  }

  override fun ed25519PublicKey(): Signature.PublicKey? {
    throw UnsupportedOperationException()
  }

  override fun secp256k1PublicKey(): SECP256K1.PublicKey {
    return keyPair.publicKey()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val identity = other as SECP256K1KeyPairIdentity
    return keyPair == identity.keyPair
  }

  override fun hashCode(): Int {
    return Objects.hash(keyPair)
  }

  override fun toString(): String {
    return toCanonicalForm()
  }
}
