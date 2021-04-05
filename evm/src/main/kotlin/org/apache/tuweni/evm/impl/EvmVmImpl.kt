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
import org.apache.tuweni.evm.opcodes
import org.slf4j.LoggerFactory

data class Result(
  val status: EVMExecutionStatusCode? = null,
  val newCodePosition: Int? = null,
  val output: Bytes? = null,
  val validationStatus: EVMExecutionStatusCode? = null,
)

class EvmVmImpl : EvmVm {

  companion object {
    fun create(): EvmVm {
      return EvmVmImpl()
    }
    val registry = OpcodeRegistry.create()
    val logger = LoggerFactory.getLogger(EvmVmImpl::class.java)
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
    logger.trace("Code: $code")
    val stack = Stack()
    var current = 0
    val gasManager = GasManager(msg.gas)
    val memory = Memory()
    val executionPath = mutableListOf<Byte>()
    while (current < code.size()) {
      if (logger.isTraceEnabled) {
        logger.trace("Stack contents (${stack.size()}):")
        for (i in (0 until stack.size())) {
          logger.trace("$i - ${stack.get(i)?.toHexString()}")
        }
      }
      executionPath.add(code.get(current))
      val opcode = registry.get(fork, code.get(current))
      if (opcode == null) {
        logger.error("Could not find opcode for ${code.slice(current, 1)} at position $current")
        return EVMResult(EVMExecutionStatusCode.INVALID_INSTRUCTION, gasManager, hostContext)
      }
      val currentOpcodeByte = code.get(current)
      current++
      val result = opcode.execute(gasManager, hostContext, stack, msg, code, current, memory)
      logger.trace(
        ">> OPCODE: ${opcodes[currentOpcodeByte] ?: currentOpcodeByte.toString(16)} " +
          "gas: ${gasManager.gasLeft()} cost: ${gasManager.lastGasCost()}"
      )
      if (result?.status != null) {
        if (logger.isTraceEnabled) {
          logger.trace(executionPath.map { opcodes[it] ?: it.toString(16) }.joinToString(">"))
        }
        if (result.status == EVMExecutionStatusCode.SUCCESS && !gasManager.hasGasLeft()) {
          return EVMResult(EVMExecutionStatusCode.OUT_OF_GAS, gasManager, hostContext)
        }
        return EVMResult(result.status, gasManager, hostContext, result.output)
      }
      result?.newCodePosition?.let {
        current = result.newCodePosition
      }
      if (!gasManager.hasGasLeft()) {
        return EVMResult(EVMExecutionStatusCode.OUT_OF_GAS, gasManager, hostContext)
      }
      if (stack.overflowed()) {
        return EVMResult(EVMExecutionStatusCode.STACK_OVERFLOW, gasManager, hostContext)
      }
      if (result?.validationStatus != null) {
        return EVMResult(result.validationStatus, gasManager, hostContext)
      }
    }
    if (logger.isTraceEnabled) {
      logger.trace(executionPath.map { opcodes[it] ?: it.toString(16) }.joinToString(">"))
    }
    return EVMResult(EVMExecutionStatusCode.SUCCESS, gasManager, hostContext)
  }

  override fun capabilities(): Int {
    TODO("Not yet implemented")
  }
}
