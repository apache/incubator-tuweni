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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress

@ExtendWith(BouncyCastleExtension::class)
class EthereumNodeRecordTest {

  @Test
  fun tooLong() {
    val tooLong = Bytes.random(312)
    val exception: IllegalArgumentException = assertThrows({
      EthereumNodeRecord.fromRLP(tooLong)
    })
    assertEquals("Record too long", exception.message)
  }

  @Test
  fun readFromRLP() {
    val enr = EthereumNodeRecord.fromRLP(
      Bytes.fromHexString(
        "f884b8407098ad865b00a582051940cb9cf36836572411a4727878307701" +
          "1599ed5cd16b76f2635f4e234738f30813a89eb9137e3e3df5266e3a1f11" +
          "df72ecf1145ccb9c01826964827634826970847f00000189736563703235" +
          "366b31a103ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1" +
          "400f3258cd31388375647082765f"
      )
    )
    assertEquals(1L, enr.seq)
    assertEquals(Bytes.wrap("v4".toByteArray()), enr.data["id"])
    assertEquals(Bytes.fromHexString("7f000001"), enr.data["ip"])
    assertEquals(
      Bytes.fromHexString("03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138"),
      enr.data["secp256k1"]
    )
    assertEquals(Bytes.fromHexString("765f"), enr.data["udp"])
    enr.validate()
  }

  @Test
  fun toRLP() {
    val keypair = SECP256K1.KeyPair.random()
    val rlp = EthereumNodeRecord.toRLP(
      keypair,
      seq = 1L,
      data = mutableMapOf(Pair("key", Bytes.fromHexString("deadbeef"))),
      listData = mutableMapOf(Pair("foo", listOf(Bytes.fromHexString("deadbeef")))),
      ip = InetAddress.getByName("127.0.0.1")
    )
    val record = EthereumNodeRecord.fromRLP(rlp)
    assertEquals(1L, record.seq)
    assertEquals(Bytes.wrap("v4".toByteArray()), record.data["id"])
    assertEquals(Bytes.fromHexString("7f000001"), record.data["ip"])
    assertEquals(keypair.publicKey(), record.publicKey())
    assertEquals(Bytes.fromHexString("deadbeef"), record.data["key"])
    assertEquals(Bytes.fromHexString("deadbeef"), (record.listData["foo"] ?: error("None"))[0])
  }
}
