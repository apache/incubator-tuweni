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

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.v5.encrypt.AES128GCM
import org.apache.tuweni.devp2p.v5.misc.AuthHeader
import org.apache.tuweni.devp2p.v5.misc.DecodeResult
import org.apache.tuweni.devp2p.v5.misc.EncodeResult
import org.apache.tuweni.devp2p.v5.misc.HandshakeInitParameters
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlp.RLPReader

/**
 * Message reader/writer. It encodes and decodes messages, structured like at schema below
 *
 * tag || auth_tag || message
 *
 * tag || auth_header || message
 *
 * magic || message
 *
 * It also responsible for encryption functionality, so handlers receives raw messages for processing
 */
internal class PacketCodec(
  private val keyPair: SECP256K1.KeyPair,
  private val routingTable: RoutingTable,
  private val nodeId: Bytes = Hash.sha2_256(routingTable.getSelfEnr()),
  private val authenticationProvider: AuthenticationProvider = DefaultAuthenticationProvider(
    keyPair,
    routingTable
  )
) {

  /**
   * Encodes message, encrypting its body
   *
   * @param message message for encoding
   * @param destNodeId receiver node identifier for tag creation
   * @param handshakeParams optional handshake parameter, if it is required to initialize handshake
   *
   * @return encoded message
   */
  fun encode(message: Message, destNodeId: Bytes, handshakeParams: HandshakeInitParameters? = null): EncodeResult {
    if (message is WhoAreYouMessage) {
      val magic = Message.magic(nodeId)
      val content = message.encode()
      return EncodeResult(magic, Bytes.wrap(magic, content))
    }

    val tag = Message.tag(nodeId, destNodeId)
    if (message is RandomMessage) {
      return encodeRandomMessage(tag, message)
    }

    val sessionKey = authenticationProvider.findSessionKey(destNodeId.toHexString())
    val authHeader = handshakeParams?.let {
      if (null == sessionKey) {
        authenticationProvider.authenticate(handshakeParams)
      } else null
    }

    val initiatorKey = authenticationProvider.findSessionKey(destNodeId.toHexString())?.initiatorKey
      ?: return encodeRandomMessage(tag, RandomMessage())
    val messagePlain = Bytes.wrap(message.messageIdentifier(), message.encode())
    return if (null != authHeader) {
      val encodedHeader = authHeader.asRlp()
      val authTag = authHeader.authTag
      val encryptionMeta = Bytes.wrap(tag, encodedHeader)
      val encryptionResult = AES128GCM.encrypt(initiatorKey, authTag, messagePlain, encryptionMeta)
      if (message is NodesMessage) {
        println(encryptionResult)
      }
      EncodeResult(authTag, Bytes.wrap(tag, encodedHeader, encryptionResult))
    } else {
      val authTag = Message.authTag()
      val authTagHeader = RLP.encodeValue(authTag)
      val encryptionResult = AES128GCM.encrypt(initiatorKey, authTag, messagePlain, tag)
      EncodeResult(authTag, Bytes.wrap(tag, authTagHeader, encryptionResult))
    }
  }

  /**
   * Decodes message, decrypting its body
   *
   * @param message message for decoding
   *
   * @return decoding result, including sender identifier and decoded message
   */
  fun decode(message: Bytes): DecodeResult {
    val tag = message.slice(0, Message.TAG_LENGTH)
    val senderNodeId = Message.getSourceFromTag(tag, nodeId)
    val contentWithHeader = message.slice(Message.TAG_LENGTH)
    val decodedMessage = RLP.decode(contentWithHeader) { reader -> read(tag, senderNodeId, contentWithHeader, reader) }
    return DecodeResult(senderNodeId, decodedMessage)
  }

  private fun read(tag: Bytes, senderNodeId: Bytes, contentWithHeader: Bytes, reader: RLPReader): Message {
    // Distinguish auth header or auth tag
    var authHeader: AuthHeader? = null
    var authTag: Bytes = Bytes.EMPTY
    if (reader.nextIsList()) {
      if (WHO_ARE_YOU_MESSAGE_LENGTH == contentWithHeader.size()) {
        return WhoAreYouMessage.create(contentWithHeader)
      }
      authHeader = reader.readList { listReader ->
        val authenticationTag = listReader.readValue()
        val idNonce = listReader.readValue()
        val authScheme = listReader.readString()
        val ephemeralPublicKey = listReader.readValue()
        val authResponse = listReader.readValue()
        return@readList AuthHeader(authenticationTag, idNonce, ephemeralPublicKey, authResponse, authScheme)
      }
      authenticationProvider.finalizeHandshake(senderNodeId, authHeader)
    } else {
      authTag = reader.readValue()
    }

    val encryptedContent = contentWithHeader.slice(reader.position())

    // Decrypt
    val decryptionKey = authenticationProvider.findSessionKey(senderNodeId.toHexString())?.initiatorKey
      ?: return RandomMessage.create(authTag, encryptedContent)
    val decryptMetadata = authHeader?.let { Bytes.wrap(tag, authHeader.asRlp()) } ?: tag
    val decryptedContent = AES128GCM.decrypt(encryptedContent, decryptionKey, decryptMetadata)
    val messageType = decryptedContent.slice(0, Byte.SIZE_BYTES)
    val message = decryptedContent.slice(Byte.SIZE_BYTES)

    // Retrieve result
    return when (messageType.toInt()) {
      1 -> PingMessage.create(message)
      2 -> PongMessage.create(message)
      3 -> FindNodeMessage.create(message)
      4 -> NodesMessage.create(message)
      5 -> RegTopicMessage.create(message)
      6 -> TicketMessage.create(message)
      7 -> RegConfirmationMessage.create(message)
      8 -> TopicQueryMessage.create(message)
      else -> throw IllegalArgumentException("Unknown message retrieved")
    }
  }

  private fun encodeRandomMessage(tag: Bytes, message: RandomMessage): EncodeResult {
    val rlpAuthTag = RLP.encodeValue(message.authTag)
    val content = message.encode()
    return EncodeResult(message.authTag, Bytes.wrap(tag, rlpAuthTag, content))
  }

  companion object {
    private const val WHO_ARE_YOU_MESSAGE_LENGTH = 48
  }
}
