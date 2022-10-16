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
package org.apache.tuweni.devp2p

import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress

@ExtendWith(BouncyCastleExtension::class)
internal class ENRResponsePacketTest {

  @Test
  fun shouldEncodeThenDecodePacket() {
    val keyPair = SECP256K1.KeyPair.random()

    val requestHash = Bytes32.random()
    val enr = EthereumNodeRecord.toRLP(
      SECP256K1.KeyPair.random(),
      2,
      emptyMap(),
      emptyMap(),
      InetAddress.getByName("localhost"),
      3000,
      12000
    )
    val now = System.currentTimeMillis()
    val pong = ENRResponsePacket.create(keyPair, now, requestHash, enr)

    val datagram = pong.encode()
    val packet = Packet.decodeFrom(datagram)
    assertTrue(packet is ENRResponsePacket)

    val responsePacket = packet as ENRResponsePacket
    assertEquals(keyPair.publicKey(), responsePacket.nodeId)
    assertEquals(enr, responsePacket.enr)
    assertEquals(requestHash, responsePacket.requestHash)
    assertEquals(((now + PACKET_EXPIRATION_PERIOD_MS + 999) / 1000) * 1000, responsePacket.expiration)
  }
}
