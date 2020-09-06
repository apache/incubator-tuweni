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

import kotlinx.coroutines.CoroutineScope
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.CompletableAsyncResult
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.encrypt.AES128GCM
import org.apache.tuweni.devp2p.v5.encrypt.SessionKeyGenerator
import org.apache.tuweni.devp2p.v5.misc.SessionKey
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlp.RLPReader
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext

private val DISCOVERY_ID_NONCE: Bytes = Bytes.wrap("discovery-id-nonce".toByteArray())

internal class HandshakeSession(
  private val keyPair: SECP256K1.KeyPair,
  private val address: InetSocketAddress,
  private var publicKey: SECP256K1.PublicKey? = null,
  private val sendFn: (address: InetSocketAddress, message: Bytes) -> Unit,
  private val enr: () -> EthereumNodeRecord,
  override val coroutineContext: CoroutineContext
) : CoroutineScope {

  private val connected: CompletableAsyncResult<SessionKey> = AsyncResult.incomplete()
  var receivedEnr : EthereumNodeRecord? = null
  val nodeId = Hash.sha2_256(keyPair.publicKey().bytes())
  private val whoAreYouHeader = Hash.sha2_256(Bytes.concatenate(nodeId, Bytes.wrap("WHOAREYOU".toByteArray())))

  private val tokens = ArrayList<Bytes>()

  companion object {
    private val logger = LoggerFactory.getLogger(HandshakeSession::class.java)
  }

  fun connect(): AsyncResult<SessionKey> {
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

      val destNodeId = Hash.sha2_256(publicKey!!.bytes())
      val secret = SECP256K1.calculateKeyAgreement(ephemeralKey, publicKey)

      // Derive keys
      val newSession = SessionKeyGenerator.generate(nodeId, destNodeId, secret, message.idNonce)
      val signValue = Bytes.wrap(DISCOVERY_ID_NONCE, message.idNonce)
      val hashedSignValue = Hash.sha2_256(signValue)
      val signature = SECP256K1.sign(hashedSignValue, keyPair)
      val plain = RLP.encodeList { writer ->
        writer.writeInt(5)
        writer.writeValue(signature.bytes())
        writer.writeValue(enr().toRLP())
      }
      val zeroNonce = Bytes.wrap(ByteArray(12))
      val authResponse = AES128GCM.encrypt(newSession.authRespKey, zeroNonce, plain, Bytes.EMPTY)
      val authTag = Message.authTag()
      val encryptedMessage = AES128GCM.encrypt(
        newSession.initiatorKey,
        authTag,
        Bytes.concatenate(Bytes.of(MessageType.FINDNODE.byte()), FindNodeMessage().toRLP()),
        message.token
      )
      val response = Bytes.concatenate(tag(), RLP.encodeList {
        it.writeValue(authTag)
        it.writeValue(message.idNonce)
        it.writeValue(Bytes.wrap("gcm".toByteArray()))
        it.writeValue(ephemeralKeyPair.publicKey().bytes())
        it.writeValue(authResponse)
      }, encryptedMessage)
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
          val secret = SECP256K1.calculateKeyAgreement(keyPair.secretKey(), ephemeralPublicKey)
          val senderNodeId = Message.getSourceFromTag(tag, nodeId)
          val sessionKey = SessionKeyGenerator.generate(senderNodeId, nodeId, secret, idNonce)
          val decryptedAuthResponse = AES128GCM.decrypt(authResponse, sessionKey.authRespKey, Bytes.EMPTY)
          RLP.decodeList(Bytes.wrap(decryptedAuthResponse)) { reader ->
            reader.skipNext()
            val signatureBytes = reader.readValue()
            val enrRLP = reader.readValue()
            val enr = EthereumNodeRecord.fromRLP(enrRLP)
            receivedEnr = enr
            publicKey = enr.publicKey()
            val signatureVerified = verifySignature(signatureBytes, idNonce, enr.publicKey())
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
        sendFn(address, response.toRLP())
      }
    }
  }

  private fun verifySignature(signatureBytes: Bytes, idNonce: Bytes, publicKey: SECP256K1.PublicKey): Boolean {
    val signature = SECP256K1.Signature.fromBytes(signatureBytes)
    val signValue = Bytes.concatenate(DISCOVERY_ID_NONCE, idNonce)
    val hashedSignValue = Hash.sha2_256(signValue)
    return SECP256K1.verify(hashedSignValue, signature, publicKey)
  }

  fun awaitConnection(): AsyncResult<SessionKey> = connected

  fun tag() = Message.tag(nodeId, Hash.sha2_256(publicKey!!.bytes()))
}
