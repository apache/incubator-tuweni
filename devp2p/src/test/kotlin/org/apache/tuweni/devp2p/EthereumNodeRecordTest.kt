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
    val keyPair = SECP256K1.KeyPair.random()
    val enr = EthereumNodeRecord.create(
      keyPair,
      1,
      emptyMap(),
      emptyMap(),
      InetAddress.getLoopbackAddress(),
      null,
      10000
    )
    enr.validate()
    assertEquals(1L, enr.seq())
    assertEquals(Bytes.wrap("v4".toByteArray()), enr.data["id"])
    assertEquals(Bytes.fromHexString("7f000001"), enr.data["ip"])
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
    assertEquals(1L, record.seq())
    assertEquals(Bytes.wrap("v4".toByteArray()), record.data["id"])
    assertEquals(Bytes.fromHexString("7f000001"), record.data["ip"])
    assertEquals(keypair.publicKey(), record.publicKey())
    assertEquals(Bytes.fromHexString("deadbeef"), record.data["key"])
    assertEquals(Bytes.fromHexString("deadbeef"), (record.listData["foo"] ?: error("None"))[0])
  }
}
