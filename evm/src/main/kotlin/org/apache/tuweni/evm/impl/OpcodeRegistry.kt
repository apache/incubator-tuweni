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
import org.apache.tuweni.bytes.MutableBytes
import org.apache.tuweni.evm.EVMExecutionStatusCode
import org.apache.tuweni.evm.EVMMessage
import org.apache.tuweni.evm.HardFork
import org.apache.tuweni.evm.HostContext
import org.apache.tuweni.evm.impl.frontier.add
import org.apache.tuweni.evm.impl.frontier.addmod
import org.apache.tuweni.evm.impl.frontier.address
import org.apache.tuweni.evm.impl.frontier.and
import org.apache.tuweni.evm.impl.frontier.balance
import org.apache.tuweni.evm.impl.frontier.blockhash
import org.apache.tuweni.evm.impl.frontier.byte
import org.apache.tuweni.evm.impl.frontier.calldatacopy
import org.apache.tuweni.evm.impl.frontier.calldataload
import org.apache.tuweni.evm.impl.frontier.calldatasize
import org.apache.tuweni.evm.impl.frontier.caller
import org.apache.tuweni.evm.impl.frontier.callvalue
import org.apache.tuweni.evm.impl.frontier.codecopy
import org.apache.tuweni.evm.impl.frontier.codesize
import org.apache.tuweni.evm.impl.frontier.coinbase
import org.apache.tuweni.evm.impl.frontier.difficulty
import org.apache.tuweni.evm.impl.frontier.div
import org.apache.tuweni.evm.impl.frontier.dup
import org.apache.tuweni.evm.impl.frontier.eq
import org.apache.tuweni.evm.impl.frontier.exp
import org.apache.tuweni.evm.impl.frontier.extcodesize
import org.apache.tuweni.evm.impl.frontier.gas
import org.apache.tuweni.evm.impl.frontier.gasLimit
import org.apache.tuweni.evm.impl.frontier.gasPrice
import org.apache.tuweni.evm.impl.frontier.gt
import org.apache.tuweni.evm.impl.frontier.invalid
import org.apache.tuweni.evm.impl.frontier.isZero
import org.apache.tuweni.evm.impl.frontier.jump
import org.apache.tuweni.evm.impl.frontier.jumpdest
import org.apache.tuweni.evm.impl.frontier.jumpi
import org.apache.tuweni.evm.impl.frontier.log
import org.apache.tuweni.evm.impl.frontier.lt
import org.apache.tuweni.evm.impl.frontier.mload
import org.apache.tuweni.evm.impl.frontier.mod
import org.apache.tuweni.evm.impl.frontier.msize
import org.apache.tuweni.evm.impl.frontier.mstore
import org.apache.tuweni.evm.impl.frontier.mstore8
import org.apache.tuweni.evm.impl.frontier.mul
import org.apache.tuweni.evm.impl.frontier.mulmod
import org.apache.tuweni.evm.impl.frontier.not
import org.apache.tuweni.evm.impl.frontier.number
import org.apache.tuweni.evm.impl.frontier.or
import org.apache.tuweni.evm.impl.frontier.origin
import org.apache.tuweni.evm.impl.frontier.pc
import org.apache.tuweni.evm.impl.frontier.pop
import org.apache.tuweni.evm.impl.frontier.push
import org.apache.tuweni.evm.impl.frontier.retuRn
import org.apache.tuweni.evm.impl.frontier.sdiv
import org.apache.tuweni.evm.impl.frontier.selfdestruct
import org.apache.tuweni.evm.impl.frontier.sgt
import org.apache.tuweni.evm.impl.frontier.sha3
import org.apache.tuweni.evm.impl.frontier.signextend
import org.apache.tuweni.evm.impl.frontier.sload
import org.apache.tuweni.evm.impl.frontier.slt
import org.apache.tuweni.evm.impl.frontier.smod
import org.apache.tuweni.evm.impl.frontier.sstore
import org.apache.tuweni.evm.impl.frontier.stop
import org.apache.tuweni.evm.impl.frontier.sub
import org.apache.tuweni.evm.impl.frontier.swap
import org.apache.tuweni.evm.impl.frontier.timestamp
import org.apache.tuweni.evm.impl.frontier.xor
import org.apache.tuweni.units.bigints.UInt256
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

data class Result(
  val status: EVMExecutionStatusCode? = null,
  val newCodePosition: Int? = null,
  val output: Bytes? = null,
  val validationStatus: EVMExecutionStatusCode? = null,
)

class Memory {

  companion object {
    val capacity = 1000000000
    val logger = LoggerFactory.getLogger(Memory::class.java)
  }

  var wordsSize = UInt256.ZERO
  var memoryData: MutableBytes? = null

  fun write(offset: UInt256, sourceOffset: UInt256, numBytes: UInt256, code: Bytes): Boolean {
    logger.trace("Write to memory at offset $offset, size $numBytes")
    val maxDistance = offset.add(numBytes)
    if (!offset.fitsInt() || !numBytes.fitsInt() || !maxDistance.fitsInt() || maxDistance.intValue() > capacity) {
      logger.warn("Memory write aborted, values too large")
      return false
    }
    var localMemoryData = memoryData
    if (localMemoryData == null) {
      localMemoryData = MutableBytes.wrapByteBuffer(ByteBuffer.allocate(maxDistance.intValue()))
    } else if (localMemoryData.size() < maxDistance.intValue()) {
      val buffer = ByteBuffer.allocate(maxDistance.intValue() * 2)
      buffer.put(localMemoryData.toArrayUnsafe())
      localMemoryData = MutableBytes.wrapByteBuffer(buffer)
    }
    memoryData = localMemoryData
    if (sourceOffset.fitsInt() && sourceOffset.intValue() < code.size()) {
      val maxCodeLength = code.size() - sourceOffset.intValue()
      val length = if (maxCodeLength < numBytes.intValue()) maxCodeLength else numBytes.intValue()
      logger.trace("Writing ${code.slice(sourceOffset.intValue(), maxCodeLength)}")
      memoryData!!.set(offset.intValue(), code.slice(sourceOffset.intValue(), length))
    }

    wordsSize = newSize(offset, numBytes)
    return true
  }

  fun allocatedBytes(): UInt256 {
    return wordsSize.multiply(32)
  }

  fun size(): UInt256 {
    return wordsSize
  }

  fun newSize(memOffset: UInt256, length: UInt256): UInt256 {

    val candidate = memOffset.add(length)
    if (candidate < memOffset || candidate < length) {
      return UInt256.MAX_VALUE
    }
    val candidateWords = candidate.divideCeil(32)
    return if (wordsSize > candidateWords) wordsSize else candidateWords
  }

  fun read(from: UInt256, length: UInt256): Bytes? {
    val max = from.add(length)
    if (!from.fitsInt() || !length.fitsInt() || !max.fitsInt()) {
      return null
    }
    val localMemoryData = memoryData
    if (localMemoryData != null) {
      if (localMemoryData.size() < max.intValue()) {
        val l = max.intValue() - localMemoryData.size()
        return Bytes.concatenate(
          localMemoryData.slice(from.intValue(), length.intValue() - l),
          Bytes.wrap(ByteArray(l))
        )
      }
      return localMemoryData.slice(from.intValue(), length.intValue())
    } else {
      return Bytes.wrap(ByteArray(length.intValue()))
    }
  }
}

fun interface Opcode {
  fun execute(
    gasManager: GasManager,
    hostContext: HostContext,
    stack: Stack,
    msg: EVMMessage,
    code: Bytes,
    currentIndex: Int,
    memory: Memory
  ): Result?
}

class OpcodeRegistry(val opcodes: Map<HardFork, Map<Byte, Opcode>>) {
  fun get(fork: HardFork, opcode: Byte): Opcode? {
    return opcodes[fork]?.get(opcode)
  }

  companion object {
    fun create(): OpcodeRegistry {
      val opcodes = mutableMapOf<Byte, Opcode>()
      opcodes[0x00] = stop
      opcodes[0x01] = add
      opcodes[0x02] = mul
      opcodes[0x03] = sub
      opcodes[0x04] = div
      opcodes[0x05] = sdiv
      opcodes[0x06] = mod
      opcodes[0x07] = smod
      opcodes[0x08] = addmod
      opcodes[0x09] = mulmod
      opcodes[0x10] = lt
      opcodes[0x11] = gt
      opcodes[0x12] = slt
      opcodes[0x13] = sgt
      opcodes[0x0a] = exp
      opcodes[0x0b] = signextend
      opcodes[0x14] = eq
      opcodes[0x15] = isZero
      opcodes[0x16] = and
      opcodes[0x17] = or
      opcodes[0x18] = xor
      opcodes[0x19] = not
      opcodes[0x1a] = byte
      opcodes[0x20] = sha3
      opcodes[0x30] = address
      opcodes[0x31] = balance
      opcodes[0x32] = origin
      opcodes[0x33] = caller
      opcodes[0x34] = callvalue
      opcodes[0x35] = calldataload
      opcodes[0x36] = calldatasize
      opcodes[0x37] = calldatacopy
      opcodes[0x38] = codesize
      opcodes[0x39] = codecopy
      opcodes[0x3a] = gasPrice
      opcodes[0x3b] = extcodesize
      opcodes[0x40] = blockhash
      opcodes[0x41] = coinbase
      opcodes[0x42] = timestamp
      opcodes[0x43] = number
      opcodes[0x44] = difficulty
      opcodes[0x45] = gasLimit
      opcodes[0x50] = pop
      opcodes[0x51] = mload
      opcodes[0x52] = mstore
      opcodes[0x53] = mstore8
      opcodes[0x54] = sload
      opcodes[0x55] = sstore
      opcodes[0x56] = jump
      opcodes[0x57] = jumpi
      opcodes[0x58] = pc
      opcodes[0x59] = msize
      opcodes[0x5a] = gas
      opcodes[0x5b] = jumpdest
      opcodes[0xf3.toByte()] = retuRn
      opcodes[0xfe.toByte()] = invalid
      opcodes[0xff.toByte()] = selfdestruct
      for (i in 1..32) {
        opcodes[(0x60 + i - 1).toByte()] = push(i)
      }

      for (i in 1..16) {
        opcodes[(0x80 + i - 1).toByte()] = dup(i)
      }

      for (i in 1..16) {
        opcodes[(0x90 + i - 1).toByte()] = swap(i)
      }

      for (i in 0..4) {
        opcodes[(0xa0 + i).toByte()] = log(i)
      }
      val forks = mapOf(Pair(HardFork.BERLIN, opcodes))
      return OpcodeRegistry(forks)
    }
  }
}

val opcodes = mapOf<Byte, String>(
  Pair(0x00, "stop"),
  Pair(0x01, "add"),
  Pair(0x02, "mul"),
  Pair(0x03, "sub"),
  Pair(0x04, "div"),
  Pair(0x05, "sdiv"),
  Pair(0x06, "mod"),
  Pair(0x06, "mod"),
  Pair(0x07, "smod"),
  Pair(0x08, "addmod"),
  Pair(0x09, "mulmod"),
  Pair(0x0a, "exp"),
  Pair(0x0b, "signextend"),
  Pair(0x10, "lt"),
  Pair(0x11, "gt"),
  Pair(0x12, "slt"),
  Pair(0x13, "sgt"),
  Pair(0x14, "eq"),
  Pair(0x15, "isZero"),
  Pair(0x16, "and"),
  Pair(0x17, "or"),
  Pair(0x18, "xor"),
  Pair(0x19, "not"),
  Pair(0x1a, "byte"),
  Pair(0x20, "sha3"),
  Pair(0x30, "address"),
  Pair(0x31, "balance"),
  Pair(0x32, "origin"),
  Pair(0x33, "caller"),
  Pair(0x34, "callvalue"),
  Pair(0x35, "calldataload"),
  Pair(0x36, "calldatasize"),
  Pair(0x37, "calldatacopy"),
  Pair(0x38, "codesize"),
  Pair(0x39, "codecopy"),
  Pair(0x3a, "gasPrice"),
  Pair(0x3b, "extcodesize"),
  Pair(0x40, "blockhash"),
  Pair(0x41, "coinbase"),
  Pair(0x42, "timestamp"),
  Pair(0x43, "number"),
  Pair(0x44, "difficulty"),
  Pair(0x45, "gaslimit"),
  Pair(0x50, "pop"),
  Pair(0x51, "mload"),
  Pair(0x52, "mstore"),
  Pair(0x53, "mstore8"),
  Pair(0x54, "sload"),
  Pair(0x55, "sstore"),
  Pair(0x56, "jump"),
  Pair(0x57, "jumpi"),
  Pair(0x58, "pc"),
  Pair(0x59, "msize"),
  Pair(0x5a, "gas"),
  Pair(0x5b, "jumpdest"),
  Pair(0x60, "push1"),
  Pair(0x61, "push2"),
  Pair(0x62, "push3"),
  Pair(0x63, "push4"),
  Pair(0x64, "push5"),
  Pair(0x65, "push6"),
  Pair(0x66, "push7"),
  Pair(0x67, "push8"),
  Pair(0x68, "push9"),
  Pair(0x69, "push10"),
  Pair(0x6a, "push11"),
  Pair(0x6b, "push12"),
  Pair(0x6c, "push13"),
  Pair(0x6d, "push14"),
  Pair(0x6e, "push15"),
  Pair(0x6f, "push16"),
  Pair(0x70, "push17"),
  Pair(0x71, "push18"),
  Pair(0x72, "push19"),
  Pair(0x73, "push20"),
  Pair(0x74, "push21"),
  Pair(0x75, "push22"),
  Pair(0x76, "push23"),
  Pair(0x77, "push24"),
  Pair(0x78, "push25"),
  Pair(0x79, "push26"),
  Pair(0x7a, "push27"),
  Pair(0x7b, "push28"),
  Pair(0x7c, "push29"),
  Pair(0x7d, "push30"),
  Pair(0x7e, "push31"),
  Pair(0x7f, "push32"),
  Pair(0x90.toByte(), "swap1"),
  Pair(0x91.toByte(), "swap2"),
  Pair(0x92.toByte(), "swap3"),
  Pair(0x93.toByte(), "swap4"),
  Pair(0x94.toByte(), "swap5"),
  Pair(0x95.toByte(), "swap6"),
  Pair(0x96.toByte(), "swap7"),
  Pair(0x97.toByte(), "swap8"),
  Pair(0x98.toByte(), "swap9"),
  Pair(0x99.toByte(), "swap10"),
  Pair(0x9a.toByte(), "swap11"),
  Pair(0x9b.toByte(), "swap12"),
  Pair(0x9c.toByte(), "swap13"),
  Pair(0x9d.toByte(), "swap14"),
  Pair(0x9e.toByte(), "swap15"),
  Pair(0x9f.toByte(), "swap16"),
  Pair(0xf3.toByte(), "return"),
  Pair(0xa0.toByte(), "log0"),
  Pair(0xa1.toByte(), "log1"),
  Pair(0xa2.toByte(), "log2"),
  Pair(0xa3.toByte(), "log3"),
  Pair(0xa4.toByte(), "log4"),
  Pair(0xfe.toByte(), "invalid"),
  Pair(0xff.toByte(), "selfdestruct"),
)
