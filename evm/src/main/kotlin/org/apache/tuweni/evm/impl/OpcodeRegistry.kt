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
import org.apache.tuweni.evm.impl.berlin.call
import org.apache.tuweni.evm.impl.berlin.callcode
import org.apache.tuweni.evm.impl.berlin.calldatacopy
import org.apache.tuweni.evm.impl.berlin.calldataload
import org.apache.tuweni.evm.impl.berlin.calldatasize
import org.apache.tuweni.evm.impl.berlin.caller
import org.apache.tuweni.evm.impl.berlin.callvalue
import org.apache.tuweni.evm.impl.berlin.codecopy
import org.apache.tuweni.evm.impl.berlin.codesize
import org.apache.tuweni.evm.impl.berlin.coinbase
import org.apache.tuweni.evm.impl.berlin.delegatecall
import org.apache.tuweni.evm.impl.berlin.difficulty
import org.apache.tuweni.evm.impl.berlin.div
import org.apache.tuweni.evm.impl.berlin.dup
import org.apache.tuweni.evm.impl.berlin.eq
import org.apache.tuweni.evm.impl.berlin.exp
import org.apache.tuweni.evm.impl.berlin.extcodehash
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
import org.apache.tuweni.evm.impl.berlin.returndatacopy
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
import org.apache.tuweni.evm.impl.berlin.extcodecopy
import org.apache.tuweni.evm.impl.berlin.returndatasize
import org.apache.tuweni.evm.impl.berlin.sar
import org.apache.tuweni.evm.impl.berlin.shl
import org.apache.tuweni.evm.impl.berlin.shr
import org.apache.tuweni.evm.impl.berlin.staticcall

fun interface Opcode {
  fun execute(
    gasManager: GasManager,
    hostContext: HostContext,
    stack: Stack,
    msg: EVMMessage,
    code: Bytes,
    currentIndex: Int,
    memory: Memory,
    callResult: Result?,
  ): Result?
}

class OpcodeRegistry(val opcodes: Map<HardFork, Map<Byte, Opcode>>) {
  fun get(fork: HardFork, opcode: Byte): Opcode? {
    return opcodes[fork]?.get(opcode)
  }

  companion object {
    fun create(): OpcodeRegistry {
      val berlinOpcodes = mutableMapOf<Byte, Opcode>()
      berlinOpcodes[0x00] = stop
      berlinOpcodes[0x01] = add
      berlinOpcodes[0x02] = mul
      berlinOpcodes[0x03] = sub
      berlinOpcodes[0x04] = div
      berlinOpcodes[0x05] = sdiv
      berlinOpcodes[0x06] = mod
      berlinOpcodes[0x07] = smod
      berlinOpcodes[0x08] = addmod
      berlinOpcodes[0x09] = mulmod
      berlinOpcodes[0x10] = lt
      berlinOpcodes[0x11] = gt
      berlinOpcodes[0x12] = slt
      berlinOpcodes[0x13] = sgt
      berlinOpcodes[0x0a] = exp
      berlinOpcodes[0x0b] = signextend
      berlinOpcodes[0x14] = eq
      berlinOpcodes[0x15] = isZero
      berlinOpcodes[0x16] = and
      berlinOpcodes[0x17] = or
      berlinOpcodes[0x18] = xor
      berlinOpcodes[0x19] = not
      berlinOpcodes[0x1a] = byte
      berlinOpcodes[0x1b] = shl
      berlinOpcodes[0x1c] = shr
      berlinOpcodes[0x1d] = sar
      berlinOpcodes[0x20] = sha3
      berlinOpcodes[0x30] = address
      berlinOpcodes[0x31] = balance
      berlinOpcodes[0x32] = origin
      berlinOpcodes[0x33] = caller
      berlinOpcodes[0x34] = callvalue
      berlinOpcodes[0x35] = calldataload
      berlinOpcodes[0x36] = calldatasize
      berlinOpcodes[0x37] = calldatacopy
      berlinOpcodes[0x38] = codesize
      berlinOpcodes[0x39] = codecopy
      berlinOpcodes[0x3a] = gasPrice
      berlinOpcodes[0x3b] = extcodesize
      berlinOpcodes[0x3c] = extcodecopy
      berlinOpcodes[0x3d] = returndatasize
      berlinOpcodes[0x3e] = returndatacopy
      berlinOpcodes[0x3f] = extcodehash

      berlinOpcodes[0x40] = blockhash
      berlinOpcodes[0x41] = coinbase
      berlinOpcodes[0x42] = timestamp
      berlinOpcodes[0x43] = number
      berlinOpcodes[0x44] = difficulty
      berlinOpcodes[0x45] = gasLimit
      berlinOpcodes[0x50] = pop
      berlinOpcodes[0x51] = mload
      berlinOpcodes[0x52] = mstore
      berlinOpcodes[0x53] = mstore8
      berlinOpcodes[0x54] = sload
      berlinOpcodes[0x55] = sstore
      berlinOpcodes[0x56] = jump
      berlinOpcodes[0x57] = jumpi
      berlinOpcodes[0x58] = pc
      berlinOpcodes[0x59] = msize
      berlinOpcodes[0x5a] = gas
      berlinOpcodes[0x5b] = jumpdest
      berlinOpcodes[0xf0.toByte()] = org.apache.tuweni.evm.impl.berlin.create
      berlinOpcodes[0xf1.toByte()] = call
      berlinOpcodes[0xf2.toByte()] = callcode
      berlinOpcodes[0xf3.toByte()] = retuRn
      berlinOpcodes[0xf4.toByte()] = delegatecall
      berlinOpcodes[0xfa.toByte()] = staticcall
      berlinOpcodes[0xfe.toByte()] = invalid
      berlinOpcodes[0xff.toByte()] = selfdestruct
      for (i in 1..32) {
        berlinOpcodes[(0x60 + i - 1).toByte()] = push(i)
      }

      for (i in 1..16) {
        berlinOpcodes[(0x80 + i - 1).toByte()] = dup(i)
      }

      for (i in 1..16) {
        berlinOpcodes[(0x90 + i - 1).toByte()] = swap(i)
      }

      for (i in 0..4) {
        berlinOpcodes[(0xa0 + i).toByte()] = log(i)
      }
      val istanbulOpcodes = mutableMapOf<Byte, Opcode>()
      istanbulOpcodes[0x00] = org.apache.tuweni.evm.impl.istanbul.stop
      istanbulOpcodes[0x01] = org.apache.tuweni.evm.impl.istanbul.add
      istanbulOpcodes[0x02] = org.apache.tuweni.evm.impl.istanbul.mul
      istanbulOpcodes[0x03] = org.apache.tuweni.evm.impl.istanbul.sub
      istanbulOpcodes[0x04] = org.apache.tuweni.evm.impl.istanbul.div
      istanbulOpcodes[0x05] = org.apache.tuweni.evm.impl.istanbul.sdiv
      istanbulOpcodes[0x06] = org.apache.tuweni.evm.impl.istanbul.mod
      istanbulOpcodes[0x07] = org.apache.tuweni.evm.impl.istanbul.smod
      istanbulOpcodes[0x08] = org.apache.tuweni.evm.impl.istanbul.addmod
      istanbulOpcodes[0x09] = org.apache.tuweni.evm.impl.istanbul.mulmod
      istanbulOpcodes[0x10] = org.apache.tuweni.evm.impl.istanbul.lt
      istanbulOpcodes[0x11] = org.apache.tuweni.evm.impl.istanbul.gt
      istanbulOpcodes[0x12] = org.apache.tuweni.evm.impl.istanbul.slt
      istanbulOpcodes[0x13] = org.apache.tuweni.evm.impl.istanbul.sgt
      istanbulOpcodes[0x0a] = org.apache.tuweni.evm.impl.istanbul.exp
      istanbulOpcodes[0x0b] = org.apache.tuweni.evm.impl.istanbul.signextend
      istanbulOpcodes[0x14] = org.apache.tuweni.evm.impl.istanbul.eq
      istanbulOpcodes[0x15] = org.apache.tuweni.evm.impl.istanbul.isZero
      istanbulOpcodes[0x16] = org.apache.tuweni.evm.impl.istanbul.and
      istanbulOpcodes[0x17] = org.apache.tuweni.evm.impl.istanbul.or
      istanbulOpcodes[0x18] = org.apache.tuweni.evm.impl.istanbul.xor
      istanbulOpcodes[0x19] = org.apache.tuweni.evm.impl.istanbul.not
      istanbulOpcodes[0x1a] = org.apache.tuweni.evm.impl.istanbul.byte
      istanbulOpcodes[0x20] = org.apache.tuweni.evm.impl.istanbul.sha3
      istanbulOpcodes[0x30] = org.apache.tuweni.evm.impl.istanbul.address
      istanbulOpcodes[0x31] = org.apache.tuweni.evm.impl.istanbul.balance
      istanbulOpcodes[0x32] = org.apache.tuweni.evm.impl.istanbul.origin
      istanbulOpcodes[0x33] = org.apache.tuweni.evm.impl.istanbul.caller
      istanbulOpcodes[0x34] = org.apache.tuweni.evm.impl.istanbul.callvalue
      istanbulOpcodes[0x35] = org.apache.tuweni.evm.impl.istanbul.calldataload
      istanbulOpcodes[0x36] = org.apache.tuweni.evm.impl.istanbul.calldatasize
      istanbulOpcodes[0x37] = org.apache.tuweni.evm.impl.istanbul.calldatacopy
      istanbulOpcodes[0x38] = org.apache.tuweni.evm.impl.istanbul.codesize
      istanbulOpcodes[0x39] = org.apache.tuweni.evm.impl.istanbul.codecopy
      istanbulOpcodes[0x3a] = org.apache.tuweni.evm.impl.istanbul.gasPrice
      istanbulOpcodes[0x3b] = org.apache.tuweni.evm.impl.istanbul.extcodesize
      istanbulOpcodes[0x3c] = org.apache.tuweni.evm.impl.istanbul.extcodecopy
      istanbulOpcodes[0x3d] = org.apache.tuweni.evm.impl.istanbul.returndatasize
      istanbulOpcodes[0x3e] = org.apache.tuweni.evm.impl.istanbul.returndatacopy
      istanbulOpcodes[0x3f] = org.apache.tuweni.evm.impl.istanbul.extcodehash
      istanbulOpcodes[0x40] = org.apache.tuweni.evm.impl.istanbul.blockhash
      istanbulOpcodes[0x41] = org.apache.tuweni.evm.impl.istanbul.coinbase
      istanbulOpcodes[0x42] = org.apache.tuweni.evm.impl.istanbul.timestamp
      istanbulOpcodes[0x43] = org.apache.tuweni.evm.impl.istanbul.number
      istanbulOpcodes[0x44] = org.apache.tuweni.evm.impl.istanbul.difficulty
      istanbulOpcodes[0x45] = org.apache.tuweni.evm.impl.istanbul.gasLimit
      istanbulOpcodes[0x50] = org.apache.tuweni.evm.impl.istanbul.pop
      istanbulOpcodes[0x51] = org.apache.tuweni.evm.impl.istanbul.mload
      istanbulOpcodes[0x52] = org.apache.tuweni.evm.impl.istanbul.mstore
      istanbulOpcodes[0x53] = org.apache.tuweni.evm.impl.istanbul.mstore8
      istanbulOpcodes[0x54] = org.apache.tuweni.evm.impl.istanbul.sload
      istanbulOpcodes[0x55] = org.apache.tuweni.evm.impl.istanbul.sstore
      istanbulOpcodes[0x56] = org.apache.tuweni.evm.impl.istanbul.jump
      istanbulOpcodes[0x57] = org.apache.tuweni.evm.impl.istanbul.jumpi
      istanbulOpcodes[0x58] = org.apache.tuweni.evm.impl.istanbul.pc
      istanbulOpcodes[0x59] = org.apache.tuweni.evm.impl.istanbul.msize
      istanbulOpcodes[0x5a] = org.apache.tuweni.evm.impl.istanbul.gas
      istanbulOpcodes[0x5b] = org.apache.tuweni.evm.impl.istanbul.jumpdest
      istanbulOpcodes[0xf3.toByte()] = org.apache.tuweni.evm.impl.istanbul.retuRn
      istanbulOpcodes[0xfe.toByte()] = org.apache.tuweni.evm.impl.istanbul.invalid
      istanbulOpcodes[0xff.toByte()] = org.apache.tuweni.evm.impl.istanbul.selfdestruct
      for (i in 1..32) {
        istanbulOpcodes[(0x60 + i - 1).toByte()] = org.apache.tuweni.evm.impl.istanbul.push(i)
      }

      for (i in 1..16) {
        istanbulOpcodes[(0x80 + i - 1).toByte()] = org.apache.tuweni.evm.impl.istanbul.dup(i)
      }

      for (i in 1..16) {
        istanbulOpcodes[(0x90 + i - 1).toByte()] = org.apache.tuweni.evm.impl.istanbul.swap(i)
      }

      for (i in 0..4) {
        istanbulOpcodes[(0xa0 + i).toByte()] = org.apache.tuweni.evm.impl.istanbul.log(i)
      }
      val forks = mapOf(Pair(HardFork.BERLIN, berlinOpcodes), Pair(HardFork.ISTANBUL, istanbulOpcodes))
      return OpcodeRegistry(forks)
    }
  }
}
