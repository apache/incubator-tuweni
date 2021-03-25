package org.apache.tuweni.evm.impl

import org.apache.tuweni.evm.EVMExecutionStatusCode
import org.apache.tuweni.evm.HostContext
import org.apache.tuweni.evm.impl.frontier.add

fun interface Opcode {
  fun execute(gasManager : GasManager, hostContext: HostContext, stack: Stack) : EVMExecutionStatusCode
}

class OpcodeRegistry(val opcodes: Map<Byte, Opcode>) {

  companion object {
    fun frontier() : OpcodeRegistry {
      return OpcodeRegistry(mapOf(Pair(0x01, add)))
    }
  }

}
