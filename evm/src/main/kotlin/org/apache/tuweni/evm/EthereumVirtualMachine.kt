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
package org.apache.tuweni.evm

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei

/**
 * Types of EVM calls
 */
enum class CallKind(val number: Int) {
  EVMC_CALL(0),
  EVMC_DELEGATECALL(1),
  EVMC_CALLCODE(2),
  EVMC_CREATE(3),
  EVMC_CREATE2(4)
}

/**
 * EVM execution status codes
 */
enum class EVMExecutionStatusCode(val number: Int) {
  EVMC_SUCCESS(0),
  EVMC_FAILURE(1),
  EVMC_REVERT(2),
  EVMC_OUT_OF_GAS(3),
  EVMC_INVALID_INSTRUCTION(4),
  EVMC_UNDEFINED_INSTRUCTION(5),
  EVMC_STACK_OVERFLOW(6),
  EVMC_STACK_UNDERFLOW(7),
  EVMC_BAD_JUMP_DESTINATION(8),
  EVMC_INVALID_MEMORY_ACCESS(9),
  EVMC_CALL_DEPTH_EXCEEDED(10),
  EVMC_STATIC_MODE_VIOLATION(11),
  EVMC_PRECOMPILE_FAILURE(12),
  EVMC_CONTRACT_VALIDATION_FAILURE(13),
  EVMC_ARGUMENT_OUT_OF_RANGE(14),
  EVMC_WASM_UNREACHABLE_INSTRUCTION(15),
  EVMC_WASM_TRAP(16),
  EVMC_INTERNAL_ERROR(-1),
  EVMC_REJECTED(-2),
  EVMC_OUT_OF_MEMORY(-3);
}

/**
 * Finds a code matching a number, or throw an exception if no matching code exists.
 * @param code the number to match
 * @return the execution code
 */
fun fromCode(code: Int): EVMExecutionStatusCode = EVMExecutionStatusCode.values().first {
  code == it.number
}

/**
 * Known hard fork revisions to execute against.
 */
enum class HardFork(val number: Int) {
  EVMC_FRONTIER(0),
  EVMC_HOMESTEAD(1),
  EVMC_TANGERINE_WHISTLE(2),
  EVMC_SPURIOUS_DRAGON(3),
  EVMC_BYZANTIUM(4),
  EVMC_CONSTANTINOPLE(5),
  EVMC_PETERSBURG(6),
  EVMC_ISTANBUL(7),
  EVMC_BERLIN(8),
  EVMC_MAX_REVISION(8)
}

/**
 * Result of EVM execution
 * @param statusCode the execution result status
 * @param gasLeft how much gas is left
 * @param hostContext the context of changes
 */
data class EVMResult(
  val statusCode: EVMExecutionStatusCode,
  val gasLeft: Long,
  val hostContext: TransactionalEVMHostContext
)

/**
 * Message sent to the EVM for execution
 */
data class EVMMessage(
  val kind: Int,
  val flags: Int,
  val depth: Int = 0,
  val gas: Gas,
  val destination: Address,
  val sender: Address,
  val inputData: Bytes,
  val value: Bytes,
  val createSalt: Bytes32 = Bytes32.ZERO
)

/**
 * An Ethereum Virtual Machine.
 *
 * @param repository the blockchain repository
 * @param evmVmFactory factory to create the EVM
 * @param options the options to set on the EVM, specific to the library
 */
class EthereumVirtualMachine(
  private val repository: BlockchainRepository,
  private val evmVmFactory: () -> EvmVm,
  private val options: Map<String, String> = mapOf()
) {

  private var vm: EvmVm? = null

  private fun vm() = vm!!

  /**
   * Start the EVM
   */
  fun start() {
    vm = evmVmFactory()
    options.forEach { (k, v) ->
      vm().setOption(k, v)
    }
  }

  /**
   * Provides the version of the EVM
   *
   * @return the version of the underlying EVM library
   */
  fun version(): String = vm().version()

  /**
   * Stop the EVM
   */
  fun stop() {
    vm().close()
  }

  /**
   * Execute an operation in the EVM.
   * @param sender the sender of the transaction
   * @param destination the destination of the transaction
   * @param code the code to execute
   * @param inputData the execution input
   * @param gas the gas available for the operation
   * @param gasPrice current gas price
   * @param currentCoinbase the coinbase address to reward
   * @param currentNumber current block number
   * @param currentTimestamp current block timestamp
   * @param currentGasLimit current gas limit
   * @param currentDifficulty block current total difficulty
   * @param callKind the type of call
   * @param revision the hard fork revision in which to execute
   * @return the result of the execution
   */
  fun execute(
    sender: Address,
    destination: Address,
    value: Bytes,
    code: Bytes,
    inputData: Bytes,
    gas: Gas,
    gasPrice: Wei,
    currentCoinbase: Address,
    currentNumber: Long,
    currentTimestamp: Long,
    currentGasLimit: Long,
    currentDifficulty: UInt256,
    callKind: CallKind = CallKind.EVMC_CALL,
    revision: HardFork = HardFork.EVMC_MAX_REVISION,
    depth: Int = 0
  ): EVMResult {
    val hostContext = TransactionalEVMHostContext(
      repository,
      this,
      depth,
      sender,
      destination,
      value,
      code,
      gas,
      gasPrice,
      currentCoinbase,
      currentNumber,
      currentTimestamp,
      currentGasLimit,
      currentDifficulty
    )
    val result =
      executeInternal(
        sender,
        destination,
        value,
        code,
        inputData,
        gas,
        callKind,
        revision,
        depth,
        hostContext
      )

    return result
  }

  internal fun executeInternal(
    sender: Address,
    destination: Address,
    value: Bytes,
    code: Bytes,
    inputData: Bytes,
    gas: Gas,
    callKind: CallKind = CallKind.EVMC_CALL,
    revision: HardFork = HardFork.EVMC_MAX_REVISION,
    depth: Int = 0,
    hostContext: HostContext
  ): EVMResult {
    val msg =
      EVMMessage(
        callKind.number, 0, depth, gas, destination, sender, inputData,
        value
      )

    return vm().execute(
      hostContext,
      revision.number,
      msg,
      code
    )
  }

  /**
   * Provides the capabilities exposed by the underlying EVM library
   *
   * @return the EVM capabilities
   */
  fun capabilities(): Int = vm().capabilities()
}
