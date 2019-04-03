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
package org.apache.tuweni.rlpx;

import static org.apache.tuweni.bytes.Bytes.concatenate;
import static org.apache.tuweni.crypto.Hash.keccak256;
import static org.apache.tuweni.crypto.SECP256K1.Parameters.CURVE;
import static org.apache.tuweni.crypto.SECP256K1.calculateKeyAgreement;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.crypto.SECP256K1.KeyPair;
import org.apache.tuweni.crypto.SECP256K1.PublicKey;
import org.apache.tuweni.crypto.SECP256K1.SecretKey;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bouncycastle.crypto.BasicAgreement;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.IESWithCipherParameters;
import org.bouncycastle.crypto.params.KDFParameters;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.BigIntegers;

/**
 * Factory creating RLPxConnection, either from initiating a handshake or responding to a handshake request.
 */
public final class RLPxConnectionFactory {

  private static final SecureRandom random = new SecureRandom();

  /**
   * Creates a complete interaction to run a handshake with a remote peer.
   * 
   * @param keyPair our key pair
   * @param remotePublicKey the peer public key
   * @param initAndResponse a function giving us the peer response, and allowing us to respond to them to finalize the
   *        handshake
   * @return a future RLPxConnection created as the result of the handshake
   */
  public static AsyncResult<RLPxConnection> createHandshake(
      KeyPair keyPair,
      PublicKey remotePublicKey,
      Function<Bytes, AsyncResult<Bytes>> initAndResponse) {

    Bytes32 nonce = generateRandomBytes32();
    KeyPair ephemeralKeyPair = KeyPair.random();
    Bytes initHandshakeMessage = init(keyPair, remotePublicKey, ephemeralKeyPair, nonce);
    AsyncResult<Bytes> response = initAndResponse.apply(initHandshakeMessage);

    return response.thenApply(responseBytes -> {
      HandshakeMessage responseMessage = readResponse(responseBytes, keyPair.secretKey());
      return createConnection(
          true,
          initHandshakeMessage,
          responseBytes,
          ephemeralKeyPair.secretKey(),
          responseMessage.ephemeralPublicKey(),
          nonce,
          responseMessage.nonce(),
          keyPair.publicKey(),
          remotePublicKey);
    });
  }

  /**
   * Creates a RLPxConnection in response to a handshake initiation message.
   *
   * @param initiatorMessageBytes the initiation message raw bytes
   * @param keyPair our key pair
   * @param responseHandler a function to respond back to the peer that we acknowledged the connection
   * @return a valid RLPxConnection
   */
  public static RLPxConnection respondToHandshake(
      Bytes initiatorMessageBytes,
      KeyPair keyPair,
      Consumer<Bytes> responseHandler) {
    InitiatorHandshakeMessage initiatorHandshakeMessage = read(initiatorMessageBytes, keyPair.secretKey());
    Bytes32 nonce = Bytes32.wrap(new byte[32]);
    random.nextBytes(nonce.toArrayUnsafe());
    KeyPair ephemeralKeyPair = KeyPair.random();

    PublicKey initiatorPublicKey = initiatorHandshakeMessage.publicKey();

    ResponderHandshakeMessage responderMessage = ResponderHandshakeMessage.create(ephemeralKeyPair.publicKey(), nonce);
    Bytes responseBytes = encryptMessage(responderMessage.encode(), initiatorPublicKey);
    responseHandler.accept(responseBytes);

    return createConnection(
        false,
        initiatorMessageBytes,
        responseBytes,
        ephemeralKeyPair.secretKey(),
        initiatorHandshakeMessage.ephemeralPublicKey(),
        initiatorHandshakeMessage.nonce(),
        nonce,
        keyPair.publicKey(),
        initiatorPublicKey);
  }

  /**
   * Creates a handshake initiation message using ephemeral keys and a random nonce.
   * 
   * @param keyPair our key pair
   * @param remotePublicKey the peer public key
   * @param ephemeralKeyPair our ephemeral key pair for this connection
   * @param initiatorNonce our random nonce
   * @return the bytes of a handshake initiation message for a given peer
   */
  public static Bytes init(
      KeyPair keyPair,
      PublicKey remotePublicKey,
      KeyPair ephemeralKeyPair,
      Bytes32 initiatorNonce) {
    Bytes32 sharedSecret = calculateKeyAgreement(keyPair.secretKey(), remotePublicKey);
    InitiatorHandshakeMessage message =
        InitiatorHandshakeMessage.create(keyPair.publicKey(), ephemeralKeyPair, sharedSecret, initiatorNonce);
    return encryptMessage(message.encode(), remotePublicKey);
  }

  /**
   * Decrypts the handshake response using our private key.
   * 
   * @param response the raw response bytes
   * @param privateKey our private key
   * @return a decrypted handshake response message
   */
  public static HandshakeMessage readResponse(Bytes response, SecretKey privateKey) {
    return ResponderHandshakeMessage.decode(decryptMessage(response, privateKey));
  }

  /**
   * Generates a new random 32 byte array.
   *
   * @return a new Bytes32 object filled with random bytes
   */
  public static Bytes32 generateRandomBytes32() {
    Bytes32 nonce = Bytes32.wrap(new byte[32]);
    random.nextBytes(nonce.toArrayUnsafe());
    return nonce;
  }

  /**
   * Creates a RLPxConnection based off the complete handshake exchange.
   *
   * @param initiator whether we initiated the handshake
   * @param initiatorMessage the bytes of the initiation message
   * @param responderMessage the bytes of the response message
   * @param ourEphemeralPrivateKey our ephemeral private key
   * @param peerEphemeralPublicKey the peer ephemeral public key
   * @param initiatorNonce the initiation random nonce
   * @param responderNonce the responder random nonce
   * @param ourPublicKey our public key
   * @param peerPublicKey the public key of the peer
   * @return a valid RPLx connection to communicate between peers
   */
  public static RLPxConnection createConnection(
      boolean initiator,
      Bytes initiatorMessage,
      Bytes responderMessage,
      SecretKey ourEphemeralPrivateKey,
      PublicKey peerEphemeralPublicKey,
      Bytes32 initiatorNonce,
      Bytes32 responderNonce,
      PublicKey ourPublicKey,
      PublicKey peerPublicKey) {

    Bytes agreedSecret = calculateKeyAgreement(ourEphemeralPrivateKey, peerEphemeralPublicKey);
    Bytes sharedSecret = keccak256(concatenate(agreedSecret, keccak256(concatenate(responderNonce, initiatorNonce))));

    Bytes32 aesSecret = keccak256(concatenate(agreedSecret, sharedSecret));
    Bytes32 macSecret = keccak256(concatenate(agreedSecret, aesSecret));
    Bytes32 token = keccak256(sharedSecret);

    Bytes initiatorMac = concatenate(macSecret.xor(responderNonce), initiatorMessage);
    Bytes responderMac = concatenate(macSecret.xor(initiatorNonce), responderMessage);

    if (initiator) {
      return new RLPxConnection(aesSecret, macSecret, token, initiatorMac, responderMac, ourPublicKey, peerPublicKey);
    } else {
      return new RLPxConnection(aesSecret, macSecret, token, responderMac, initiatorMac, ourPublicKey, peerPublicKey);
    }
  }

  static InitiatorHandshakeMessage read(Bytes payload, SecretKey privateKey) {
    return InitiatorHandshakeMessage.decode(decryptMessage(payload, privateKey), privateKey);
  }

  static Bytes encryptMessage(Bytes message, PublicKey remoteKey) {
    byte[] ivb = new byte[16];
    random.nextBytes(ivb);
    Bytes iv = Bytes.wrap(ivb);
    KeyPair ephemeralKeyPair = KeyPair.random();
    Bytes bytes = addPadding(message);
    int size = bytes.size() + 65 + 16 + 32;
    Bytes sizePrefix = Bytes.of((byte) (size >>> 8), (byte) size);
    EthereumIESEncryptionEngine engine = forEncryption(remoteKey, iv, sizePrefix, ephemeralKeyPair);
    byte[] encrypted;
    try {
      encrypted = engine.processBlock(bytes.toArrayUnsafe(), 0, bytes.size());
    } catch (InvalidCipherTextException e) {
      throw new IllegalArgumentException(e);
    }
    // Create the output message by concatenating the ephemeral public key (prefixed with
    // 0x04 to designate uncompressed), IV, and encrypted bytes.
    Bytes finalBytes = concatenate(
        Bytes.of(sizePrefix.get(0), sizePrefix.get(1), (byte) 0x04),
        ephemeralKeyPair.publicKey().bytes(),
        iv,
        Bytes.wrap(encrypted));
    return finalBytes;
  }

  private static EthereumIESEncryptionEngine forEncryption(
      PublicKey pubKey,
      Bytes iv,
      Bytes commonMac,
      KeyPair ephemeralKeyPair) {
    CipherParameters pubParam = new ECPublicKeyParameters(pubKey.asEcPoint(), CURVE);
    CipherParameters privParam =
        new ECPrivateKeyParameters(ephemeralKeyPair.secretKey().bytes().toUnsignedBigInteger(), CURVE);

    BasicAgreement agree = new ECDHBasicAgreement();
    agree.init(privParam);
    BigInteger z = agree.calculateAgreement(pubParam);
    byte[] zbytes = BigIntegers.asUnsignedByteArray(agree.getFieldSize(), z);

    IESWithCipherParameters iesWithCipherParameters = new IESWithCipherParameters(new byte[0], new byte[0], 128, 128);

    // Initialise the KDF.
    EthereumIESEncryptionEngine.ECIESHandshakeKDFFunction kdf =
        new EthereumIESEncryptionEngine.ECIESHandshakeKDFFunction(1, new SHA256Digest());
    kdf.init(new KDFParameters(zbytes, iesWithCipherParameters.getDerivationV()));
    EthereumIESEncryptionEngine engine = new EthereumIESEncryptionEngine(
        agree,
        kdf,
        new HMac(new SHA256Digest()),
        commonMac.toArrayUnsafe(),
        new BufferedBlockCipher(new SICBlockCipher(new AESEngine())));
    ParametersWithIV cipherParameters = new ParametersWithIV(iesWithCipherParameters, iv.toArrayUnsafe());
    engine.init(true, privParam, pubParam, cipherParameters);

    return engine;
  }

  /**
   * Identify the size of a handshake message based on elements of the common MAC.
   *
   * @param msgBytes the bytes of the message
   * @return the size of the message, including MAC, key and IV
   */
  public static int messageSize(Bytes msgBytes) {
    Bytes commonMac = msgBytes.slice(0, 2);
    int size = (commonMac.get(1) & 0xFF) + ((commonMac.get(0) & 0xFF) << 8);
    return size + 2;
  }

  static Bytes decryptMessage(Bytes msgBytes, SecretKey ourKey) {
    Bytes commonMac = msgBytes.slice(0, 2);
    int size = (commonMac.get(1) & 0xFF) + ((commonMac.get(0) & 0xFF) << 8);
    PublicKey ephemeralPublicKey = PublicKey.fromBytes(msgBytes.slice(3, 64));
    Bytes iv = msgBytes.slice(67, 16);
    Bytes encrypted = msgBytes.slice(83, size - 81);

    EthereumIESEncryptionEngine decryptor = forDecryption(ourKey, ephemeralPublicKey, iv, commonMac);
    byte[] result;
    try {
      result = decryptor.processBlock(encrypted.toArrayUnsafe(), 0, encrypted.size());
    } catch (InvalidCipherTextException e) {
      throw new InvalidMACException(e);
    }
    return Bytes.wrap(result);
  }

  private static Bytes addPadding(final Bytes message) {
    final int padding = 100 + random.nextInt(200);
    final byte[] paddingBytes = new byte[padding];
    random.nextBytes(paddingBytes);
    return concatenate(message, Bytes.wrap(paddingBytes));
  }

  private static EthereumIESEncryptionEngine forDecryption(
      SecretKey privateKey,
      PublicKey ephemeralPublicKey,
      Bytes iv,
      Bytes commonMac) {
    CipherParameters pubParam = new ECPublicKeyParameters(ephemeralPublicKey.asEcPoint(), CURVE);
    CipherParameters privParam = new ECPrivateKeyParameters(privateKey.bytes().toUnsignedBigInteger(), CURVE);

    BasicAgreement agreement = new ECDHBasicAgreement();
    agreement.init(privParam);
    byte[] agreementValue =
        BigIntegers.asUnsignedByteArray(agreement.getFieldSize(), agreement.calculateAgreement(pubParam));

    IESWithCipherParameters iesWithCipherParameters = new IESWithCipherParameters(new byte[0], new byte[0], 128, 128);

    EthereumIESEncryptionEngine.ECIESHandshakeKDFFunction kdf =
        new EthereumIESEncryptionEngine.ECIESHandshakeKDFFunction(1, new SHA256Digest());
    kdf.init(new KDFParameters(agreementValue, iesWithCipherParameters.getDerivationV()));
    EthereumIESEncryptionEngine engine = new EthereumIESEncryptionEngine(
        agreement,
        kdf,
        new HMac(new SHA256Digest()),
        commonMac.toArrayUnsafe(),
        new BufferedBlockCipher(new SICBlockCipher(new AESEngine())));
    ParametersWithIV cipherParameters = new ParametersWithIV(iesWithCipherParameters, iv.toArrayUnsafe());
    engine.init(false, privParam, pubParam, cipherParameters);
    return engine;
  }
}
