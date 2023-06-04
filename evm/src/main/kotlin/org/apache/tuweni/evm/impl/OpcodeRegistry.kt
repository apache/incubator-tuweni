// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.evm.impl

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.evm.EVMMessage
import org.apache.tuweni.evm.HardFork
import org.apache.tuweni.evm.HostContext

fun interface Opcode {
  suspend fun execute(
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
      val forks = mapOf(
        Pair(HardFork.FRONTIER, org.apache.tuweni.evm.impl.frontier.frontierOpcodes),
        Pair(HardFork.HOMESTEAD, org.apache.tuweni.evm.impl.homestead.homesteadOpcodes),
        Pair(HardFork.TANGERINE_WHISTLE, org.apache.tuweni.evm.impl.tangerineWhistle.tangerineWhistleOpcodes),
        Pair(HardFork.SPURIOUS_DRAGON, org.apache.tuweni.evm.impl.spuriousDragon.spuriousDragonOpcodes),
        Pair(HardFork.BYZANTIUM, org.apache.tuweni.evm.impl.bizantium.bizantiumOpcodes),
        Pair(HardFork.CONSTANTINOPLE, org.apache.tuweni.evm.impl.constantinople.constantinopleOpcodes),
        Pair(HardFork.PETERSBURG, org.apache.tuweni.evm.impl.petersburg.petersburgOpcodes),
        Pair(HardFork.ISTANBUL, org.apache.tuweni.evm.impl.istanbul.istanbulOpcodes),
        Pair(HardFork.BERLIN, org.apache.tuweni.evm.impl.berlin.berlinOpcodes),
      )
      return OpcodeRegistry(forks)
    }
  }
}
