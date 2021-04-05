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
import org.apache.tuweni.evm.EVMMessage
import org.apache.tuweni.evm.HardFork
import org.apache.tuweni.evm.HostContext
import org.apache.tuweni.evm.impl.berlin.add
import org.apache.tuweni.evm.impl.berlin.addmod
import org.apache.tuweni.evm.impl.berlin.address
import org.apache.tuweni.evm.impl.berlin.and
import org.apache.tuweni.evm.impl.berlin.balance
import org.apache.tuweni.evm.impl.berlin.blockhash
import org.apache.tuweni.evm.impl.berlin.byte
import org.apache.tuweni.evm.impl.berlin.calldatacopy
import org.apache.tuweni.evm.impl.berlin.calldataload
import org.apache.tuweni.evm.impl.berlin.calldatasize
import org.apache.tuweni.evm.impl.berlin.caller
import org.apache.tuweni.evm.impl.berlin.callvalue
import org.apache.tuweni.evm.impl.berlin.codecopy
import org.apache.tuweni.evm.impl.berlin.codesize
import org.apache.tuweni.evm.impl.berlin.coinbase
import org.apache.tuweni.evm.impl.berlin.difficulty
import org.apache.tuweni.evm.impl.berlin.div
import org.apache.tuweni.evm.impl.berlin.dup
import org.apache.tuweni.evm.impl.berlin.eq
import org.apache.tuweni.evm.impl.berlin.exp
import org.apache.tuweni.evm.impl.berlin.extcodesize
import org.apache.tuweni.evm.impl.berlin.gas
import org.apache.tuweni.evm.impl.berlin.gasLimit
import org.apache.tuweni.evm.impl.berlin.gasPrice
import org.apache.tuweni.evm.impl.berlin.gt
import org.apache.tuweni.evm.impl.berlin.invalid
import org.apache.tuweni.evm.impl.berlin.isZero
import org.apache.tuweni.evm.impl.berlin.jump
import org.apache.tuweni.evm.impl.berlin.jumpdest
import org.apache.tuweni.evm.impl.berlin.jumpi
import org.apache.tuweni.evm.impl.berlin.log
import org.apache.tuweni.evm.impl.berlin.lt
import org.apache.tuweni.evm.impl.berlin.mload
import org.apache.tuweni.evm.impl.berlin.mod
import org.apache.tuweni.evm.impl.berlin.msize
import org.apache.tuweni.evm.impl.berlin.mstore
import org.apache.tuweni.evm.impl.berlin.mstore8
import org.apache.tuweni.evm.impl.berlin.mul
import org.apache.tuweni.evm.impl.berlin.mulmod
import org.apache.tuweni.evm.impl.berlin.not
import org.apache.tuweni.evm.impl.berlin.number
import org.apache.tuweni.evm.impl.berlin.or
import org.apache.tuweni.evm.impl.berlin.origin
import org.apache.tuweni.evm.impl.berlin.pc
import org.apache.tuweni.evm.impl.berlin.pop
import org.apache.tuweni.evm.impl.berlin.push
import org.apache.tuweni.evm.impl.berlin.retuRn
import org.apache.tuweni.evm.impl.berlin.sdiv
import org.apache.tuweni.evm.impl.berlin.selfdestruct
import org.apache.tuweni.evm.impl.berlin.sgt
import org.apache.tuweni.evm.impl.berlin.sha3
import org.apache.tuweni.evm.impl.berlin.signextend
import org.apache.tuweni.evm.impl.berlin.sload
import org.apache.tuweni.evm.impl.berlin.slt
import org.apache.tuweni.evm.impl.berlin.smod
import org.apache.tuweni.evm.impl.berlin.sstore
import org.apache.tuweni.evm.impl.berlin.stop
import org.apache.tuweni.evm.impl.berlin.sub
import org.apache.tuweni.evm.impl.berlin.swap
import org.apache.tuweni.evm.impl.berlin.timestamp
import org.apache.tuweni.evm.impl.berlin.xor

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
