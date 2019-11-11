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
package org.apache.tuweni.devp2p.v5.internal

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.v5.AuthenticationProvider
import org.apache.tuweni.devp2p.v5.PacketCodec
import org.apache.tuweni.devp2p.v5.storage.RoutingTable
import org.apache.tuweni.devp2p.v5.encrypt.AES128GCM
import org.apache.tuweni.devp2p.v5.packet.FindNodeMessage
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import org.apache.tuweni.devp2p.v5.misc.AuthHeader
import org.apache.tuweni.devp2p.v5.misc.DecodeResult
import org.apache.tuweni.devp2p.v5.misc.EncodeResult
import org.apache.tuweni.devp2p.v5.misc.HandshakeInitParameters
import org.apache.tuweni.devp2p.v5.packet.NodesMessage
import org.apache.tuweni.devp2p.v5.packet.PingMessage
import org.apache.tuweni.devp2p.v5.packet.PongMessage
import org.apache.tuweni.devp2p.v5.packet.RegConfirmationMessage
import org.apache.tuweni.devp2p.v5.packet.RegTopicMessage
import org.apache.tuweni.devp2p.v5.packet.TicketMessage
import org.apache.tuweni.devp2p.v5.packet.TopicQueryMessage
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlp.RLPReader
import kotlin.IllegalArgumentException

class DefaultPacketCodec(
  private val keyPair: SECP256K1.KeyPair,
  private val routingTable: RoutingTable,
  private val nodeId: Bytes = Hash.sha2_256(routingTable.getSelfEnr()),
  private val authenticationProvider: AuthenticationProvider = DefaultAuthenticationProvider(keyPair, routingTable)
) : PacketCodec {

  override fun encode(message: UdpMessage, destNodeId: Bytes, handshakeParams: HandshakeInitParameters?): EncodeResult {
    if (message is WhoAreYouMessage) {
      val magic = UdpMessage.magic(nodeId)
      val content = message.encode()
      return EncodeResult(magic, Bytes.wrap(magic, content))
    }

    val tag = UdpMessage.tag(nodeId, destNodeId)
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
    val messagePlain = Bytes.wrap(message.getMessageType(), message.encode())
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
      val authTag = UdpMessage.authTag()
      val authTagHeader = RLP.encodeValue(authTag)
      val encryptionResult = AES128GCM.encrypt(initiatorKey, authTag, messagePlain, tag)
      EncodeResult(authTag, Bytes.wrap(tag, authTagHeader, encryptionResult))
    }
  }

  override fun decode(message: Bytes): DecodeResult {
    val tag = message.slice(0, UdpMessage.TAG_LENGTH)
    val senderNodeId = UdpMessage.getSourceFromTag(tag, nodeId)
    val contentWithHeader = message.slice(UdpMessage.TAG_LENGTH)
    val decodedMessage = RLP.decode(contentWithHeader) { reader -> read(tag, senderNodeId, contentWithHeader, reader) }
    return DecodeResult(senderNodeId, decodedMessage)
  }

  private fun read(tag: Bytes, senderNodeId: Bytes, contentWithHeader: Bytes, reader: RLPReader): UdpMessage {
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
