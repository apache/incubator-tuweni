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
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.AuthenticationProvider
import org.apache.tuweni.devp2p.v5.PacketCodec
import org.apache.tuweni.devp2p.v5.storage.RoutingTable
import org.apache.tuweni.devp2p.v5.encrypt.AES128GCM
import org.apache.tuweni.devp2p.v5.misc.SessionKey
import org.apache.tuweni.devp2p.v5.packet.FindNodeMessage
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress

@ExtendWith(BouncyCastleExtension::class)
class DefaultPacketCodecTest {

  private val keyPair: SECP256K1.KeyPair = SECP256K1.KeyPair.random()
  private val enr: Bytes = EthereumNodeRecord.toRLP(keyPair, ip = InetAddress.getLocalHost())
  private val nodeId: Bytes = Hash.sha2_256(enr)
  private val routingTable: RoutingTable = RoutingTable(enr)
  private val authenticationProvider: AuthenticationProvider = DefaultAuthenticationProvider(keyPair, routingTable)

  private val codec: PacketCodec = DefaultPacketCodec(keyPair, routingTable, nodeId, authenticationProvider)

  private val destNodeId: Bytes = Bytes.random(32)

  @Test
  fun encodePerformsValidEncodingOfRandomMessage() {
    val message = RandomMessage()

    val encodedResult = codec.encode(message, destNodeId)

    val encodedContent = encodedResult.content.slice(45)
    val result = RandomMessage.create(UdpMessage.authTag(), encodedContent)

    assert(result.data == message.data)
  }

  @Test
  fun encodePerformsValidEncodingOfWhoAreYouMessage() {
    val message = WhoAreYouMessage()

    val encodedResult = codec.encode(message, destNodeId)

    val encodedContent = encodedResult.content.slice(32)
    val result = WhoAreYouMessage.create(encodedContent)

    assert(result.idNonce == message.idNonce)
    assert(result.enrSeq == message.enrSeq)
    assert(result.authTag == message.authTag)
  }

  @Test
  fun encodePerformsValidEncodingOfMessagesWithTypeIncluded() {
    val message = FindNodeMessage()

    val key = Bytes.random(16)
    val sessionKey = SessionKey(key, key, key)

    authenticationProvider.setSessionKey(destNodeId.toHexString(), sessionKey)

    val encodedResult = codec.encode(message, destNodeId)

    val tag = encodedResult.content.slice(0, UdpMessage.TAG_LENGTH)
    val encryptedContent = encodedResult.content.slice(45)
    val content = AES128GCM.decrypt(encryptedContent, sessionKey.initiatorKey, tag).slice(1)
    val result = FindNodeMessage.create(content)

    assert(result.requestId == message.requestId)
    assert(result.distance == message.distance)
  }

  @Test
  fun decodePerformsValidDecodingOfRandomMessasge() {
    val message = RandomMessage()

    val encodedResult = codec.encode(message, destNodeId)

    val result = codec.decode(encodedResult.content).message as? RandomMessage

    assert(null != result)
    assert(result!!.data == message.data)
  }

  @Test
  fun decodePerformsValidDecodingOfWhoAreYouMessage() {
    val message = WhoAreYouMessage()

    val encodedResult = codec.encode(message, destNodeId)

    val result = codec.decode(encodedResult.content).message as? WhoAreYouMessage

    assert(null != result)
    assert(result!!.idNonce == message.idNonce)
    assert(result.enrSeq == message.enrSeq)
    assert(result.authTag == message.authTag)
  }

  @Test
  fun decodePerformsValidDecodingOfMessagesWithTypeIncluded() {
    val message = FindNodeMessage()

    val key = Bytes.random(16)
    val sessionKey = SessionKey(key, key, key)

    authenticationProvider.setSessionKey(destNodeId.toHexString(), sessionKey)
    authenticationProvider.setSessionKey(nodeId.toHexString(), sessionKey)

    val encodedResult = codec.encode(message, nodeId)

    val result = codec.decode(encodedResult.content).message as? FindNodeMessage

    assert(result!!.requestId == message.requestId)
    assert(result.distance == message.distance)
  }
}
