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
package org.apache.tuweni.scuttlebutt.handshake

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.sodium.Allocated
import org.apache.tuweni.crypto.sodium.Box
import org.apache.tuweni.crypto.sodium.Concatenate
import org.apache.tuweni.crypto.sodium.DiffieHelman
import org.apache.tuweni.crypto.sodium.HMACSHA512256
import org.apache.tuweni.crypto.sodium.SHA256Hash
import org.apache.tuweni.crypto.sodium.SecretBox
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.scuttlebutt.Identity
import org.apache.tuweni.scuttlebutt.Invite

/**
 * Class responsible for performing a Secure Scuttlebutt handshake with a remote peer, as defined in the
 * [Secure Scuttlebutt protocol guide](https://ssbc.github.io/scuttlebutt-protocol-guide/)
 *
 *
 * Please note that only handshakes over the Ed25519 curve are supported.
 *
 *
 * This class manages the state of one handshake. It should not be reused across handshakes.
 *
 * If the handshake fails, a HandshakeException will be thrown.
 */
class SecureScuttlebuttHandshakeClient private constructor(
  private val longTermKeyPair: Signature.KeyPair,
  networkIdentifier: Bytes32,
  serverLongTermPublicKey: Signature.PublicKey?
) {
  private val ephemeralKeyPair: Box.KeyPair
  private val networkIdentifier: HMACSHA512256.Key
  private val serverLongTermPublicKey: Signature.PublicKey?
  private var serverEphemeralPublicKey: Box.PublicKey? = null
  private var sharedSecret: DiffieHelman.Secret? = null
  private var sharedSecret2: DiffieHelman.Secret? = null
  private var sharedSecret3: DiffieHelman.Secret? = null
  private var detachedSignature: Allocated? = null

  init {
    ephemeralKeyPair = Box.KeyPair.random()
    this.networkIdentifier = HMACSHA512256.Key.fromBytes(networkIdentifier)
    this.serverLongTermPublicKey = serverLongTermPublicKey
  }

  /**
   * Creates a hello message to be sent to the other party, comprised of our ephemeral public key and an authenticator
   * against our network identifier.
   *
   * @return the hello message, ready to be sent to the remote server
   */
  fun createHello(): Bytes {
    val hmac = HMACSHA512256.authenticate(ephemeralKeyPair.publicKey().bytes(), networkIdentifier)
    return Bytes.concatenate(hmac, ephemeralKeyPair.publicKey().bytes())
  }

  /**
   * Validates the initial message's MAC with our network identifier, and returns the peer ephemeral public key.
   *
   * @param message initial handshake message
   */
  fun readHello(message: Bytes) {
    if (message.size() != 64) {
      throw HandshakeException("Invalid handshake message length: " + message.size())
    }
    val hmac = message.slice(0, 32)
    val key = message.slice(32, 32)
    if (!HMACSHA512256.verify(hmac, key, networkIdentifier)) {
      throw HandshakeException("MAC does not match our network identifier")
    }
    serverEphemeralPublicKey = Box.PublicKey.fromBytes(key)
    sharedSecret = DiffieHelman.Secret
      .forKeys(
        DiffieHelman.SecretKey.forBoxSecretKey(ephemeralKeyPair.secretKey()),
        DiffieHelman.PublicKey.forBoxPublicKey(serverEphemeralPublicKey!!)
      )
    sharedSecret2 = DiffieHelman.Secret
      .forKeys(
        DiffieHelman.SecretKey.forBoxSecretKey(ephemeralKeyPair.secretKey()),
        DiffieHelman.PublicKey.forSignaturePublicKey(serverLongTermPublicKey!!)
      )
    sharedSecret3 = DiffieHelman.Secret
      .forKeys(
        DiffieHelman.SecretKey.forSignatureSecretKey(longTermKeyPair.secretKey()),
        DiffieHelman.PublicKey.forBoxPublicKey(serverEphemeralPublicKey!!)
      )
  }

  fun sharedSecret(): DiffieHelman.Secret? {
    return sharedSecret
  }

  fun sharedSecret2(): DiffieHelman.Secret? {
    return sharedSecret2
  }

  fun sharedSecret3(): DiffieHelman.Secret? {
    return sharedSecret3
  }

  /**
   * Creates a message containing the identity of the client
   *
   * @return a message containing the identity of the client
   */
  fun createIdentityMessage(): Bytes {
    val concatenate = Concatenate()
    concatenate.add(networkIdentifier)
    concatenate.add(serverLongTermPublicKey!!)
    concatenate.add(SHA256Hash.hash(SHA256Hash.Input.fromSecret(sharedSecret!!)))
    detachedSignature = Signature.signDetached(concatenate.concatenate(), longTermKeyPair.secretKey())
    return SecretBox
      .encrypt(
        Concatenate().add(detachedSignature!!).add(longTermKeyPair.publicKey()).concatenate(),
        SecretBox.Key
          .fromHash(
            SHA256Hash
              .hash(
                SHA256Hash.Input
                  .fromPointer(
                    Concatenate()
                      .add(networkIdentifier)
                      .add(sharedSecret!!)
                      .add(sharedSecret2!!)
                      .concatenate()
                  )
              )
          ),
        SecretBox.Nonce.fromBytes(ByteArray(24))
      )
      .bytes()
  }

  /**
   * Reads the handshake acceptance message from the server
   *
   * @param message the message of acceptance of the handshake from the server
   */
  fun readAcceptMessage(message: Bytes?) {
    val serverSignature = SecretBox
      .decrypt(
        Allocated.fromBytes(message!!),
        SecretBox.Key
          .fromHash(
            SHA256Hash
              .hash(
                SHA256Hash.Input
                  .fromPointer(
                    Concatenate()
                      .add(networkIdentifier)
                      .add(sharedSecret!!)
                      .add(sharedSecret2!!)
                      .add(sharedSecret3!!)
                      .concatenate()
                  )
              )
          ),
        SecretBox.Nonce.fromBytes(ByteArray(24))
      )
      ?: throw HandshakeException("Could not decrypt accept message with our shared secrets")
    val verified = serverLongTermPublicKey!!
      .verify(
        Concatenate()
          .add(networkIdentifier)
          .add(detachedSignature!!)
          .add(longTermKeyPair.publicKey())
          .add(SHA256Hash.hash(SHA256Hash.Input.fromSecret(sharedSecret!!)))
          .concatenate(),
        serverSignature
      )
    if (!verified) {
      throw HandshakeException("Accept message signature does not match")
    }
  }

  /**
   * If the handshake completed successfully, this provides the secret box key to use to send messages to the server
   * going forward.
   *
   * @return a new secret box key for use with encrypting messages to the server.
   */
  fun clientToServerSecretBoxKey(): SHA256Hash.Hash {
    return SHA256Hash
      .hash(
        SHA256Hash.Input
          .fromPointer(
            Concatenate()
              .add(
                SHA256Hash
                  .hash(
                    SHA256Hash.Input
                      .fromHash(
                        SHA256Hash
                          .hash(
                            SHA256Hash.Input
                              .fromPointer(
                                Concatenate()
                                  .add(networkIdentifier)
                                  .add(sharedSecret!!)
                                  .add(sharedSecret2!!)
                                  .add(sharedSecret3!!)
                                  .concatenate()
                              )
                          )
                      )
                  )
              )
              .add(serverLongTermPublicKey!!)
              .concatenate()
          )
      )
  }

  /**
   * If the handshake completed successfully, this provides the clientToServerNonce to use to send messages to the
   * server going forward.
   *
   * @return a clientToServerNonce for use with encrypting messages to the server.
   */
  fun clientToServerNonce(): Bytes {
    return HMACSHA512256.authenticate(serverEphemeralPublicKey!!.bytes(), networkIdentifier).slice(0, 24)
  }

  /**
   * If the handshake completed successfully, this provides the secret box key to use to receive messages from the
   * server going forward.
   *
   * @return a new secret box key for use with decrypting messages from the server.
   */
  fun serverToClientSecretBoxKey(): SHA256Hash.Hash {
    return SHA256Hash
      .hash(
        SHA256Hash.Input
          .fromPointer(
            Concatenate()
              .add(
                SHA256Hash
                  .hash(
                    SHA256Hash.Input
                      .fromHash(
                        SHA256Hash
                          .hash(
                            SHA256Hash.Input
                              .fromPointer(
                                Concatenate()
                                  .add(networkIdentifier)
                                  .add(sharedSecret!!)
                                  .add(sharedSecret2!!)
                                  .add(sharedSecret3!!)
                                  .concatenate()
                              )
                          )
                      )
                  )
              )
              .add(longTermKeyPair.publicKey())
              .concatenate()
          )
      )
  }

  /**
   * If the handshake completed successfully, this provides the clientToServerNonce to use to receive messages from the
   * server going forward.
   *
   * @return a clientToServerNonce for use with decrypting messages from the server.
   */
  fun serverToClientNonce(): Bytes {
    return HMACSHA512256.authenticate(ephemeralKeyPair.publicKey().bytes(), networkIdentifier).slice(0, 24)
  }

  /**
   * Creates a stream to allow communication with the other peer after the handshake has completed
   *
   * @return a new stream for encrypted communications with the peer
   */
  fun createStream(): SecureScuttlebuttStreamClient {
    return SecureScuttlebuttStream(
      clientToServerSecretBoxKey(),
      clientToServerNonce(),
      serverToClientSecretBoxKey(),
      serverToClientNonce()
    )
  }

  companion object {
    /**
     * Creates a new handshake client to connect to a specific remote peer.
     *
     * @param ourKeyPair our long term key pair
     * @param networkIdentifier the network identifier
     * @param serverLongTermPublicKey the server long term public key
     * @return a new Secure Scuttlebutt handshake client
     */
    @JvmStatic
    fun create(
      ourKeyPair: Signature.KeyPair,
      networkIdentifier: Bytes32,
      serverLongTermPublicKey: Signature.PublicKey?
    ): SecureScuttlebuttHandshakeClient {
      return SecureScuttlebuttHandshakeClient(ourKeyPair, networkIdentifier, serverLongTermPublicKey)
    }

    /**
     * Create a new handshake client to connect to the server specified in the invite
     *
     * @param networkIdentifier the networkIdentifier
     * @param invite the invite
     * @return a new Secure Scuttlebutt handshake client
     */
    @JvmStatic
    fun fromInvite(networkIdentifier: Bytes32, invite: Invite): SecureScuttlebuttHandshakeClient {
      require(Identity.Curve.Ed25519 == invite.identity.curve()) { "Only ed25519 keys are supported" }
      return SecureScuttlebuttHandshakeClient(
        Signature.KeyPair.fromSeed(invite.seedKey),
        networkIdentifier,
        invite.identity.ed25519PublicKey()
      )
    }
  }
}
