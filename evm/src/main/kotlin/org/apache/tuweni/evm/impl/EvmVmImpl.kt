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
import org.apache.tuweni.evm.EVMExecutionStatusCode
import org.apache.tuweni.evm.EVMMessage
import org.apache.tuweni.evm.EVMResult
import org.apache.tuweni.evm.EvmVm
import org.apache.tuweni.evm.HardFork
import org.apache.tuweni.evm.HostContext

class EvmVmImpl() : EvmVm {

  companion object {
    fun create(): EvmVm {
      return EvmVmImpl()
    }
    val registry = mapOf(Pair(HardFork.FRONTIER, OpcodeRegistry.frontier()))
  }

  override fun setOption(key: String, value: String) {
    TODO("Not yet implemented")
  }

  override fun version(): String {
    return "0.0.1"
  }

  override suspend fun close() {

  }

  override suspend fun execute(hostContext: HostContext, fork: HardFork, msg: EVMMessage, code: Bytes): EVMResult {
    val stack = Stack()
    var current = 0
    val gasManager = GasManager(msg.gas)
    while (current < code.size()) {
      val opcode = registry[fork]?.opcodes?.get(code.get(current))
          ?: return EVMResult(EVMExecutionStatusCode.INVALID_INSTRUCTION, gasManager.gasLeft(), hostContext)
      val result = opcode.execute(gasManager, hostContext, stack)
      if (result != EVMExecutionStatusCode.SUCCESS) {
        return EVMResult(result, gasManager.gasLeft(), hostContext)
      }
      if (gasManager.gasLeft() < 0) {
        return EVMResult(EVMExecutionStatusCode.OUT_OF_GAS, gasManager.gasLeft(), hostContext)
      }
    }
    return EVMResult(EVMExecutionStatusCode.SUCCESS, gasManager.gasLeft(), hostContext)
  }

  override fun capabilities(): Int {
    TODO("Not yet implemented")
  }
}
