// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.mikuli;

import java.util.List;

/**
 * This class represents a signature and a public key
 */
public final class SignatureAndPublicKey {

  /**
   * Aggregates list of Signature and PublicKey pairs
   *
   * @param sigAndPubKeys The list of Signatures and corresponding Public keys to aggregate, not null
   * @return SignatureAndPublicKey, not null
   * @throws IllegalArgumentException if parameter list is empty
   */
  public static SignatureAndPublicKey aggregate(List<SignatureAndPublicKey> sigAndPubKeys) {
    if (sigAndPubKeys.isEmpty()) {
      throw new IllegalArgumentException("Parameter list is empty");
    }
    return sigAndPubKeys.stream().reduce((a, b) -> a.combine(b)).get();
  }

  private final Signature signature;
  private final PublicKey publicKey;

  SignatureAndPublicKey(Signature signature, PublicKey pubKey) {
    this.signature = signature;
    this.publicKey = pubKey;
  }

  /**
   * Provides the public key.
   * 
   * @return the public key of the pair
   */
  public PublicKey publicKey() {
    return publicKey;
  }

  /**
   * Provides the signature.
   * 
   * @return the signature of the pair
   */
  public Signature signature() {
    return signature;
  }

  /**
   * Combine the signature and public key provided to form a new signature and public key pair
   *
   * @param sigAndPubKey the signature and public key pair
   * @return a new signature and public key pair as a combination of both elements.
   */
  public SignatureAndPublicKey combine(SignatureAndPublicKey sigAndPubKey) {
    Signature newSignature = signature.combine(sigAndPubKey.signature);
    PublicKey newPubKey = publicKey.combine(sigAndPubKey.publicKey);
    return new SignatureAndPublicKey(newSignature, newPubKey);
  }
}
