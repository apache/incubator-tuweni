// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
