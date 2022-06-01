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
package org.apache.tuweni.evmdsl

import org.apache.tuweni.bytes.Bytes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class CodeTest {

  @Test
  fun testRoundtrip() {
    val code = Code(
      buildList {
        this.add(Push(Bytes.fromHexString("0xdeadbeef")))
        this.add(Push(Bytes.fromHexString("0xf00ba3")))
        this.add(Push(Bytes.fromHexString("0xf000")))
      }
    )
    assertEquals(Bytes.fromHexString("0x63deadbeef62f00ba361f000"), code.toBytes())
    val codeRead = Code.read(code.toBytes())
    assertEquals(3, codeRead.instructions.size)
    assertEquals(Bytes.fromHexString("0xdeadbeef"), (codeRead.instructions[0] as Push).bytesToPush)
    assertEquals(Bytes.fromHexString("0xf00ba3"), (codeRead.instructions[1] as Push).bytesToPush)
    assertEquals(Bytes.fromHexString("0xf000"), (codeRead.instructions[2] as Push).bytesToPush)
  }

  @Test
  fun decodeExistingContract() {
    // decode the contract:
    val ba = CodeTest::class.java.getResourceAsStream("/contract.txt")!!.readAllBytes()
    val contract = Bytes.fromHexString(String(ba))
    val code = assertDoesNotThrow {
      Code.read(contract)
    }
    assertEquals("PUSH 0x80", code.toString().splitToSequence('\n').first())
    val codeStr = code.toString()
    val reread = Code.read(code.toBytes())
    assertEquals(codeStr, reread.toString())
  }

  @Test
  fun testValidateUnderFlow() {
    val code = Code(
      buildList {
        this.add(Push(Bytes.fromHexString("0x4567")))
        this.add(Push(Bytes.fromHexString("0x456778")))
        this.add(Call)
      }
    )
    val err = code.validate()!!
    assertEquals(2, err.index)
    assertEquals(Error.STACK_UNDERFLOW, err.error)
    assertEquals(Call, err.instruction)
  }

  @Test
  fun testValidateInvalid() {
    val code = Code(
      buildList {
        this.add(Push(Bytes.fromHexString("0x4567")))
        this.add(Push(Bytes.fromHexString("0x456778")))
        this.add(Invalid(0xfe.toByte()))
        this.add(Push(Bytes.fromHexString("0x456778")))
      }
    )
    val err = code.validate()!!
    assertEquals(2, err.index)
    assertEquals(Error.HIT_INVALID_OPCODE, err.error)
    assertEquals(Invalid(0xfe.toByte()), err.instruction)
  }

  @Test
  fun testValidateOverFlow() {
    val code = Code(
      buildList {
        for (i in 0..1024) {
          this.add(Push(Bytes.fromHexString("0x4567")))
        }
      }
    )
    val err = code.validate()!!
    assertEquals(1024, err.index)
    assertEquals(Error.STACK_OVERFLOW, err.error)
  }

  @Test
  fun testCreateASimpleReturn() {
    val code = Code(
      buildList {
        this.add(Push(Bytes.wrap("hello world".toByteArray())))
        this.add(Push(Bytes.fromHexString("0x00")))
        this.add(Mstore)
        this.add(Push(Bytes.of("hello world".toByteArray().size)))
        this.add(Push(Bytes.of(32 - "hello world".toByteArray().size)))
        this.add(Return)
      }
    )
    assertNull(code.validate()?.error)
  }
}
