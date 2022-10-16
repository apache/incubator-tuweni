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
import org.apache.tuweni.eth.Log
import org.apache.tuweni.eth.precompiles.PrecompileContract
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.StateRepository
import org.apache.tuweni.evm.impl.GasManager
import org.apache.tuweni.evm.impl.Memory
import org.apache.tuweni.evm.impl.Stack
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.slf4j.LoggerFactory

/**
 * Types of EVM calls
 */
enum class CallKind(val number: Int) {
  CALL(0),
  DELEGATECALL(1),
  CALLCODE(2),
  CREATE(3),
  CREATE2(4),
  STATICCALL(5)
}

/**
 * EVM execution status codes
 */
enum class EVMExecutionStatusCode(val number: Int) {
  SUCCESS(0),
  FAILURE(1),
  REVERT(2),
  OUT_OF_GAS(3),
  INVALID_INSTRUCTION(4),
  UNDEFINED_INSTRUCTION(5),
  STACK_OVERFLOW(6),
  STACK_UNDERFLOW(7),
  BAD_JUMP_DESTINATION(8),
  INVALID_MEMORY_ACCESS(9),
  CALL_DEPTH_EXCEEDED(10),
  STATIC_MODE_VIOLATION(11),
  PRECOMPILE_FAILURE(12),
  CONTRACT_VALIDATION_FAILURE(13),
  ARGUMENT_OUT_OF_RANGE(14),
  WASM_UNREACHABLE_INSTRUCTION(15),
  WASM_TRAP(16),
  INTERNAL_ERROR(-1),
  REJECTED(-2),
  OUT_OF_MEMORY(-3),
  HALTED(-4);
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
  FRONTIER(0),
  HOMESTEAD(1),
  TANGERINE_WHISTLE(2),
  SPURIOUS_DRAGON(3),
  BYZANTIUM(4),
  CONSTANTINOPLE(5),
  PETERSBURG(6),
  ISTANBUL(7),
  BERLIN(8)
}

val latestHardFork = HardFork.BERLIN

/**
 * State of the EVM
 *
 * @param gasManager the gas manager
 * @param logs the logs of the EVM
 * @param stack the stack of the execution
 * @param memory the current memory allocation
 * @param output the output of the execution
 */
data class EVMState(
  val gasManager: GasManager,
  val logs: List<Log>,
  val stack: Stack,
  val memory: Memory,
  val output: Bytes? = null
) {

  /**
   * Dumps the EVM execution state into a byte array
   */
  fun toBytes(): Bytes = RLP.encodeList {
    it.writeString("gas")
    it.writeValue(gasManager.gas.toBytes())
    it.writeString("memory")
    it.writeValue(memory.memoryData ?: Bytes.EMPTY)
    it.writeString("stack")
    for (i in 0 until stack.size()) {
      it.writeValue(stack.get(i) ?: Bytes.EMPTY)
    }
    it.writeString("output")
    it.writeValue(output ?: Bytes.EMPTY)
    it.writeString("logs")
    for (log in logs) {
      it.writeValue(log.toBytes())
    }
  }
}

/**
 * Result of EVM execution
 * @param statusCode the execution result status
 * @param hostContext the context of changes
 * @param changes the set of changes
 * @param state the EVM state
 */
data class EVMResult(
  val statusCode: EVMExecutionStatusCode,
  val hostContext: HostContext,
  val changes: ExecutionChanges,
  val state: EVMState
)

/**
 * Message sent to the EVM for execution
 */
data class EVMMessage(
  val kind: CallKind,
  val flags: Int,
  val depth: Int = 0,
  val gas: Gas,
  val contract: Address,
  val destination: Address,
  val sender: Address,
  val origin: Address,
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
  private val transientRepository: StateRepository,
  private val repository: BlockchainRepository,
  val precompiles: Map<Address, PrecompileContract>,
  private val evmVmFactory: () -> EvmVm,
  private val options: Map<String, String> = mapOf()
) {

  companion object {
    val logger = LoggerFactory.getLogger(EthereumVirtualMachine::class.java)

    private val MAX_NONCE = UInt256.fromHexString("0xffffffffffffffff")
  }

  private var vm: EvmVm? = null

  private fun vm() = vm!!

  /**
   * Start the EVM
   */
  suspend fun start() {
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
  suspend fun stop() {
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
   * @param chainId the ID of the current chain
   * @param callKind the type of call
   * @param revision the hard fork revision in which to execute
   * @return the result of the execution
   */
  suspend fun execute(
    sender: Address,
    destination: Address,
    value: Bytes,
    code: Bytes,
    inputData: Bytes,
    gas: Gas,
    gasPrice: Wei,
    currentCoinbase: Address,
    currentNumber: UInt256,
    currentTimestamp: UInt256,
    currentGasLimit: Long,
    currentDifficulty: UInt256,
    chainId: UInt256,
    callKind: CallKind,
    revision: HardFork = latestHardFork,
    depth: Int = 0
  ): EVMResult {
    val hostContext = TransactionalEVMHostContext(
      repository,
      transientRepository,
      this,
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
      currentDifficulty,
      chainId
    )
    val result =
      executeInternal(
        sender,
        sender,
        destination,
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

  internal suspend fun executeInternal(
    origin: Address,
    sender: Address,
    destination: Address,
    contractAddress: Address,
    value: Bytes,
    code: Bytes,
    inputData: Bytes,
    gas: Gas,
    callKind: CallKind,
    revision: HardFork = latestHardFork,
    depth: Int = 0,
    hostContext: HostContext
  ): EVMResult {
    var gasAvailable = gas
    if (depth > 1024) {
      val gasManager = GasManager(gas)
      return EVMResult(EVMExecutionStatusCode.CALL_DEPTH_EXCEEDED, hostContext, NoOpExecutionChanges, EVMState(gasManager, listOf(), Stack(), Memory(), null))
    }
    val senderNonce = hostContext.getNonce(sender)

    if (senderNonce >= MAX_NONCE) {
      return EVMResult(EVMExecutionStatusCode.REJECTED, hostContext, NoOpExecutionChanges, EVMState(GasManager(gas), listOf(), Stack(), Memory(), null))
    }

    if (callKind == CallKind.CREATE || callKind == CallKind.CREATE2) {
      val codeDepositGasFee = UInt256.valueOf(code.size() * 200L)
      if (gas < codeDepositGasFee) {
        return EVMResult(EVMExecutionStatusCode.REJECTED, hostContext, NoOpExecutionChanges, EVMState(GasManager(gas), listOf(), Stack(), Memory(), null))
      }
      gasAvailable = gasAvailable.subtract(codeDepositGasFee)
    }

    val precompile = precompiles[contractAddress]

    if (!value.isZero && !options.containsKey("DISABLE_TRANSFER_VALUE")) {
      val destinationBalance = hostContext.getBalance(destination)
      val senderBalance = hostContext.getBalance(sender)
      val amount = Wei.valueOf(UInt256.fromBytes(value))
      if (senderBalance.toUInt256() < amount.toUInt256()) {
        return EVMResult(EVMExecutionStatusCode.REJECTED, hostContext, NoOpExecutionChanges, EVMState(GasManager(gas), listOf(), Stack(), Memory(), null))
      }
      hostContext.setBalance(sender, senderBalance.subtract(amount))
      hostContext.setBalance(destination, destinationBalance.add(amount))
    }

    if (precompile != null) {
      if (callKind == CallKind.CREATE || callKind == CallKind.CREATE2) {
        return EVMResult(EVMExecutionStatusCode.REJECTED, hostContext, NoOpExecutionChanges, EVMState(GasManager(gas), listOf(), Stack(), Memory(), null))
      }
      logger.trace("Executing precompile $contractAddress")
      val result = precompile.run(inputData)
      val gasManager = GasManager(gasAvailable)
      gasManager.add(result.gas)
      return EVMResult(if (result.output == null) EVMExecutionStatusCode.PRECOMPILE_FAILURE else EVMExecutionStatusCode.SUCCESS, hostContext, NoOpExecutionChanges, EVMState(gasManager, listOf(), Stack(), Memory(), result.output))
    } else {
      val msg =
        EVMMessage(
          callKind, 0, depth, gas, contractAddress, destination, sender, origin, inputData,
          value
        )

      return vm().execute(
        hostContext,
        revision,
        msg,
        code
      )
    }
  }

  /**
   * Provides the capabilities exposed by the underlying EVM library
   *
   * @return the EVM capabilities
   */
  fun capabilities(): Int = vm().capabilities()
}

/**
 * This interface represents the callback functions must be implemented in order to interface with
 * the EVM.
 */
interface HostContext {
  /**
   * Check account existence function.
   *
   *
   * This function is used by the VM to check if there exists an account at given address.
   *
   * @param address The address of the account the query is about.
   * @return true if exists, false otherwise.
   */
  suspend fun accountExists(address: Address): Boolean

  /**
   * Get repository storage function.
   *
   *
   * This function is used by a VM to query the given account storage entry.
   *
   * @param address The address of the account.
   * @param key The index of the account's storage entry.
   * @return The storage value at the given storage key or null bytes if the account does not exist.
   */
  suspend fun getRepositoryStorage(address: Address, key: Bytes32): Bytes?

  /**
   * Get storage function.
   *
   *
   * This function is used by a VM to query first the transaction changes, and then the given account storage entry.
   *
   * @param address The address of the account.
   * @param key The index of the account's storage entry.
   * @return The storage value at the given storage key or null bytes if the account does not exist.
   */
  suspend fun getStorage(address: Address, key: Bytes): Bytes32?

  /**
   * Set storage function.
   *
   *
   * This function is used by a VM to update the given account storage entry. The VM MUST make
   * sure that the account exists. This requirement is only a formality because VM implementations
   * only modify storage of the account of the current execution context (i.e. referenced by
   * message::destination).
   *
   * @param address The address of the account.
   * @param key The index of the storage entry.
   * @param value The value to be stored.
   * @return The effect on the storage item.
   */
  suspend fun setStorage(address: Address, key: Bytes, value: Bytes): Int

  /**
   * Get balance function.
   *
   *
   * This function is used by a VM to query the balance of the given account.
   *
   * @param address The address of the account.
   * @return The balance of the given account or 0 if the account does not exist.
   */
  suspend fun getBalance(address: Address): Wei

  /**
   * Get code size function.
   *
   *
   * This function is used by a VM to get the size of the code stored in the account at the given
   * address.
   *
   * @param address The address of the account.
   * @return The size of the code in the account or 0 if the account does not exist.
   */
  suspend fun getCodeSize(address: Address): Int

  /**
   * Get code hash function.
   *
   *
   * This function is used by a VM to get the keccak256 hash of the code stored in the account at
   * the given address. For existing accounts not having a code, this function returns keccak256
   * hash of empty data.
   *
   * @param address The address of the account.
   * @return The hash of the code in the account or null bytes if the account does not exist.
   */
  suspend fun getCodeHash(address: Address): Bytes32

  /**
   * Get account nonce
   *
   *
   * This function is used by a VM to get the nonce of the account at
   * the given address.
   *
   * @param address The address of the account.
   * @return The nonce of the accountt.
   */
  suspend fun getNonce(address: Address): UInt256

  /**
   * Copy code function.
   *
   *
   * This function is used by an EVM to request a copy of the code of the given account to the
   * memory buffer provided by the EVM. The Client MUST copy the requested code, starting with the
   * given offset, to the provided memory buffer up to the size of the buffer or the size of the
   * code, whichever is smaller.
   *
   * @param address The address of the account.
   * @return A copy of the requested code.
   */
  suspend fun getCode(address: Address): Bytes

  /**
   * Selfdestruct function.
   *
   *
   * This function is used by an EVM to SELFDESTRUCT given contract. The execution of the
   * contract will not be stopped, that is up to the EVM.
   *
   * @param address The address of the contract to be selfdestructed.
   * @param beneficiary The address where the remaining ETH is going to be transferred.
   */
  suspend fun selfdestruct(address: Address, beneficiary: Address)

  /**
   * This function supports EVM calls.
   *
   * @param msg The call parameters.
   * @return The result of the call.
   */
  suspend fun call(evmMessage: EVMMessage): EVMResult

  /**
   * This function supports EVM create calls.
   *
   * @param msg The create parameters.
   * @return The result of the call.
   */
  suspend fun create(evmMessage: EVMMessage, code: Bytes): EVMResult

  /**
   * Get transaction context function.
   *
   *
   * This function is used by an EVM to retrieve the transaction and block context.
   *
   * @return The transaction context.
   */
  fun getTxContext(): Bytes?

  /**
   * Get block hash function.
   *
   *
   * This function is used by a VM to query the hash of the header of the given block. If the
   * information about the requested block is not available, then this is signalled by returning
   * zeroed bytes.
   *
   * @param number The block hash.
   * @return The block hash or zeroed bytes if the information about the block is not available.
   */
  fun getBlockHash(number: UInt256): Bytes32

  /**
   * Log function.
   *
   *
   * This function is used by an EVM to inform about a LOG that happened during an EVM bytecode
   * execution.
   *
   * @param address The address of the contract that generated the log.
   * @param data The unindexed data attached to the log.
   * @param dataSize The length of the data.
   * @param topics The list of topics attached to the log.
   */
  fun emitLog(address: Address, data: Bytes, topics: List<Bytes32>)

  /**
   * Returns true if the account was warmed up.
   */
  fun warmUpAccount(address: Address): Boolean

  /**
   * Returns true if the storage slot was warmed up.
   */
  fun warmUpStorage(address: Address, key: UInt256): Boolean

  /**
   * Provides the gas price of the transaction.
   */
  fun getGasPrice(): Wei

  /**
   * Provides the timestamp of the transaction
   */
  fun timestamp(): UInt256
  fun getGasLimit(): Long
  fun getBlockNumber(): UInt256
  fun getBlockHash(): Bytes32
  fun getCoinbase(): Address
  fun getDifficulty(): UInt256
  fun getChaindId(): UInt256
  suspend fun setBalance(address: Address, balance: Wei)
  fun addRefund(address: Address, refund: Wei)
  fun addRefund(address: Address, refund: Long)
  suspend fun isEmptyAccount(address: Address): Boolean
  suspend fun incrementNonce(address: Address): UInt256
}

interface EvmVm {
  fun setOption(key: String, value: String)
  fun version(): String
  suspend fun close()
  suspend fun execute(hostContext: HostContext, fork: HardFork, msg: EVMMessage, code: Bytes): EVMResult
  fun capabilities(): Int
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
  Pair(0x1b, "shl"),
  Pair(0x1c, "shr"),
  Pair(0x1d, "sar"),
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
  Pair(0x3c, "extcodecopy"),
  Pair(0x3d, "returndatasize"),
  Pair(0x3e, "returndatacopy"),
  Pair(0x3f, "extcodehash"),
  Pair(0x40, "blockhash"),
  Pair(0x41, "coinbase"),
  Pair(0x42, "timestamp"),
  Pair(0x43, "number"),
  Pair(0x44, "difficulty"),
  Pair(0x45, "gaslimit"),
  Pair(0x46, "chainid"),
  Pair(0x47, "selfbalance"),
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
  Pair(0x80.toByte(), "dup1"),
  Pair(0x81.toByte(), "dup2"),
  Pair(0x82.toByte(), "dup3"),
  Pair(0x83.toByte(), "dup4"),
  Pair(0x84.toByte(), "dup5"),
  Pair(0x85.toByte(), "dup6"),
  Pair(0x86.toByte(), "dup7"),
  Pair(0x87.toByte(), "dup8"),
  Pair(0x88.toByte(), "dup9"),
  Pair(0x89.toByte(), "dup10"),
  Pair(0x8a.toByte(), "dup11"),
  Pair(0x8b.toByte(), "dup12"),
  Pair(0x8c.toByte(), "dup13"),
  Pair(0x8d.toByte(), "dup14"),
  Pair(0x8e.toByte(), "dup15"),
  Pair(0x8f.toByte(), "dup16"),
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
  Pair(0xa0.toByte(), "log0"),
  Pair(0xa1.toByte(), "log1"),
  Pair(0xa2.toByte(), "log2"),
  Pair(0xa3.toByte(), "log3"),
  Pair(0xa4.toByte(), "log4"),
  Pair(0xf0.toByte(), "create"),
  Pair(0xf1.toByte(), "call"),
  Pair(0xf2.toByte(), "callcode"),
  Pair(0xf3.toByte(), "return"),
  Pair(0xf4.toByte(), "delegatecall"),
  Pair(0xf5.toByte(), "create2"),
  Pair(0xf6.toByte(), "unused"),
  Pair(0xf7.toByte(), "unused"),
  Pair(0xf8.toByte(), "unused"),
  Pair(0xf9.toByte(), "unused"),
  Pair(0xfa.toByte(), "staticcall"),
  Pair(0xfb.toByte(), "unused"),
  Pair(0xfd.toByte(), "revert"),
  Pair(0xfe.toByte(), "invalid"),
  Pair(0xff.toByte(), "selfdestruct")
)

/**
 * EVM transaction changes
 */
interface ExecutionChanges {

  /**
   * Logs emitted during execution
   */
  fun getLogs(): List<Log>

  /**
   * Lists of accounts to destroy
   */
  fun accountsToDestroy(): List<Address>
}

object NoOpExecutionChanges : ExecutionChanges {
  override fun getLogs(): List<Log> = listOf()

  override fun accountsToDestroy(): List<Address> = listOf()
}
