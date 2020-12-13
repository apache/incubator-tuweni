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

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class)
internal class PongPacketTest {

  @Test
  fun shouldEncodeThenDecodePacket() {
    val keyPair = SECP256K1.KeyPair.random()
    val to = Endpoint("10.0.0.54", 6543, 6543)
    val pingHash = Bytes32.random()
    val now = System.currentTimeMillis()
    val pong = PongPacket.create(keyPair, now, to, pingHash, null)

    val datagram = pong.encode()
    val packet = Packet.decodeFrom(datagram)
    assertTrue(packet is PongPacket)

    val pongPacket = packet as PongPacket
    assertEquals(keyPair.publicKey(), pongPacket.nodeId)
    assertEquals(Endpoint("10.0.0.54", 6543, 6543), pongPacket.to)
    assertEquals(pingHash, pongPacket.pingHash)
    assertEquals(((now + PACKET_EXPIRATION_PERIOD_MS + 999) / 1000) * 1000, pongPacket.expiration)
  }

  @Test
  fun shouldEncodeThenDecodePacketWithSeq() {
    val keyPair = SECP256K1.KeyPair.random()
    val to = Endpoint("10.0.0.54", 6543, 6543)
    val pingHash = Bytes32.random()
    val now = System.currentTimeMillis()
    val pong = PongPacket.create(keyPair, now, to, pingHash, 32)

    val datagram = pong.encode()
    val packet = Packet.decodeFrom(datagram)
    assertTrue(packet is PongPacket)

    val pongPacket = packet as PongPacket
    assertEquals(keyPair.publicKey(), pongPacket.nodeId)
    assertEquals(Endpoint("10.0.0.54", 6543, 6543), pongPacket.to)
    assertEquals(pingHash, pongPacket.pingHash)
    assertEquals(((now + PACKET_EXPIRATION_PERIOD_MS + 999) / 1000) * 1000, pongPacket.expiration)
    assertEquals(32L, pongPacket.enrSeq)
  }

  @Disabled("EIP-868 supercedes EIP-8 behavior")
  @Test
  fun decodeReferencePacket1() {
    // https://github.com/ethereum/EIPs/blob/master/EIPS/eip-8.md
    val datagram = Bytes.fromHexString(
      "09b2428d83348d27cdf7064ad9024f526cebc19e4958f0fdad87c15eb598dd61d08423e0bf66b206" +
        "9869e1724125f820d851c136684082774f870e614d95a2855d000f05d1648b2d5945470bc187c2d2" +
        "216fbe870f43ed0909009882e176a46b0102f846d79020010db885a308d313198a2e037073488208" +
        "ae82823aa0fbc914b16819237dcd8801d7e53f69e9719adecb3cc0e790c57e91ca4461c9548443b9" +
        "a355c6010203c2040506a0c969a58f6f9095004c0177a6b47f451530cab38966a25cca5cb58f0555" +
        "42124e"
    )
    val packet = Packet.decodeFrom(datagram)

    assertTrue(packet is PongPacket)
  }
}
