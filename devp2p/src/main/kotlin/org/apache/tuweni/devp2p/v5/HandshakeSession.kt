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
package org.apache.tuweni.devp2p.v5

import io.vertx.core.net.SocketAddress
import kotlinx.coroutines.CoroutineScope
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.CompletableAsyncResult
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.encrypt.AES128GCM
import org.apache.tuweni.devp2p.v5.encrypt.SessionKey
import org.apache.tuweni.devp2p.v5.encrypt.SessionKeyGenerator
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.units.bigints.UInt256
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

private val DISCOVERY_ID_NONCE: Bytes = Bytes.wrap("discovery-id-nonce".toByteArray())

internal class HandshakeSession(
  private val keyPair: SECP256K1.KeyPair,
  private val address: SocketAddress,
  private var peerPublicKey: SECP256K1.PublicKey? = null,
  private val sendFn: (SocketAddress, Bytes) -> Unit,
  private val enr: () -> EthereumNodeRecord,
  override val coroutineContext: CoroutineContext,
) : CoroutineScope {

  var requestId: Bytes? = null
  private val connected: CompletableAsyncResult<SessionKey> = AsyncResult.incomplete()
  private var initialNonce: Bytes? = null
  private var whoAreYouPacketBytes: Bytes? = null
  private var idNonce: Bytes? = null
  var receivedEnr: EthereumNodeRecord? = null
  val nodeId = EthereumNodeRecord.nodeId(keyPair.publicKey())

  companion object {
    private val logger = LoggerFactory.getLogger(HandshakeSession::class.java)
  }

  suspend fun connect(): AsyncResult<SessionKey> {
    val message = RandomMessage()
    val packet = OrdinaryPacket(nodeId, message.toRLP())
    initialNonce = packet.nonce
    logger.trace("Sending initial packet {} {}", address, packet)
    sendFn(address, packet.toBytes())
    return connected
  }

  suspend fun processMessage(messageBytes: Bytes) {
    if (messageBytes.size() > Message.MAX_UDP_MESSAGE_SIZE) {
      logger.trace("Message too long, dropping from {}", address)
      return
    }
    if (messageBytes.size() < 32) {
      logger.trace("Message too short, dropping from {}", address)
    }

    logger.trace("Received message from {}", address)
    val discv5Protocol = messageBytes.slice(16, 6) == Bytes.wrap("discv5".toByteArray())
    if (!discv5Protocol) {
      logger.trace("Message is not a discv5 protocol message, dropping from {}", address)
      return
    }
    val version = messageBytes.slice(22, 2) == Bytes.fromHexString("0x0001")
    if (!version) {
      logger.trace("Message does not use the same protocol version, dropping from {}", address)
      return
    }
    val flag = messageBytes.slice(24, 1)
    val nonce = messageBytes.slice(25, 12)
    val authdataSizeBytes = messageBytes.slice(37, 2)
    val authdataSize = authdataSizeBytes.get(0).toInt().shl(8) + authdataSizeBytes.get(1).toInt()
    val authdata = messageBytes.slice(39, authdataSize)
    val message = messageBytes.slice(39 + authdataSize)

    // it's a WHOAREYOU message
    if (flag.get(0) == 1.toByte()) {
      logger.trace("Identified a WHOAREYOU message")
      if (initialNonce == null) {
        logger.trace(
          "WHOAREYOU packet unexpected, dropping from {}",
          address
        )
        return
      }
      if (nonce != initialNonce) {
        logger.trace(
          "WHOAREYOU packet doesn't match our request nonce, dropping from {}",
          address
        )
        return
      }
      if (message.size() > 0) {
        logger.trace(
          "WHOAREYOU packet with {} bytes in message, should be 0, dropping from {}",
          message.size(),
          address
        )
        return
      }
      if (authdataSize != 24) {
        logger.trace(
          "WHOAREYOU packet with {} bytes in authdata, should be 24, dropping from {}",
          authdataSize,
          address
        )
        return
      }
      val idNonce = authdata.slice(0, 16)

      // Use the WHOAREYOU info to send handshake.
      // Generate ephemeral key pair
      val ephemeralKeyPair = SECP256K1.KeyPair.random()
      val ephemeralKey = ephemeralKeyPair.secretKey()
      val challengeData = Bytes.wrap(messageBytes.slice(0, 39), authdata)
      val destNodeId = EthereumNodeRecord.nodeId(peerPublicKey!!)

      val idSignatureContents = Bytes.concatenate(
        Bytes.wrap("discovery v5 identity proof".toByteArray()),
        challengeData,
        Bytes.wrap(ephemeralKeyPair.publicKey().asEcPoint().getEncoded(true)),
        destNodeId
      )
      val idSignature = SECP256K1.signHashed(Hash.sha2_256(idSignatureContents), keyPair)

      val secret = SECP256K1.deriveECDHKeyAgreement(ephemeralKey.bytes(), peerPublicKey!!.bytes())
      // Derive keys
      val newSession = SessionKeyGenerator.generate(nodeId, destNodeId, secret, idNonce)
      val signValue = Bytes.concatenate(DISCOVERY_ID_NONCE, idNonce, ephemeralKeyPair.publicKey().bytes())
      val signature = SECP256K1.signHashed(Hash.sha2_256(signValue), keyPair)
      val plain = RLP.encodeList { writer ->
        writer.writeInt(5)
        writer.writeValue(
          Bytes.concatenate(
            UInt256.valueOf(signature.r()).toBytes(),
            UInt256.valueOf(signature.s()).toBytes()
          )
        )
        writer.writeRLP(enr().toRLP())
      }
      val zeroNonce = Bytes.wrap(ByteArray(12))
      val authResponse = AES128GCM.encrypt(newSession.authRespKey, zeroNonce, plain, Bytes.EMPTY)
      println(authResponse)
      val authTag = Message.authTag()
      val findNode = FindNodeMessage()
      requestId = findNode.requestId
      val encryptedMessage = AES128GCM.encrypt(
        newSession.initiatorKey,
        authTag,
        Bytes.concatenate(Bytes.of(MessageType.FINDNODE.byte()), findNode.toRLP()),
        Bytes.EMPTY
      )

      val packet = HandshakeMessagePacket(
        nodeId, ephemeralKeyPair.publicKey(),
        Bytes.concatenate(
          UInt256.valueOf(idSignature.r()).toBytes(),
          UInt256.valueOf(idSignature.s()).toBytes()
        ),
        enr(), encryptedMessage
      )
      logger.trace("Sending handshake FindNode {}", packet)
      connected.complete(newSession)
      sendFn(address, packet.toBytes())
    } else if (flag.get(0) == 2.toByte()) { // it's a handshake response
      if (whoAreYouPacketBytes == null) {
        logger.trace("Unexpected handshake message!")
        return
      }
      val srcId = authdata.slice(0, 32)
      val signatureSize = authdata.get(32).toInt()
      val keySize = authdata.get(33).toInt()
      val signature = authdata.slice(34, signatureSize)
      val ephPubKeyCompressed = authdata.slice(34 + signatureSize, keySize)
      val enrBytes = authdata.slice(34 + signatureSize + keySize)
      val ecPoint = SECP256K1.Parameters.CURVE.getCurve().decodePoint(ephPubKeyCompressed.toArrayUnsafe())
      val ephPubKey = SECP256K1.PublicKey.fromBytes(Bytes.wrap(ecPoint.getEncoded(false)).slice(1))

      if (!verifySignature(signature, ephPubKey, peerPublicKey!!)) {
        logger.trace("Signature doesn't match the whoareyou packet challenge")
        return
      }
      receivedEnr = EthereumNodeRecord.fromRLP(enrBytes)

      val secret = SECP256K1.deriveECDHKeyAgreement(keyPair.secretKey().bytes(), ephPubKey.bytes())
      val newSession = SessionKeyGenerator.generate(srcId, nodeId, secret, idNonce!!)
      connected.complete(newSession)
    } else {
      logger.trace("Identified a message from an unknown party")
      // Build a WHOAREYOU message with the nonce of the random message.
      val response = WhoAreYouPacket(nonce, enr().seq())
      idNonce = response.idNonce
      whoAreYouPacketBytes = response.toBytes()
      logger.trace("Sending WHOAREYOU to {}", address)
      sendFn(address, whoAreYouPacketBytes!!)
    }
  }

  private fun verifySignature(
    signatureBytes: Bytes,
    ephemeralPublicKey: SECP256K1.PublicKey,
    publicKey: SECP256K1.PublicKey,
  ): Boolean {
    val signature = SECP256K1.Signature.create(
      1, signatureBytes.slice(0, 32).toUnsignedBigInteger(),
      signatureBytes.slice(32).toUnsignedBigInteger()
    )
    val wayBytes = whoAreYouPacketBytes!!
    val authdataSizeBytes = wayBytes.slice(37, 2)

    val authdataSize = authdataSizeBytes.get(0).toInt().shl(8) + authdataSizeBytes.get(1).toInt()
    val authdata = wayBytes.slice(39, authdataSize)

    val challengeData = Bytes.wrap(wayBytes.slice(0, 39), authdata)

    val idSignatureContents = Bytes.concatenate(
      Bytes.wrap("discovery v5 identity proof".toByteArray()),
      challengeData,
      Bytes.wrap(ephemeralPublicKey.asEcPoint().getEncoded(true)),
      nodeId
    )
    val hashedSignValue = Hash.sha2_256(idSignatureContents)
    if (!SECP256K1.verifyHashed(hashedSignValue, signature, publicKey)) {
      val signature0 = SECP256K1.Signature.create(
        0, signatureBytes.slice(0, 32).toUnsignedBigInteger(),
        signatureBytes.slice(32).toUnsignedBigInteger()
      )
      return SECP256K1.verifyHashed(hashedSignValue, signature0, publicKey)
    } else {
      return true
    }
  }

  fun awaitConnection(): AsyncResult<SessionKey> = connected
}
