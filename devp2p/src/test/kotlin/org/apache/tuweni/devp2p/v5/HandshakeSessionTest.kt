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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.encrypt.AES128GCM
import org.apache.tuweni.devp2p.v5.encrypt.SessionKeyGenerator
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress

@ExtendWith(BouncyCastleExtension::class)
class HandshakeSessionTest {

  @Test
  fun testConnectTwoClients() =
    runBlocking {
      val keyPair = SECP256K1.KeyPair.random()
      val peerKeyPair = SECP256K1.KeyPair.random()
      val address = SocketAddress.inetSocketAddress(1234, "localhost")
      val peerAddress = SocketAddress.inetSocketAddress(1235, "localhost")
      val enr = EthereumNodeRecord.create(keyPair, ip = InetAddress.getLoopbackAddress(), udp = 1234)
      val peerEnr = EthereumNodeRecord.create(peerKeyPair, ip = InetAddress.getLoopbackAddress(), udp = 1235)
      var peerSession: HandshakeSession? = null

      val session =
        HandshakeSession(
          keyPair,
          peerAddress,
          peerKeyPair.publicKey(),
          { _, message -> runBlocking { peerSession!!.processMessage(message) } },
          { enr },
          Dispatchers.Default
        )
      peerSession =
        HandshakeSession(
          peerKeyPair,
          address,
          keyPair.publicKey(),
          { _, message -> runBlocking { session.processMessage(message) } },
          { peerEnr },
          Dispatchers.Default
        )

      val key = session.connect().await()
      val peerKey = peerSession.awaitConnection().await()
      assertEquals(key, peerKey)
    }

  @Test
  fun testInitiatorAndRecipientKey() {
    val keyPair = SECP256K1.KeyPair.random()
    val peerKeyPair = SECP256K1.KeyPair.random()
    val ephemeralKeyPair = SECP256K1.KeyPair.random()
    val enr = EthereumNodeRecord.create(keyPair, ip = InetAddress.getLoopbackAddress(), udp = 1234)
    val peerEnr = EthereumNodeRecord.create(peerKeyPair, ip = InetAddress.getLoopbackAddress(), udp = 1235)
    val secret = SECP256K1.deriveECDHKeyAgreement(ephemeralKeyPair.secretKey().bytes(), keyPair.publicKey().bytes())
    val nonce = Bytes.random(12)
    val session = SessionKeyGenerator.generate(enr.nodeId(), peerEnr.nodeId(), secret, nonce)
    val peerSession = SessionKeyGenerator.generate(enr.nodeId(), peerEnr.nodeId(), secret, nonce)
    val authTag = Message.authTag()
    val token = Message.authTag()
    val encryptedMessage = AES128GCM.encrypt(
      session.initiatorKey,
      authTag,
      Bytes.wrap("hello world".toByteArray()),
      token
    )
    val decryptedMessage = AES128GCM.decrypt(peerSession.initiatorKey, authTag, encryptedMessage, token)
    assertEquals(Bytes.wrap("hello world".toByteArray()), decryptedMessage)
  }
}
