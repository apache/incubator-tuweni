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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class)
internal class NeighborsPacketTest {

  @Test
  fun shouldHaveExpectedMinimumSize() {
    val packet =
      NeighborsPacket.create(SECP256K1.KeyPair.random(), System.currentTimeMillis(), emptyList())
    val buffer = packet.encode()
    // the minimum also includes a list length prefix of 4 bytes
    assertEquals(NeighborsPacket.RLP_MIN_SIZE, buffer.size() + 4)
  }

  @Test
  fun shouldEncodeThenDecodePacket() {
    val keyPair = SECP256K1.KeyPair.random()
    val neighbors = listOf(
      Node(Endpoint("10.0.0.54", 6543, 6543), SECP256K1.KeyPair.random().publicKey()),
      Node(Endpoint("192.168.34.65", 9832, 1453), SECP256K1.KeyPair.random().publicKey())
    )
    val now = System.currentTimeMillis()
    val pong = NeighborsPacket.create(keyPair, now, neighbors)

    val datagram = pong.encode()
    val packet = Packet.decodeFrom(datagram)
    assertTrue(packet is NeighborsPacket)

    val neighborsPacket = packet as NeighborsPacket
    assertEquals(keyPair.publicKey(), neighborsPacket.nodeId)
    assertEquals(neighbors, neighborsPacket.nodes)
    assertEquals(((now + PACKET_EXPIRATION_PERIOD_MS + 999) / 1000) * 1000, neighborsPacket.expiration)
  }

  @Test
  fun decodeReferencePacket1() {
    // https://github.com/ethereum/EIPs/blob/master/EIPS/eip-8.md
    val datagram = Bytes.fromHexString(
      "c679fc8fe0b8b12f06577f2e802d34f6fa257e6137a995f6f4cbfc9ee50ed3710faf6e66f932c4c8" +
        "d81d64343f429651328758b47d3dbc02c4042f0fff6946a50f4a49037a72bb550f3a7872363a83e1" +
        "b9ee6469856c24eb4ef80b7535bcf99c0004f9015bf90150f84d846321163782115c82115db84031" +
        "55e1427f85f10a5c9a7755877748041af1bcd8d474ec065eb33df57a97babf54bfd2103575fa8291" +
        "15d224c523596b401065a97f74010610fce76382c0bf32f84984010203040101b840312c55512422" +
        "cf9b8a4097e9a6ad79402e87a15ae909a4bfefa22398f03d20951933beea1e4dfa6f968212385e82" +
        "9f04c2d314fc2d4e255e0d3bc08792b069dbf8599020010db83c4d001500000000abcdef12820d05" +
        "820d05b84038643200b172dcfef857492156971f0e6aa2c538d8b74010f8e140811d53b98c765dd2" +
        "d96126051913f44582e8c199ad7c6d6819e9a56483f637feaac9448aacf8599020010db885a308d3" +
        "13198a2e037073488203e78203e8b8408dcab8618c3253b558d459da53bd8fa68935a719aff8b811" +
        "197101a4b2b47dd2d47295286fc00cc081bb542d760717d1bdd6bec2c37cd72eca367d6dd3b9df73" +
        "8443b9a355010203b525a138aa34383fec3d2719a0"
    )
    val packet = Packet.decodeFrom(datagram)

    assertTrue(packet is NeighborsPacket)
  }
}
