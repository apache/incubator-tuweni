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
internal class FindNodePacketTest {

  @Test
  fun shouldEncodeThenDecodePacket() {
    val keyPair = SECP256K1.KeyPair.random()
    val target = SECP256K1.KeyPair.random().publicKey()
    val now = System.currentTimeMillis()
    val pong = FindNodePacket.create(keyPair, now, target)

    val datagram = pong.encode()
    val packet = Packet.decodeFrom(datagram)
    assertTrue(packet is FindNodePacket)

    val findNodePacket = packet as FindNodePacket
    assertEquals(keyPair.publicKey(), findNodePacket.nodeId)
    assertEquals(target, findNodePacket.target)
    assertEquals(((now + PACKET_EXPIRATION_PERIOD_MS + 999) / 1000) * 1000, findNodePacket.expiration)
  }

  @Test
  fun decodeReferencePacket1() {
    // https://github.com/ethereum/EIPs/blob/master/EIPS/eip-8.md
    val datagram = Bytes.fromHexString(
      "c7c44041b9f7c7e41934417ebac9a8e1a4c6298f74553f2fcfdcae6ed6fe53163eb3d2b52e39fe91" +
        "831b8a927bf4fc222c3902202027e5e9eb812195f95d20061ef5cd31d502e47ecb61183f74a504fe" +
        "04c51e73df81f25c4d506b26db4517490103f84eb840ca634cae0d49acb401d8a4c6b6fe8c55b70d" +
        "115bf400769cc1400f3258cd31387574077f301b421bc84df7266c44e9e6d569fc56be0081290476" +
        "7bf5ccd1fc7f8443b9a35582999983999999280dc62cc8255c73471e0a61da0c89acdc0e035e260a" +
        "dd7fc0c04ad9ebf3919644c91cb247affc82b69bd2ca235c71eab8e49737c937a2c396"
    )
    val packet = Packet.decodeFrom(datagram)

    assertTrue(packet is FindNodePacket)
  }
}
