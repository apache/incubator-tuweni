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
