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
import kotlin.Byte

/**
 * An EVM instruction. It is made of an opcode, optionally followed by bytes to be consumed by the opcode execution.
 */
interface Instruction {

  fun toBytes(): Bytes

  fun stackItemsNeeded() = stackItemsConsumed()

  fun stackItemsConsumed(): Int

  fun stackItemsProduced(): Int

  fun end(): Boolean = false
}

data class InstructionModel(val opcode: Byte, val additionalBytesToRead: Int = 0, val creator: (code: Bytes, index: Int) -> Instruction)

/**
 * A registry of instructions that can be used to read code back into the DSL.
 */
object InstructionRegistry {
  val opcodes: Map<Byte, InstructionModel> = buildMap {
    this[0x00] = InstructionModel(0x00, 0) { _, _ -> Stop }
    this[0x01] = InstructionModel(0x01, 0) { _, _ -> Add }
    this[0x02] = InstructionModel(0x02, 0) { _, _ -> Mul }
    this[0x03] = InstructionModel(0x03, 0) { _, _ -> Sub }
    this[0x04] = InstructionModel(0x04, 0) { _, _ -> Div }
    this[0x05] = InstructionModel(0x05, 0) { _, _ -> SDiv }
    this[0x06] = InstructionModel(0x06, 0) { _, _ -> Mod }
    this[0x07] = InstructionModel(0x07, 0) { _, _ -> SMod }
    this[0x08] = InstructionModel(0x08, 0) { _, _ -> AddMod }
    this[0x09] = InstructionModel(0x09, 0) { _, _ -> MulMod }
    this[0x10] = InstructionModel(0x10, 0) { _, _ -> Lt }
    this[0x11] = InstructionModel(0x11, 0) { _, _ -> Gt }
    this[0x12] = InstructionModel(0x12, 0) { _, _ -> Slt }
    this[0x13] = InstructionModel(0x13, 0) { _, _ -> Sgt }
    this[0x0a] = InstructionModel(0x0a, 0) { _, _ -> Exp }
    this[0x0b] = InstructionModel(0x0b, 0) { _, _ -> SignExtend }
    this[0x14] = InstructionModel(0x14, 0) { _, _ -> Eq }
    this[0x15] = InstructionModel(0x15, 0) { _, _ -> IsZero }
    this[0x16] = InstructionModel(0x16, 0) { _, _ -> And }
    this[0x17] = InstructionModel(0x17, 0) { _, _ -> Or }
    this[0x18] = InstructionModel(0x18, 0) { _, _ -> Xor }
    this[0x19] = InstructionModel(0x19, 0) { _, _ -> Not }
    this[0x1a] = InstructionModel(0x1a, 0) { _, _ -> org.apache.tuweni.evmdsl.Byte }
    this[0x1b] = InstructionModel(0x1b, 0, { _, _ -> Shl })
    this[0x1c] = InstructionModel(0x1c, 0, { _, _ -> Shr })
    this[0x1d] = InstructionModel(0x1d, 0, { _, _ -> Sar })
    this[0x20] = InstructionModel(0x20, 0) { _, _ -> Sha3 }
    this[0x30] = InstructionModel(0x30, 0, { _, _ -> Address })
    this[0x31] = InstructionModel(0x31, 0) { _, _ -> Balance }
    this[0x32] = InstructionModel(0x32, 0) { _, _ -> Origin }
    this[0x33] = InstructionModel(0x33, 0) { _, _ -> Caller }
    this[0x34] = InstructionModel(0x34, 0) { _, _ -> CallValue }
    this[0x35] = InstructionModel(0x35, 0) { _, _ -> CallDataLoad }
    this[0x36] = InstructionModel(0x36, 0) { _, _ -> CallDataSize }
    this[0x37] = InstructionModel(0x37, 0, { _, _ -> CallDataCopy })
    this[0x38] = InstructionModel(0x38, 0, { _, _ -> CodeSize })
    this[0x39] = InstructionModel(0x39, 0, { _, _ -> CodeCopy })
    this[0x3a] = InstructionModel(0x3a, 0) { _, _ -> GasPrice }
    this[0x3b] = InstructionModel(0x3b, 0) { _, _ -> ExtCodeSize }
    this[0x3c] = InstructionModel(0x3c, 0) { _, _ -> ExtCodeCopy }
    this[0x3d] = InstructionModel(0x3d, 0) { _, _ -> ReturnDataSize }
    this[0x3e] = InstructionModel(0x3e, 0) { _, _ -> ReturnDataCopy }
    this[0x3f] = InstructionModel(0x3f, 0) { _, _ -> ExtCodeHash }
    this[0x40] = InstructionModel(0x40, 0) { _, _ -> BlockHash }
    this[0x41] = InstructionModel(0x41, 0) { _, _ -> Coinbase }
    this[0x42] = InstructionModel(0x42, 0) { _, _ -> Timestamp }
    this[0x43] = InstructionModel(0x43, 0) { _, _ -> Number }
    this[0x44] = InstructionModel(0x44, 0) { _, _ -> Difficulty }
    this[0x45] = InstructionModel(0x45, 0) { _, _ -> GasLimit }
    this[0x46] = InstructionModel(0x46, 0) { _, _ -> ChainId }
    this[0x47] = InstructionModel(0x47, 0) { _, _ -> SelfBalance }
    this[0x50] = InstructionModel(0x50, 0) { _, _ -> Pop }
    this[0x51] = InstructionModel(0x51, 0) { _, _ -> Mload }
    this[0x52] = InstructionModel(0x52, 0) { _, _ -> Mstore }
    this[0x53] = InstructionModel(0x53, 0) { _, _ -> Mstore8 }
    this[0x54] = InstructionModel(0x54, 0) { _, _ -> Sload }
    this[0x55] = InstructionModel(0x55, 0) { _, _ -> Sstore }
    this[0x56] = InstructionModel(0x56, 0) { _, _ -> Jump }
    this[0x57] = InstructionModel(0x57, 0) { _, _ -> Jumpi }
    this[0x58] = InstructionModel(0x58, 0) { _, _ -> Pc }
    this[0x59] = InstructionModel(0x59, 0) { _, _ -> Msize }
    this[0x5a] = InstructionModel(0x5a, 0) { _, _ -> Gas }
    this[0x5b] = InstructionModel(0x5b, 0) { _, _ -> JumpDest }
    this[0xf0.toByte()] = InstructionModel(0xf0.toByte(), 0) { _, _ -> Create }
    this[0xf1.toByte()] = InstructionModel(0xf1.toByte(), 0) { _, _ -> Call }
    this[0xf2.toByte()] = InstructionModel(0xf2.toByte(), 0) { _, _ -> CallCode }
    this[0xf3.toByte()] = InstructionModel(0xf3.toByte(), 0) { _, _ -> Return }
    this[0xf4.toByte()] = InstructionModel(0xf4.toByte(), 0) { _, _ -> DelegateCall }
    this[0xf5.toByte()] = InstructionModel(0xf5.toByte(), 0) { _, _ -> Create2 }
    this[0xfa.toByte()] = InstructionModel(0xfa.toByte(), 0) { _, _ -> StaticCall }
    this[0xfd.toByte()] = InstructionModel(0xfd.toByte(), 0) { _, _ -> Revert }
    this[0xfe.toByte()] = InstructionModel(0xfe.toByte(), 0) { _, _ -> Invalid(0xfe.toByte()) }
    this[0xff.toByte()] = InstructionModel(0xff.toByte(), 0) { _, _ -> SelfDestruct }
    for (i in 1..32) {
      val b = (0x60 + i - 1).toByte()
      this.put(b, InstructionModel(b, i) { code, index -> Push(code.slice(index, i)) })
    }

    for (i in 1..16) {
      val b = (0x80 + i - 1).toByte()
      this.put(b, InstructionModel(b, 0) { _, _ -> Dup(i) })
    }

    for (i in 1..16) {
      val b = (0x90 + i - 1).toByte()
      this.put(b, InstructionModel(b, 0) { _, _ -> Swap(i) })
    }

    for (i in 0..4) {
      val b = (0xa0 + i).toByte()
      this.put(b, InstructionModel(b, 0) { _, _ -> Log(i) })
    }
    for (i in 0x0c..0x0f) {
      val b = i.toByte()
      this.put(b, InstructionModel(b, 0) { _, _ -> Invalid(b) })
    }
    for (i in 0x21..0x2f) {
      val b = i.toByte()
      this.put(b, InstructionModel(b, 0) { _, _ -> Invalid(b) })
    }
    for (i in 0x49..0x4f) {
      val b = i.toByte()
      this.put(b, InstructionModel(b, 0) { _, _ -> Invalid(b) })
    }
    for (i in 0x5c..0x5f) {
      val b = i.toByte()
      this.put(b, InstructionModel(b, 0) { _, _ -> Invalid(b) })
    }
    for (i in 0xa5..0xaf) {
      val b = i.toByte()
      this.put(b, InstructionModel(b, 0) { _, _ -> Invalid(b) })
    }
    for (i in 0xb0..0xef) {
      val b = i.toByte()
      this.put(b, InstructionModel(b, 0) { _, _ -> Invalid(b) })
    }
    for (i in 0xf6..0xf9) {
      val b = i.toByte()
      this.put(b, InstructionModel(b, 0) { _, _ -> Invalid(b) })
    }
    for (i in 0xfb..0xfc) {
      val b = i.toByte()
      this.put(b, InstructionModel(b, 0) { _, _ -> Invalid(b) })
    }
  }
}

class Push(bytes: Bytes) : Instruction {

  val bytesToPush: Bytes
  init {
    if (bytes.size() > 32) {
      throw IllegalArgumentException("Push can push at most 32 bytes, ${bytes.size()} provided")
    }
    if (bytes.isEmpty) {
      throw IllegalArgumentException("Push requires at least one byte")
    }
    val trimmedBytes = bytes.trimLeadingZeros()
    bytesToPush = if (trimmedBytes.isEmpty) {
      Bytes.fromHexString("0x00")
    } else {
      trimmedBytes
    }
  }

  override fun toBytes(): Bytes = Bytes.wrap(Bytes.of((0x60 + bytesToPush.size() - 1).toByte()), bytesToPush)
  override fun toString(): String = "PUSH ${bytesToPush.toHexString()}"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

data class Invalid(val invalidByte: Byte) : Instruction {
  override fun toBytes(): Bytes = Bytes.of(invalidByte)
  override fun toString(): String = "INVALID ${Bytes.of(invalidByte)}"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 0
}

object Stop : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x00)
  override fun toString(): String = "STOP"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 0
}

object Add : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x01)
  override fun toString(): String = "ADD"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Mul : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x02)
  override fun toString(): String = "MUL"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Sub : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x03)
  override fun toString(): String = "SUB"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Div : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x04)
  override fun toString(): String = "DIV"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object SDiv : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x05)
  override fun toString(): String = "SDIV"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Mod : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x06)
  override fun toString(): String = "MOD"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object SMod : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x07)
  override fun toString(): String = "SMOD"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object AddMod : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x08)
  override fun toString(): String = "ADDMOD"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object MulMod : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x09)
  override fun toString(): String = "MULMOD"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Lt : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x10)
  override fun toString(): String = "LT"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Gt : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x11)
  override fun toString(): String = "GT"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Slt : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x12)
  override fun toString(): String = "SLT"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Sgt : Instruction {

  override fun toBytes(): Bytes = Bytes.of(0x13)
  override fun toString(): String = "SGT"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Exp : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x0a)
  override fun toString(): String = "EXP"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object SignExtend : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x0b)
  override fun toString(): String = "SIGNEXTEND"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Eq : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x14)
  override fun toString(): String = "EQ"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object IsZero : Instruction {

  override fun toBytes(): Bytes = Bytes.of(0x15)
  override fun toString(): String = "ISZERO"
  override fun stackItemsConsumed() = 1
  override fun stackItemsProduced() = 1
}

object And : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x16)
  override fun toString(): String = "AND"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Or : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x17)
  override fun toString(): String = "OR"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Xor : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x18)
  override fun toString(): String = "XOR"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Not : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x19)
  override fun toString(): String = "NOT"
  override fun stackItemsConsumed() = 1
  override fun stackItemsProduced() = 1
}

object Byte : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x1a)
  override fun toString(): String = "BYTE"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Shl : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x1b)
  override fun toString(): String = "SHL"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Shr : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x1c)
  override fun toString(): String = "SHR"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Sar : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x1d)
  override fun toString(): String = "SAR"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Sha3 : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x20)
  override fun toString(): String = "SHA3"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Address : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x30)
  override fun toString(): String = "ADDRESS"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object Balance : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x31)
  override fun toString(): String = "BALANCE"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object Origin : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x32)
  override fun toString(): String = "ORIGIN"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object Caller : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x33)
  override fun toString(): String = "CALLER"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object CallValue : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x34)
  override fun toString(): String = "CALLVALUE"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object CallDataLoad : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x35)
  override fun toString(): String = "CALLDATALOAD"
  override fun stackItemsConsumed() = 1
  override fun stackItemsProduced() = 1
}

object CallDataSize : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x36)
  override fun toString(): String = "CALLDATASIZE"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object CallDataCopy : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x37)
  override fun toString(): String = "CALLDATACOPY"
  override fun stackItemsConsumed() = 3
  override fun stackItemsProduced() = 0
}

object CodeSize : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x38)
  override fun toString(): String = "CODESIZE"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object CodeCopy : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x39)
  override fun toString(): String = "CODECOPY"
  override fun stackItemsConsumed() = 3
  override fun stackItemsProduced() = 0
}

object GasPrice : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x3a)
  override fun toString(): String = "GASPRICE"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object ExtCodeSize : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x3b)
  override fun toString(): String = "EXTCODESIZE"
  override fun stackItemsConsumed() = 1
  override fun stackItemsProduced() = 1
}

object ExtCodeCopy : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x3c)
  override fun toString(): String = "EXTCODECOPY"
  override fun stackItemsConsumed() = 4
  override fun stackItemsProduced() = 0
}

object ReturnDataSize : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x3d)
  override fun toString(): String = "RETURNDATASIZE"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object ReturnDataCopy : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x3e)
  override fun toString(): String = "RETURNDATACOPY"
  override fun stackItemsConsumed() = 3
  override fun stackItemsProduced() = 0
}

object ExtCodeHash : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x3f)
  override fun toString(): String = "EXTCODEHASH"
  override fun stackItemsConsumed() = 1
  override fun stackItemsProduced() = 1
}

object BlockHash : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x40)
  override fun toString(): String = "BLOCKHASH"
  override fun stackItemsConsumed() = 1
  override fun stackItemsProduced() = 1
}

object Coinbase : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x41)
  override fun toString(): String = "COINBASE"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object Timestamp : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x42)
  override fun toString(): String = "TIMESTAMP"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object Number : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x43)
  override fun toString(): String = "NUMBER"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object Difficulty : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x44)
  override fun toString(): String = "DIFFICULTY"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object GasLimit : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x45)
  override fun toString(): String = "GASLIMIT"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object ChainId : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x46)
  override fun toString(): String = "CHAINID"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object SelfBalance : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x47)
  override fun toString(): String = "SELFBALANCE"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object Pop : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x50)
  override fun toString(): String = "POP"
  override fun stackItemsConsumed() = 1
  override fun stackItemsProduced() = 0
}

object Mload : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x51)
  override fun toString(): String = "MLOAD"
  override fun stackItemsConsumed() = 1
  override fun stackItemsProduced() = 1
}

object Mstore : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x52)
  override fun toString(): String = "MSTORE"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 0
}

object Mstore8 : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x53)
  override fun toString(): String = "MSTORE8"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Sload : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x54)
  override fun toString(): String = "SLOAD"
  override fun stackItemsConsumed() = 1
  override fun stackItemsProduced() = 1
}

object Sstore : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x55)
  override fun toString(): String = "SSTORE"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 0
}

object Jump : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x56)
  override fun toString(): String = "JUMP"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 0
}

object Jumpi : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x57)
  override fun toString(): String = "JUMPI"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 1
}

object Pc : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x58)
  override fun toString(): String = "PC"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object Msize : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x59)
  override fun toString(): String = "MSIZE"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object Gas : Instruction {

  override fun toBytes(): Bytes = Bytes.of(0x5a)
  override fun toString(): String = "GAS"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 1
}

object JumpDest : Instruction {

  override fun toBytes(): Bytes = Bytes.of(0x5b)
  override fun toString(): String = "JUMPDEST"
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 0
}

object Create : Instruction {

  override fun toBytes(): Bytes = Bytes.of(0xf0)
  override fun toString(): String = "CREATE"
  override fun stackItemsConsumed() = 3
  override fun stackItemsProduced() = 1
}

object Call : Instruction {

  override fun toBytes(): Bytes = Bytes.of(0xf1)
  override fun toString(): String = "CALL"
  override fun stackItemsConsumed() = 7
  override fun stackItemsProduced() = 1
}

object CallCode : Instruction {

  override fun toBytes(): Bytes = Bytes.of(0xf2)
  override fun toString(): String = "CALLCODE"
  override fun stackItemsConsumed() = 7
  override fun stackItemsProduced() = 1
}

object Return : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0xf3)
  override fun toString(): String = "RETURN"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 0
  override fun end() = true
}

object DelegateCall : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0xf4)
  override fun toString(): String = "DELEGATECALL"
  override fun stackItemsConsumed() = 7
  override fun stackItemsProduced() = 1
}

object Create2 : Instruction {

  override fun toBytes(): Bytes = Bytes.of(0xf5)
  override fun toString(): String = "CREATE2"
  override fun stackItemsConsumed() = 4
  override fun stackItemsProduced() = 1
}

object StaticCall : Instruction {

  override fun toBytes(): Bytes = Bytes.of(0xfa)
  override fun toString(): String = "STATICCALL"
  override fun stackItemsConsumed() = 7
  override fun stackItemsProduced() = 1
}

object Revert : Instruction {

  override fun toBytes(): Bytes = Bytes.of(0xfd)
  override fun toString(): String = "REVERT"
  override fun stackItemsConsumed() = 2
  override fun stackItemsProduced() = 0
  override fun end() = true
}

object SelfDestruct : Instruction {

  override fun toBytes(): Bytes = Bytes.of(0xff)
  override fun toString(): String = "SELFDESTRUCT"
  override fun stackItemsConsumed() = 1
  override fun stackItemsProduced() = 0
}

class Dup(val dupIndex: Int) : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x80 + dupIndex - 1)
  override fun toString(): String = "DUP$dupIndex"
  override fun stackItemsNeeded() = dupIndex
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = dupIndex
}

class Swap(val swapIndex: Int) : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0x90 + swapIndex - 1)
  override fun toString(): String = "SWAP$swapIndex"
  override fun stackItemsNeeded() = swapIndex
  override fun stackItemsConsumed() = 0
  override fun stackItemsProduced() = 0
}

class Log(val logIndex: Int) : Instruction {
  override fun toBytes(): Bytes = Bytes.of(0xa0 + logIndex)
  override fun toString(): String = "LOG$logIndex"
  override fun stackItemsConsumed() = logIndex + 2
  override fun stackItemsProduced() = 0
}

data class Custom(val bytes: Bytes, val str: String, val consumed: Int, val produced: Int) : Instruction {
  override fun toBytes() = bytes
  override fun toString() = str
  override fun stackItemsConsumed() = consumed
  override fun stackItemsProduced() = produced
}
