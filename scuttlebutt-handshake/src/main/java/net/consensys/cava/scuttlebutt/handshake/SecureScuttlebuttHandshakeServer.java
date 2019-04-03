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
package org.apache.tuweni.scuttlebutt.handshake;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.sodium.Allocated;
import org.apache.tuweni.crypto.sodium.Box;
import org.apache.tuweni.crypto.sodium.Concatenate;
import org.apache.tuweni.crypto.sodium.DiffieHelman;
import org.apache.tuweni.crypto.sodium.HMACSHA512256;
import org.apache.tuweni.crypto.sodium.SHA256Hash;
import org.apache.tuweni.crypto.sodium.SecretBox;
import org.apache.tuweni.crypto.sodium.Signature;

/**
 * Class responsible for performing a Secure Scuttlebutt handshake with a remote peer, as defined in the
 * <a href="https://ssbc.github.io/scuttlebutt-protocol-guide/">Secure Scuttlebutt protocol guide</a>
 * <p>
 * Please note that only handshakes over the Ed25519 curve are supported.
 * <p>
 * This class manages the state of one handshake. It should not be reused across handshakes.
 *
 * If the handshake fails, a HandshakeException will be thrown.
 */
public final class SecureScuttlebuttHandshakeServer {

  private final Signature.KeyPair longTermKeyPair;
  private final Box.KeyPair ephemeralKeyPair;
  private final HMACSHA512256.Key networkIdentifier;
  private Box.PublicKey clientEphemeralPublicKey;
  private DiffieHelman.Secret sharedSecret;
  private DiffieHelman.Secret sharedSecret2;
  private Signature.PublicKey clientLongTermPublicKey;
  private DiffieHelman.Secret sharedSecret3;
  private Allocated detachedSignature;


  /**
   * Creates a new handshake server able to reply to the request of one client
   *
   * @param ourKeyPair the server long term key pair
   * @param networkIdentifier the network identifier
   * @return the handshake server
   */
  public static SecureScuttlebuttHandshakeServer create(Signature.KeyPair ourKeyPair, Bytes32 networkIdentifier) {
    return new SecureScuttlebuttHandshakeServer(ourKeyPair, networkIdentifier);
  }

  private SecureScuttlebuttHandshakeServer(Signature.KeyPair longTermKeyPair, Bytes32 networkIdentifier) {
    this.longTermKeyPair = longTermKeyPair;
    this.ephemeralKeyPair = Box.KeyPair.random();
    this.networkIdentifier = HMACSHA512256.Key.fromBytes(networkIdentifier);
  }

  /**
   * Creates a hello message to be sent to the other party, comprised of our ephemeral public key and an authenticator
   * against our network identifier.
   * 
   * @return the message
   */
  public Bytes createHello() {
    Bytes hmac = HMACSHA512256.authenticate(ephemeralKeyPair.publicKey().bytes(), networkIdentifier);
    return Bytes.concatenate(hmac, ephemeralKeyPair.publicKey().bytes());
  }

  /**
   * Validates the initial message's MAC with our network identifier, and returns the peer ephemeral public key.
   *
   * @param message initial handshake message
   */
  public void readHello(Bytes message) {
    if (message.size() != 64) {
      throw new HandshakeException("Invalid handshake message length: " + message.size());
    }
    Bytes hmac = message.slice(0, 32);
    Bytes key = message.slice(32, 32);
    if (!HMACSHA512256.verify(hmac, key, networkIdentifier)) {
      throw new HandshakeException("MAC does not match our network identifier");
    }
    this.clientEphemeralPublicKey = Box.PublicKey.fromBytes(key);
    computeSharedSecrets();
  }

  void computeSharedSecrets() {
    sharedSecret = DiffieHelman.Secret.forKeys(
        DiffieHelman.SecretKey.forBoxSecretKey(ephemeralKeyPair.secretKey()),
        DiffieHelman.PublicKey.forBoxPublicKey(clientEphemeralPublicKey));
    sharedSecret2 = DiffieHelman.Secret.forKeys(
        DiffieHelman.SecretKey.forSignatureSecretKey(longTermKeyPair.secretKey()),
        DiffieHelman.PublicKey.forBoxPublicKey(clientEphemeralPublicKey));
  }

  DiffieHelman.Secret sharedSecret() {
    return sharedSecret;
  }

  DiffieHelman.Secret sharedSecret2() {
    return sharedSecret2;
  }

  DiffieHelman.Secret sharedSecret3() {
    return sharedSecret3;
  }

  Signature.PublicKey clientLongTermPublicKey() {
    return clientLongTermPublicKey;
  }

  /**
   * Reads the message containing the identity of the client, verifying it matches our shared secrets.
   *
   * @param message the message containing the identity of the client
   */
  public void readIdentityMessage(Bytes message) {
    Bytes plaintext = SecretBox.decrypt(
        message,
        SecretBox.Key.fromHash(
            SHA256Hash.hash(
                SHA256Hash.Input.fromPointer(
                    new Concatenate().add(networkIdentifier).add(sharedSecret).add(sharedSecret2).concatenate()))),
        SecretBox.Nonce.fromBytes(new byte[24]));

    if (plaintext == null) {
      throw new HandshakeException("Could not decrypt the plaintext with our shared secrets");
    }

    if (plaintext.size() != 96) {
      throw new HandshakeException("Identity message should be 96 bytes long, was " + plaintext.size());
    }

    detachedSignature = Allocated.fromBytes(plaintext.slice(0, 64));
    clientLongTermPublicKey = Signature.PublicKey.fromBytes(plaintext.slice(64, 32));

    boolean verified = clientLongTermPublicKey.verify(
        new Concatenate()
            .add(networkIdentifier)
            .add(longTermKeyPair.publicKey())
            .add(SHA256Hash.hash(SHA256Hash.Input.fromSecret(sharedSecret)))
            .concatenate(),
        detachedSignature);
    if (!verified) {
      throw new HandshakeException("Identity message signature does not match");
    }
    sharedSecret3 = DiffieHelman.Secret.forKeys(
        DiffieHelman.SecretKey.forBoxSecretKey(ephemeralKeyPair.secretKey()),
        DiffieHelman.PublicKey.forSignaturePublicKey(clientLongTermPublicKey));
  }

  /**
   * Produces a message to accept the handshake with the client
   *
   * @return a message to accept the handshake
   */
  public Bytes createAcceptMessage() {
    Allocated signature = Signature.signDetached(
        new Concatenate()
            .add(networkIdentifier)
            .add(detachedSignature)
            .add(clientLongTermPublicKey)
            .add(SHA256Hash.hash(SHA256Hash.Input.fromSecret(sharedSecret)))
            .concatenate(),
        longTermKeyPair.secretKey());

    return SecretBox
        .encrypt(
            signature,
            SecretBox.Key.fromHash(
                SHA256Hash.hash(
                    SHA256Hash.Input.fromPointer(
                        new Concatenate()
                            .add(networkIdentifier)
                            .add(sharedSecret)
                            .add(sharedSecret2)
                            .add(sharedSecret3)
                            .concatenate()))),
            SecretBox.Nonce.fromBytes(new byte[24]))
        .bytes();
  }

  /**
   * If the handshake completed successfully, this provides the secret box key to use to send messages to the server
   * going forward.
   *
   * @return a new secret box key for use with encrypting messages to the server.
   */
  SHA256Hash.Hash clientToServerSecretBoxKey() {
    return SHA256Hash.hash(
        SHA256Hash.Input.fromPointer(
            new Concatenate()
                .add(
                    SHA256Hash.hash(
                        SHA256Hash.Input.fromHash(
                            SHA256Hash.hash(
                                SHA256Hash.Input.fromPointer(
                                    new Concatenate()
                                        .add(networkIdentifier)
                                        .add(sharedSecret)
                                        .add(sharedSecret2)
                                        .add(sharedSecret3)
                                        .concatenate())))))
                .add(longTermKeyPair.publicKey())
                .concatenate()));
  }

  /**
   * If the handshake completed successfully, this provides the clientToServerNonce to use to send messages to the
   * server going forward.
   *
   * @return a clientToServerNonce for use with encrypting messages to the server.
   */
  Bytes clientToServerNonce() {
    return HMACSHA512256.authenticate(ephemeralKeyPair.publicKey().bytes(), networkIdentifier).slice(0, 24);
  }

  /**
   * If the handshake completed successfully, this provides the secret box key to use to receive messages from the
   * server going forward.
   *
   * @return a new secret box key for use with decrypting messages from the server.
   */
  SHA256Hash.Hash serverToClientSecretBoxKey() {
    return SHA256Hash.hash(
        SHA256Hash.Input.fromPointer(
            new Concatenate()
                .add(
                    SHA256Hash.hash(
                        SHA256Hash.Input.fromHash(
                            SHA256Hash.hash(
                                SHA256Hash.Input.fromPointer(
                                    new Concatenate()
                                        .add(networkIdentifier)
                                        .add(sharedSecret)
                                        .add(sharedSecret2)
                                        .add(sharedSecret3)
                                        .concatenate())))))
                .add(clientLongTermPublicKey)
                .concatenate()));
  }

  /**
   * If the handshake completed successfully, this provides the clientToServerNonce to use to receive messages from the
   * server going forward.
   *
   * @return a clientToServerNonce for use with decrypting messages from the server.
   */
  Bytes serverToClientNonce() {
    return HMACSHA512256.authenticate(clientEphemeralPublicKey.bytes(), networkIdentifier).slice(0, 24);
  }

  /**
   * Creates a stream to allow communication with the other peer after the handshake has completed
   * 
   * @return a new stream for encrypted communications with the peer
   */
  public SecureScuttlebuttStreamServer createStream() {
    return new SecureScuttlebuttStream(
        clientToServerSecretBoxKey(),
        clientToServerNonce(),
        serverToClientSecretBoxKey(),
        serverToClientNonce());
  }
}
