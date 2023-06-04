// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.crypto.SECP256K1.KeyPair;
import org.apache.tuweni.crypto.SECP256K1.PublicKey;
import org.apache.tuweni.crypto.SECP256K1.SecretKey;
import org.apache.tuweni.crypto.SECP256K1.Signature;
import org.apache.tuweni.rlp.RLP;

/** The initial message sent during a RLPx handshake. */
final class InitiatorHandshakeMessage implements HandshakeMessage {

  static final int VERSION = 4;

  private final PublicKey publicKey;
  private final Signature signature;
  private final PublicKey ephemeralPublicKey;
  private final Bytes32 nonce;

  private InitiatorHandshakeMessage(
      PublicKey publicKey, Signature signature, PublicKey ephemeralPublicKey, Bytes32 nonce) {
    this.publicKey = publicKey;
    this.signature = signature;
    this.ephemeralPublicKey = ephemeralPublicKey;
    this.nonce = nonce;
  }

  static InitiatorHandshakeMessage create(
      PublicKey ourPubKey, KeyPair ephemeralKeyPair, Bytes32 staticSharedSecret, Bytes32 nonce) {
    Bytes32 toSign = staticSharedSecret.xor(nonce);
    return new InitiatorHandshakeMessage(
        ourPubKey,
        SECP256K1.signHashed(toSign, ephemeralKeyPair),
        ephemeralKeyPair.publicKey(),
        nonce);
  }

  static InitiatorHandshakeMessage decode(Bytes payload, SecretKey privateKey) {
    return RLP.decodeList(
        payload,
        reader -> {
          Signature signature = Signature.fromBytes(reader.readValue());
          PublicKey pubKey = PublicKey.fromBytes(reader.readValue());
          Bytes32 nonce = Bytes32.wrap(reader.readValue());
          Bytes32 staticSharedSecret = SECP256K1.calculateKeyAgreement(privateKey, pubKey);
          Bytes32 toSign = staticSharedSecret.xor(nonce);
          PublicKey ephemeralPublicKey = PublicKey.recoverFromHashAndSignature(toSign, signature);
          return new InitiatorHandshakeMessage(pubKey, signature, ephemeralPublicKey, nonce);
        });
  }

  Bytes encode() {
    return RLP.encodeList(
        writer -> {
          writer.writeValue(signature.bytes());
          writer.writeValue(publicKey.bytes());
          writer.writeValue(nonce);
          writer.writeInt(VERSION);
        });
  }

  PublicKey publicKey() {
    return publicKey;
  }

  @Override
  public PublicKey ephemeralPublicKey() {
    return ephemeralPublicKey;
  }

  @Override
  public Bytes32 nonce() {
    return nonce;
  }
}
