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
import org.apache.tuweni.evm.EVMState
import org.apache.tuweni.evm.EvmVm
import org.apache.tuweni.evm.HardFork
import org.apache.tuweni.evm.HostContext
import org.apache.tuweni.evm.TransactionalEVMHostContext
import org.apache.tuweni.evm.opcodes
import org.slf4j.LoggerFactory

data class Result(
  val status: EVMExecutionStatusCode? = null,
  val newCodePosition: Int? = null,
  val output: Bytes? = null,
  val validationStatus: EVMExecutionStatusCode? = null
)

/**
 * A listener that is executed at the end of each step.
 */
interface StepListener {
  /**
   * Checks the execution path
   *
   * @param executionPath the path of execution
   * @param state the state of the EVM
   * @return true to halt the execution
   */
  fun handleStep(executionPath: List<Byte>, state: EVMState): Boolean
}

class EvmVmImpl(val stepListener: StepListener? = null) : EvmVm {

  companion object {
    fun create(stepListener: StepListener? = null): EvmVm {
      return EvmVmImpl(stepListener)
    }
    val registry = OpcodeRegistry.create()
    val logger = LoggerFactory.getLogger(EvmVmImpl::class.java)
  }

  private val options = mutableMapOf<String, String>()

  override fun setOption(key: String, value: String) {
    options[key] = value
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
        return EVMResult(EVMExecutionStatusCode.INVALID_INSTRUCTION, hostContext, hostContext as TransactionalEVMHostContext, EVMState(gasManager, hostContext.getLogs(), stack, memory))
      }
      val currentOpcodeByte = code.get(current)
      current++
      val result = opcode.execute(gasManager, hostContext, stack, msg, code, current, memory, null)
      if (logger.isTraceEnabled) {
        val opCodeAsString = Bytes.of(currentOpcodeByte).toHexString()

        logger.trace(
          ">> OPCODE: ${opcodes[currentOpcodeByte] ?: opCodeAsString } " +
            "gas: ${gasManager.gasLeft()} cost: ${gasManager.lastGasCost()}"
        )
      }
      val state = EVMState(gasManager, (hostContext as TransactionalEVMHostContext).getLogs(), stack, memory, result?.output)

      if (result?.status != null) {
        traceExecution(executionPath)
        if (result.status == EVMExecutionStatusCode.SUCCESS && !gasManager.hasGasLeft()) {
          return EVMResult(EVMExecutionStatusCode.OUT_OF_GAS, hostContext, hostContext, state)
        }
        return EVMResult(result.status, hostContext, hostContext, state)
      }
      result?.newCodePosition?.let {
        current = result.newCodePosition
      }
      traceExecution(executionPath)
      if (!gasManager.hasGasLeft()) {
        return EVMResult(EVMExecutionStatusCode.OUT_OF_GAS, hostContext, hostContext, state)
      }
      if (stack.overflowed()) {
        return EVMResult(EVMExecutionStatusCode.STACK_OVERFLOW, hostContext, hostContext, state)
      }
      if (result?.validationStatus != null) {
        return EVMResult(result.validationStatus, hostContext, hostContext, state)
      }
      stepListener?.handleStep(executionPath, state)?.let {
        if (it) {
          return EVMResult(
            EVMExecutionStatusCode.HALTED,
            hostContext,
            hostContext,
            EVMState(gasManager, hostContext.getLogs(), stack, memory, result?.output)
          )
        }
      }
    }
    val state = EVMState(gasManager, (hostContext as TransactionalEVMHostContext).getLogs(), stack, memory)
    return EVMResult(EVMExecutionStatusCode.SUCCESS, hostContext, hostContext, state)
  }

  private fun traceExecution(executionPath: List<Byte>) {
    if (logger.isTraceEnabled) {
      logger.trace(executionPath.map { opcodes[it] ?: it.toString(16) }.joinToString(">"))
    }
  }

  override fun capabilities(): Int {
    TODO("Not yet implemented")
  }
}
