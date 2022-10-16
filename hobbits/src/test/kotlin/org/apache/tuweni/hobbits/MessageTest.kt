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
