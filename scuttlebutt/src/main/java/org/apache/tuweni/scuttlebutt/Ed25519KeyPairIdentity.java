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
package org.apache.tuweni.scuttlebutt;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.crypto.sodium.Signature;

import java.util.Objects;

/**
 * A complete Scuttlebutt identity backed by a Ed25519 key pair.
 *
 */
final class Ed25519KeyPairIdentity implements Identity {

  private final Signature.KeyPair keyPair;

  public Ed25519KeyPairIdentity(Signature.KeyPair keyPair) {
    this.keyPair = keyPair;
  }

  @Override
  public Bytes sign(Bytes message) {
    return Signature.signDetached(message, keyPair.secretKey());
  }

  @Override
  public boolean verify(Bytes signature, Bytes message) {
    return Signature.verifyDetached(message, signature, keyPair.publicKey());
  }

  @Override
  public String publicKeyAsBase64String() {
    return keyPair.publicKey().bytes().toBase64String();
  }

  @Override
  public Curve curve() {
    return Curve.Ed25519;
  }

  @Override
  public Signature.PublicKey ed25519PublicKey() {
    return keyPair.publicKey();
  }

  @Override
  public SECP256K1.PublicKey secp256k1PublicKey() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Ed25519KeyPairIdentity identity = (Ed25519KeyPairIdentity) o;
    return keyPair.equals(identity.keyPair);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keyPair);
  }

  @Override
  public String toString() {
    return toCanonicalForm();
  }
}
