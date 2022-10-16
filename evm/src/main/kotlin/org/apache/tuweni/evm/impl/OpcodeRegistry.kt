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

fun interface Opcode {
  suspend fun execute(
    gasManager: GasManager,
    hostContext: HostContext,
    stack: Stack,
    msg: EVMMessage,
    code: Bytes,
    currentIndex: Int,
    memory: Memory,
    callResult: Result?
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
        Pair(HardFork.BERLIN, org.apache.tuweni.evm.impl.berlin.berlinOpcodes)
      )
      return OpcodeRegistry(forks)
    }
  }
}
