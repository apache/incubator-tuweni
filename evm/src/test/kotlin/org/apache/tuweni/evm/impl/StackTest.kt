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
package org.apache.tuweni.evm.impl

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.units.bigints.UInt256
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StackTest {

  @Test
  fun testPushAndPop() {
    val tester = Bytes32.leftPad(Bytes.fromHexString("0xdeadbeef"))
    val stack = Stack()
    stack.push(tester)
    assertEquals(UInt256.fromBytes(tester), stack.pop())
  }

  @Test
  fun testPushPushAndPopPop() {
    val tester = Bytes32.leftPad(Bytes.fromHexString("0xdeadbeef"))
    val tester2 = Bytes32.leftPad(Bytes.fromHexString("0xdeadbeef20"))
    val stack = Stack()
    stack.push(tester)
    stack.push(tester2)
    assertEquals(UInt256.fromBytes(tester2), stack.pop())
    assertEquals(UInt256.fromBytes(tester), stack.pop())
  }

  @Test
  fun testPopTooMuch() {
    val stack = Stack()
    assertNull(stack.pop())
    assertNull(stack.pop())
  }

  @Test
  fun testPushTooMuch() {
    val tester = Bytes32.leftPad(Bytes.fromHexString("0xdeadbeef"))
    val stack = Stack(2)
    assertTrue(stack.push(tester))
    assertTrue(stack.push(tester))
    assertFalse(stack.push(tester))
  }

  @Test
  fun testPushGetAndPop() {
    val stack = Stack(10)
    stack.push(UInt256.valueOf(1))
    stack.push(UInt256.valueOf(2))
    stack.push(UInt256.valueOf(3))
    stack.push(UInt256.valueOf(4))
    stack.push(UInt256.valueOf(5))
    stack.push(UInt256.valueOf(6))
    stack.push(UInt256.valueOf(7))
    stack.push(UInt256.valueOf(8))
    assertEquals(UInt256.valueOf(4), stack.get(4))
    assertEquals(UInt256.valueOf(3), stack.get(5))
    assertEquals(UInt256.valueOf(2), stack.get(6))
    assertEquals(UInt256.valueOf(8), stack.pop())
    assertEquals(UInt256.valueOf(7), stack.pop())
    assertEquals(UInt256.valueOf(6), stack.pop())
    assertEquals(UInt256.valueOf(5), stack.pop())
    assertEquals(UInt256.valueOf(4), stack.pop())
    assertEquals(UInt256.valueOf(3), stack.pop())
    assertEquals(UInt256.valueOf(2), stack.pop())
    assertEquals(UInt256.valueOf(1), stack.pop())
  }

  @Test
  fun pushSet() {
    val stack = Stack(10)
    stack.push(UInt256.valueOf(3))
    stack.push(UInt256.valueOf(2))
    stack.push(UInt256.valueOf(1))
    stack.set(2, UInt256.valueOf(4))
    stack.set(1, UInt256.valueOf(5))
    stack.set(0, UInt256.valueOf(6))
    assertEquals(UInt256.valueOf(6), stack.get(0))
    assertEquals(UInt256.valueOf(5), stack.get(1))
    assertEquals(UInt256.valueOf(4), stack.get(2))
  }

  @Test
  fun bytes() {
    val stack = Stack(10)
    stack.push(Bytes.fromHexString("0x03"))
    stack.push(Bytes.fromHexString("0x0201"))
    stack.push(Bytes.fromHexString("0x040506"))
    stack.push(Bytes32.random())
    stack.push(Bytes32.random())
    stack.push(Bytes.fromHexString("0x01"))
    stack.push(Bytes.fromHexString("0x01"))
    assertEquals(Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001"), stack.pop())
    assertEquals(Bytes.fromHexString("0x01"), stack.popBytes())
    stack.pop()
    stack.pop()
    assertEquals(Bytes.fromHexString("0x040506"), stack.popBytes())
    assertEquals(Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000201"), stack.get(0))
    assertEquals(Bytes.fromHexString("0x0201"), stack.getBytes(0))
    assertEquals(Bytes.fromHexString("0x0201"), stack.popBytes())
    assertEquals(Bytes.fromHexString("0x03"), stack.popBytes())
  }
}
