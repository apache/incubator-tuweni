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
}
