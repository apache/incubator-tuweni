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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress
import java.net.InetSocketAddress

@ExtendWith(BouncyCastleExtension::class)
class HandshakeSessionTest {

  @Test
  fun testConnectTwoClients() =
    runBlocking {
      val keyPair = SECP256K1.KeyPair.random()
      val peerKeyPair = SECP256K1.KeyPair.random()
      val address = InetSocketAddress(InetAddress.getLoopbackAddress(), 1234)
      val peerAddress = InetSocketAddress(InetAddress.getLoopbackAddress(), 1235)
      val enr = EthereumNodeRecord.create(keyPair, ip = InetAddress.getLoopbackAddress(), udp = 1234)
      val peerEnr = EthereumNodeRecord.create(peerKeyPair, ip = InetAddress.getLoopbackAddress(), udp = 1235)
      var peerSession: HandshakeSession? = null

      val session =
        HandshakeSession(
          keyPair,
          peerAddress,
          peerKeyPair.publicKey(),
          { _, message -> runBlocking { peerSession!!.processMessage(message) } },
          { enr }, Dispatchers.Default
        )
      peerSession =
        HandshakeSession(
          peerKeyPair,
          address,
          keyPair.publicKey(),
          { _, message -> runBlocking { session.processMessage(message) } },
          { peerEnr }, Dispatchers.Default
        )

      val key = session.connect().await()
      val peerKey = peerSession.awaitConnection().await()
      assertEquals(key, peerKey)
    }
}
