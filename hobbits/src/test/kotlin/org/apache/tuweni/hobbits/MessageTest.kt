// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.hobbits

import org.apache.tuweni.bytes.Bytes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MessageTest {

  @Test
  fun parseMessageRoundtrip() {
    val msg = Message(
      protocol = Protocol.PING,
      headers = Bytes.fromHexString("deadbeef01"),
      body = Bytes.fromHexString("deadbeef02")
    )
    val serialized = msg.toBytes()
    val read = Message.readMessage(serialized)!!
    assertEquals(3, read.version)
    assertEquals(Protocol.PING, read.protocol)
    assertEquals(Bytes.fromHexString("deadbeef01"), read.headers)
    assertEquals(Bytes.fromHexString("deadbeef02"), read.body)
  }

  @Test
  fun invalidProtocol() {
    assertEquals(Protocol.RPC, Protocol.fromByte(0))
    assertEquals(Protocol.GOSSIP, Protocol.fromByte(1))
    assertEquals(Protocol.PING, Protocol.fromByte(2))
    assertThrows<IllegalArgumentException> { Protocol.fromByte(4) }
  }

  @Test
  fun testToString() {
    val msg = Message(
      protocol = Protocol.PING,
      headers = Bytes.fromHexString("deadbeef01"),
      body = Bytes.fromHexString("deadbeef02")
    )
    assertEquals(
      "EWP 3 PING 5 5\n" +
        "0xdeadbeef01\n" +
        "0xdeadbeef02",
      msg.toString()
    )
  }

  @Test
  fun testSize() {
    val msg = Message(
      protocol = Protocol.PING,
      headers = Bytes.fromHexString("deadbeef01"),
      body = Bytes.fromHexString("deadbeef02")
    )
    assertEquals(
      26,
      msg.size()
    )
  }

  @Test
  fun testReadMessage() {
    val msg = Message(
      protocol = Protocol.PING,
      headers = Bytes.fromHexString("deadbeef01"),
      body = Bytes.fromHexString("deadbeef02")
    )
    val serialized = msg.toBytes()
    assertNull(Message.readMessage(serialized.slice(0, 3)))
    assertNull(Message.readMessage(serialized.slice(0, 20)))
    assertNotNull(Message.readMessage(serialized))
  }
}
