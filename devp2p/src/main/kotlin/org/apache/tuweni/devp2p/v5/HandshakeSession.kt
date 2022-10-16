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
import org.apache.tuweni.rlp.RLPReader
import org.apache.tuweni.units.bigints.UInt256
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

private val DISCOVERY_ID_NONCE: Bytes = Bytes.wrap("discovery-id-nonce".toByteArray())

internal class HandshakeSession(
  private val keyPair: SECP256K1.KeyPair,
  private val address: SocketAddress,
  private var publicKey: SECP256K1.PublicKey? = null,
  private val sendFn: (SocketAddress, Bytes) -> Unit,
  private val enr: () -> EthereumNodeRecord,
  override val coroutineContext: CoroutineContext
) : CoroutineScope {

  var requestId: Bytes? = null
  private val connected: CompletableAsyncResult<SessionKey> = AsyncResult.incomplete()
  var receivedEnr: EthereumNodeRecord? = null
  val nodeId = EthereumNodeRecord.nodeId(keyPair.publicKey())
  private val whoAreYouHeader = Hash.sha2_256(Bytes.concatenate(nodeId, Bytes.wrap("WHOAREYOU".toByteArray())))

  private val tokens = ArrayList<Bytes>()

  companion object {
    private val logger = LoggerFactory.getLogger(HandshakeSession::class.java)
  }

  suspend fun connect(): AsyncResult<SessionKey> {
    val message = RandomMessage()
    tokens.add(message.authTag)
    val tag = tag()
    val rlpAuthTag = RLP.encodeValue(message.authTag)
    val content = Bytes.concatenate(tag, rlpAuthTag, message.toRLP())
    logger.trace("Sending random packet {} {}", address, content)
    sendFn(address, content)
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
    val tag = messageBytes.slice(0, 32)
    val content = messageBytes.slice(32)
    // it's either a WHOAREYOU or a RANDOM message.
    if (whoAreYouHeader == tag) {
      logger.trace("Identified a WHOAREYOU message")
      val message = WhoAreYouMessage.create(tag, content)
      if (!this.tokens.contains(message.token)) {
        // We were not expecting this WHOAREYOU.
        logger.trace("Unexpected WHOAREYOU packet {}", message.token)
        return
      }
      // Use the WHOAREYOU info to send handshake.
      // Generate ephemeral key pair
      val ephemeralKeyPair = SECP256K1.KeyPair.random()
      val ephemeralKey = ephemeralKeyPair.secretKey()

      val destNodeId = EthereumNodeRecord.nodeId(publicKey!!)
      val secret = SECP256K1.deriveECDHKeyAgreement(ephemeralKey.bytes(), publicKey!!.bytes())

      // Derive keys
      val newSession = SessionKeyGenerator.generate(nodeId, destNodeId, secret, message.idNonce)
      val signValue = Bytes.concatenate(DISCOVERY_ID_NONCE, message.idNonce, ephemeralKeyPair.publicKey().bytes())
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
      val authTag = Message.authTag()
      val newTag = tag()
      val findNode = FindNodeMessage()
      requestId = findNode.requestId
      val encryptedMessage = AES128GCM.encrypt(
        newSession.initiatorKey,
        authTag,
        Bytes.concatenate(Bytes.of(MessageType.FINDNODE.byte()), findNode.toRLP()),
        newTag
      )
      val response = Bytes.concatenate(
        newTag,
        RLP.encodeList {
          it.writeValue(authTag)
          it.writeValue(message.idNonce)
          it.writeValue(Bytes.wrap("gcm".toByteArray()))
          it.writeValue(ephemeralKeyPair.publicKey().bytes())
          it.writeValue(authResponse)
        },
        encryptedMessage
      )
      logger.trace("Sending handshake FindNode {}", response)
      connected.complete(newSession)
      sendFn(address, response)
    } else {
      // connection initiated by the peer.
      // try to see if this a message with a header we can read:
      val hasHeader = RLP.decode(content, RLPReader::nextIsList)
      if (hasHeader) {
        logger.trace("Identified a valid message")
        RLP.decodeList(content) {
          it.skipNext()
          val idNonce = it.readValue()
          it.skipNext()
          val ephemeralPublicKey = SECP256K1.PublicKey.fromBytes(it.readValue())
          val authResponse = it.readValue()

          val secret = SECP256K1.deriveECDHKeyAgreement(keyPair.secretKey().bytes(), ephemeralPublicKey.bytes())
          val senderNodeId = Message.getSourceFromTag(tag, nodeId)
          val sessionKey = SessionKeyGenerator.generate(senderNodeId, nodeId, secret, idNonce)
          val decryptedAuthResponse =
            Bytes.wrap(AES128GCM.decrypt(sessionKey.authRespKey, Bytes.wrap(ByteArray(12)), authResponse, Bytes.EMPTY))
          RLP.decodeList(decryptedAuthResponse) { reader ->
            reader.skipNext()
            val signatureBytes = reader.readValue()
            val enr = reader.readList { enrReader -> EthereumNodeRecord.fromRLP(enrReader) }
            receivedEnr = enr
            publicKey = enr.publicKey()
            val signatureVerified = verifySignature(signatureBytes, idNonce, ephemeralPublicKey, enr.publicKey())
            if (!signatureVerified) {
              throw IllegalArgumentException("Signature is not verified")
            }
            logger.trace("Finalized handshake")
            connected.complete(sessionKey)
          }
        }
      } else {
        logger.trace("Identified a RANDOM message")
        val token = RLP.decodeValue(content)
        val peerNodeId = Message.getSourceFromTag(tag, nodeId)
        logger.trace("Found peerNodeId $peerNodeId")
        // Build a WHOAREYOU message with the tag of the random message.
        val whoAreYouTag = Hash.sha2_256(Bytes.concatenate(peerNodeId, Bytes.wrap("WHOAREYOU".toByteArray())))
        val response = WhoAreYouMessage(whoAreYouTag, token, Message.idNonce(), enr().seq())
        this.tokens.add(token)
        logger.trace("Sending WHOAREYOU to {}", address)
        sendFn(address, response.toRLP())
      }
    }
  }

  private fun verifySignature(
    signatureBytes: Bytes,
    idNonce: Bytes,
    ephemeralPublicKey: SECP256K1.PublicKey,
    publicKey: SECP256K1.PublicKey
  ): Boolean {
    val signature = SECP256K1.Signature.create(
      1,
      signatureBytes.slice(0, 32).toUnsignedBigInteger(),
      signatureBytes.slice(32).toUnsignedBigInteger()
    )

    val signValue = Bytes.concatenate(DISCOVERY_ID_NONCE, idNonce, ephemeralPublicKey.bytes())
    val hashedSignValue = Hash.sha2_256(signValue)
    if (!SECP256K1.verifyHashed(hashedSignValue, signature, publicKey)) {
      val signature0 = SECP256K1.Signature.create(
        0,
        signatureBytes.slice(0, 32).toUnsignedBigInteger(),
        signatureBytes.slice(32).toUnsignedBigInteger()
      )
      return SECP256K1.verifyHashed(hashedSignValue, signature0, publicKey)
    } else {
      return true
    }
  }

  fun awaitConnection(): AsyncResult<SessionKey> = connected

  fun tag() = Message.tag(nodeId, EthereumNodeRecord.nodeId(publicKey!!))
}
