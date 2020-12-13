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
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class)
internal class PingPacketTest {

  @Test
  fun shouldEncodeThenDecodePacket() {
    val keyPair = SECP256K1.KeyPair.random()
    val from = Endpoint("10.0.0.54", 6543, 6543)
    val to = Endpoint("192.168.34.65", 9832, 1453)
    val now = System.currentTimeMillis()
    val ping = PingPacket.create(keyPair, now, from, to, null)

    val datagram = ping.encode()
    val packet = Packet.decodeFrom(datagram)
    assertTrue(packet is PingPacket)

    val pingPacket = packet as PingPacket
    assertEquals(keyPair.publicKey(), pingPacket.nodeId)
    assertEquals(Endpoint("10.0.0.54", 6543, 6543), pingPacket.from)
    assertEquals(Endpoint("192.168.34.65", 9832, 1453), pingPacket.to)
    assertEquals(((now + PACKET_EXPIRATION_PERIOD_MS + 999) / 1000) * 1000, pingPacket.expiration)
  }

  @Test
  fun shouldEncodeThenDecodePacketWithEnrSeq() {
    val keyPair = SECP256K1.KeyPair.random()
    val from = Endpoint("10.0.0.54", 6543, 6543)
    val to = Endpoint("192.168.34.65", 9832, 1453)
    val now = System.currentTimeMillis()
    val ping = PingPacket.create(keyPair, now, from, to, 64)

    val datagram = ping.encode()
    val packet = Packet.decodeFrom(datagram)
    assertTrue(packet is PingPacket)

    val pingPacket = packet as PingPacket
    assertEquals(keyPair.publicKey(), pingPacket.nodeId)
    assertEquals(Endpoint("10.0.0.54", 6543, 6543), pingPacket.from)
    assertEquals(Endpoint("192.168.34.65", 9832, 1453), pingPacket.to)
    assertEquals(((now + PACKET_EXPIRATION_PERIOD_MS + 999) / 1000) * 1000, pingPacket.expiration)
    assertEquals(64L, pingPacket.enrSeq)
  }

  @Test
  fun shouldDecodePingPacketWithMissingEndpoint() {
    val keyPair = SECP256K1.KeyPair.random()
    val from = Endpoint("10.0.0.54", 6543, 6543)
    val to = Endpoint("192.168.34.65", 9832, 1453)
    val now = System.currentTimeMillis()
    val ping = PingPacket.create(keyPair, now, from, to, null)

    val datagram = ping.encode()
    val packet = Packet.decodeFrom(datagram)
    assertTrue(packet is PingPacket)

    val pingPacket = packet as PingPacket
    assertEquals(keyPair.publicKey(), pingPacket.nodeId)
    assertEquals(Endpoint("10.0.0.54", 6543, 6543), pingPacket.from)
    assertEquals(Endpoint("192.168.34.65", 9832, 1453), pingPacket.to)
    assertEquals(((now + PACKET_EXPIRATION_PERIOD_MS + 999) / 1000) * 1000, pingPacket.expiration)
  }

  @Test
  fun decodeReferencePacket1() {
    // https://github.com/ethereum/EIPs/blob/master/EIPS/eip-8.md
    val datagram = Bytes.fromHexString(
      "e9614ccfd9fc3e74360018522d30e1419a143407ffcce748de3e22116b7e8dc92ff74788c0b6663a" +
        "aa3d67d641936511c8f8d6ad8698b820a7cf9e1be7155e9a241f556658c55428ec0563514365799a" +
        "4be2be5a685a80971ddcfa80cb422cdd0101ec04cb847f000001820cfa8215a8d790000000000000" +
        "000000000000000000018208ae820d058443b9a3550102"
    )
    val packet = Packet.decodeFrom(datagram)

    assertTrue(packet is PingPacket)
  }

  @Disabled("EIP-868 supercedes EIP-8 behavior")
  @Test
  fun decodeReferencePacket2() {
    // https://github.com/ethereum/EIPs/blob/master/EIPS/eip-8.md
    val datagram = Bytes.fromHexString(
      "577be4349c4dd26768081f58de4c6f375a7a22f3f7adda654d1428637412c3d7fe917cadc56d4e5e" +
        "7ffae1dbe3efffb9849feb71b262de37977e7c7a44e677295680e9e38ab26bee2fcbae207fba3ff3" +
        "d74069a50b902a82c9903ed37cc993c50001f83e82022bd79020010db83c4d001500000000abcdef" +
        "12820cfa8215a8d79020010db885a308d313198a2e037073488208ae82823a8443b9a355c5010203" +
        "040531b9019afde696e582a78fa8d95ea13ce3297d4afb8ba6433e4154caa5ac6431af1b80ba7602" +
        "3fa4090c408f6b4bc3701562c031041d4702971d102c9ab7fa5eed4cd6bab8f7af956f7d565ee191" +
        "7084a95398b6a21eac920fe3dd1345ec0a7ef39367ee69ddf092cbfe5b93e5e568ebc491983c09c7" +
        "6d922dc3"
    )
    val packet = Packet.decodeFrom(datagram)

    assertTrue(packet is PingPacket)
  }
}
