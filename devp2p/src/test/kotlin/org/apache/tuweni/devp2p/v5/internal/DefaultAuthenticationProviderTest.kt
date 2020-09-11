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

import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.RoutingTable
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress

@ExtendWith(BouncyCastleExtension::class)
class DefaultAuthenticationProviderTest {

  private val providerKeyPair: SECP256K1.KeyPair = SECP256K1.KeyPair.random()
  private val providerEnr = EthereumNodeRecord.create(providerKeyPair, ip = InetAddress.getLoopbackAddress())
  private val routingTable: RoutingTable =
    RoutingTable(providerEnr)

  @Test
  fun authenticateReturnsValidAuthHeader() {
//    val keyPair = SECP256K1.KeyPair.random()
//    val nonce = Bytes.fromHexString("0x012715E4EFA2464F51BE49BBC40836E5816B3552249F8AC00AD1BBDB559E44E9")
//    val authTag = Bytes.fromHexString("0x39BBC27C8CFA3735DF436AC6")
//    val destEnr = EthereumNodeRecord.toRLP(keyPair, ip = InetAddress.getLoopbackAddress())
//    val params = HandshakeInitParameters(nonce, authTag, destEnr)
//
//    val result = packetCodec.authenticate(params)
//
//    assert(result.idNonce == nonce)
//    assert(result.authTag == authTag)
//    assert(result.authScheme == "gcm")
//    assert(result.ephemeralPublicKey != providerKeyPair.publicKey().bytes())
//
//    val destNodeId = Hash.sha2_256(destEnr).toHexString()
//
//    assert(packetCodec.findSessionKey(destNodeId) != null)
  }
//
//  @Test
//  fun finalizeHandshakePersistsCreatedSessionKeys() {
//    val keyPair = SECP256K1.KeyPair.random()
//    val selfEnr = EthereumNodeRecord.create(keyPair, ip = InetAddress.getLoopbackAddress(), udp = 12344)
//    val nodeId = Hash.sha2_256(selfEnr.toRLP())
//    val nonce = Bytes.fromHexString("0x012715E4EFA2464F51BE49BBC40836E5816B3552249F8AC00AD1BBDB559E44E9")
//    val authTag = Bytes.fromHexString("0x39BBC27C8CFA3735DF436AC6")
//    val destKeyPair = SECP256K1.KeyPair.random()
//    val destEnr = EthereumNodeRecord.create(destKeyPair, ip = InetAddress.getLoopbackAddress(), udp = 12345)
//    val destNodeId = Hash.sha2_256(destEnr.toRLP())
//    val ephemeralKeyPair = SECP256K1.KeyPair.random()
//    val ephemeralKey = ephemeralKeyPair.secretKey()
//
//    val signValue = Bytes.concatenate(Bytes.wrap("discovery-id-nonce".toByteArray()), nonce)
//    val hashedSignValue = Hash.sha2_256(signValue)
//    val signature =  SECP256K1.sign(hashedSignValue, keyPair)
//
//    val plain = RLP.encodeList { writer ->
//      writer.writeInt(5)
//      writer.writeValue(signature.bytes())
//      writer.writeValue(selfEnr.toRLP())
//    }
//
//    val hkdf = HKDFBytesGenerator(SHA256Digest())
//
//    val secret = SECP256K1.calculateKeyAgreement(ephemeralKey, destEnr.publicKey())
//    val info = Bytes.wrap(Bytes.wrap("discovery v5 key agreement".toByteArray()), nodeId, destNodeId)
//    val params = HKDFParameters(secret.toArrayUnsafe(), nonce.toArrayUnsafe(), info.toArrayUnsafe())
//    hkdf.init(params)
//    val initiatorKey = Bytes.wrap(ByteArray(16))
//    hkdf.generateBytes(initiatorKey.toArrayUnsafe(), 0, initiatorKey.size())
//    val recipientKey = Bytes.wrap(ByteArray(16))
//    hkdf.generateBytes(recipientKey.toArrayUnsafe(), 0, recipientKey.size())
//    val authRespKey = Bytes.wrap(ByteArray(16))
//    hkdf.generateBytes(authRespKey.toArrayUnsafe(), 0, authRespKey.size())
//    val zeroNonce = Bytes.wrap(ByteArray(12))
//    val authResponse = AES128GCM.encrypt(authRespKey, zeroNonce, plain, Bytes.EMPTY)
//
//    val authHeader = AuthHeader(authTag, nonce, ephemeralKeyPair.publicKey().bytes(), authResponse)
//
//    val session = Session(destKeyPair, selfEnr, InetSocketAddress(InetAddress.getLoopbackAddress(), 12345),
//      destNodeId, { _, _ -> }, { destEnr }, RoutingTable(destEnr), TopicTable(), { _ -> false }, Dispatchers.Default)
//    session.finalizeHandshake(nodeId, authHeader)
//
//    assertNotNull(session.sessionKey)
//
//  }

  @Test
  fun findSessionKeyRetrievesSessionKeyIfExists() {
//    val result = packetCodec.findSessionKey(Bytes.random(32).toHexString())
//
//    assertNull(result)
  }
}
