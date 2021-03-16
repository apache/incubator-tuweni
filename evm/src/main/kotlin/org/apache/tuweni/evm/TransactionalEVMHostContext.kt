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
import org.apache.tuweni.eth.AccountState
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Log
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.trie.MerklePatriciaTrie
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.slf4j.LoggerFactory

/**
 * EVM context that records changes to the world state, so they can be applied atomically.
 */
class TransactionalEVMHostContext(
  val repository: BlockchainRepository,
  val ethereumVirtualMachine: EthereumVirtualMachine,
  val depth: Int,
  val sender: Address,
  val destination: Address,
  val value: Bytes,
  val code: Bytes,
  val gas: Gas,
  val gasPrice: Wei,
  val currentCoinbase: Address,
  val currentNumber: Long,
  val currentTimestamp: Long,
  val currentGasLimit: Long,
  val currentDifficulty: UInt256
) : HostContext {

  companion object {
    private val logger = LoggerFactory.getLogger(TransactionalEVMHostContext::class.java)
  }

  val accountChanges = HashMap<Address, HashMap<Bytes, Bytes32>>()
  val logs = mutableListOf<Log>()
  val accountsToDestroy = mutableListOf<Address>()
  val balanceChanges = HashMap<Address, Wei>()
  /**
   * Check account existence function.
   *
   *
   * This function is used by the VM to check if there exists an account at given address.
   *
   * @param address The address of the account the query is about.
   * @return true if exists, false otherwise.
   */
  override suspend fun accountExists(address: Address): Boolean {
    logger.trace("Entering accountExists")
    return accountChanges.containsKey(address) ||
      repository.accountsExists(address)
  }

  /**
   * Get storage function.
   *
   *
   * This function is used by a VM to query the given account storage entry.
   *
   * @param address The address of the account.
   * @param key The index of the account's storage entry.
   * @return The storage value at the given storage key or null bytes if the account does not exist.
   */
  override suspend fun getStorage(address: Address, keyBytes: Bytes): Bytes32 {
    logger.trace("Entering getStorage")
    val key = Bytes32.wrap(keyBytes)
    val value = accountChanges[address]?.get(key)
    logger.info("Found value $value")
    return value ?: Bytes32.ZERO
  }

  /**
   * Set storage function.
   *
   *
   * This function is used by a VM to update the given account storage entry. The VM MUST make
   * sure that the account exists. This requirement is only a formality because VM implementations
   * only modify storage of the account of the current execution context (i.e. referenced by
   * evmc_message::destination).
   *
   * @param address The address of the account.
   * @param key The index of the storage entry.
   * @param value The value to be stored.
   * @return The effect on the storage item:
   * The value of a storage item has been left unchanged: 0 -> 0 and X -> X.
   * EVMC_STORAGE_UNCHANGED = 0,
   * The value of a storage item has been modified: X -> Y.
   * EVMC_STORAGE_MODIFIED = 1,
   * A storage item has been modified after being modified before: X -> Y -> Z.
   * EVMC_STORAGE_MODIFIED_AGAIN = 2,
   * A new storage item has been added: 0 -> X.
   * EVMC_STORAGE_ADDED = 3,
   * A storage item has been deleted: X -> 0.
   * EVMC_STORAGE_DELETED = 4
   */
  override suspend fun setStorage(address: Address, key: Bytes, value: Bytes32): Int {
    logger.trace("Entering setStorage {} {} {}", address, key, value)
    var newAccount = false
    accountChanges.computeIfAbsent(address) {
      newAccount = true
      HashMap()
    }
    val map = accountChanges[address]!!
    val oldValue = map.get(key)
    val storageAdded = newAccount || oldValue == null
    val storageWasModifiedBefore = map.containsKey(key)
    val storageModified = !value.equals(oldValue)
    if (value.size() == 0) {
      map.remove(key)
      return 4
    }
    map.put(key, value)
    if (storageModified) {
      if (storageAdded) {
        return 3
      }
      if (storageWasModifiedBefore) {
        return 2
      }
      return 1
    }
    return 0
  }

  /**
   * Get balance function.
   *
   *
   * This function is used by a VM to query the balance of the given account.
   *
   * @param address The address of the account.
   * @return The balance of the given account or 0 if the account does not exist.
   */
  override suspend fun getBalance(address: Address): Bytes32 {
    logger.trace("Entering getBalance")

    val balance = balanceChanges[address]
    balance?.let {
      return it.toBytes()
    }
    val account = repository.getAccount(address)
    return account?.balance?.toBytes() ?: Bytes32.ZERO
  }

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
  override suspend fun getCodeSize(address: Address): Int {
    logger.trace("Entering getCodeSize")
    val code = repository.getAccountCode(address)
    return code?.size() ?: 0
  }

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
  override suspend fun getCodeHash(address: Address): Bytes32 {
    logger.trace("Entering getCodeHash")
    val account = repository.getAccount(address)

    return account?.codeHash ?: Bytes32.ZERO
  }

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
  override suspend fun getCode(address: Address): Bytes {
    logger.trace("Entering getCode")
    val code = repository.getAccountCode(address)
    return code ?: Bytes.EMPTY
  }

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
  override suspend fun selfdestruct(address: Address, beneficiary: Address) {
    logger.trace("Entering selfdestruct")
    accountsToDestroy.add(address)
    val account = repository.getAccount(address)
    val beneficiaryAccountState = repository.getAccount(beneficiary)
    if (beneficiaryAccountState === null) {
      repository.storeAccount(
        beneficiary,
        AccountState(
          UInt256.ZERO, Wei.valueOf(0),
          Hash.fromBytes(
            MerklePatriciaTrie.storingBytes().rootHash()
          ),
          Hash.hash(Bytes.EMPTY)
        )
      )
    }
    account?.apply {
      val balance = balanceChanges.putIfAbsent(beneficiary, account.balance)
      balance?.let {
        balanceChanges[beneficiary] = it.add(account.balance)
      }
    }
    logger.trace("Done selfdestruct")
  }

  /**
   * This function supports EVM calls.
   *
   * @param msg The call parameters.
   * @return The result of the call.
   */
  override suspend fun call(evmMessage: EVMMessage): EVMResult {
    logger.trace("Entering call")
    val result = ethereumVirtualMachine.executeInternal(
      evmMessage.sender,
      evmMessage.destination,
      evmMessage.value,
      Bytes.EMPTY,
      evmMessage.inputData,
      evmMessage.gas,
      depth = depth + 1,
      hostContext = this
    )
    return result
  }

  /**
   * Get transaction context function.
   *
   *
   * This function is used by an EVM to retrieve the transaction and block context.
   *
   * @return The transaction context.
   */
  override fun getTxContext(): Bytes? {
    logger.trace("Entering getTxContext")
    return Bytes.concatenate(
      gasPrice.toBytes(),
      sender, currentCoinbase, Bytes.ofUnsignedLong(currentNumber),
      Bytes.ofUnsignedLong(currentTimestamp),
      Bytes.ofUnsignedLong(currentGasLimit),
      currentDifficulty.toBytes(),
      UInt256.ONE.toBytes()
    )
  }

  /**
   * Get block hash function.
   *
   *
   * This function is used by a VM to query the hash of the header of the given block. If the
   * information about the requested block is not available, then this is signalled by returning
   * null bytes.
   *
   * @param number The block number.
   * @return The block hash or null bytes if the information about the block is not available.
   */
  override fun getBlockHash(number: Long): Bytes32 {
    logger.trace("Entering getBlockHash")
    val listOfCandidates = repository.findBlockByHashOrNumber(UInt256.valueOf(number).toBytes())
    return listOfCandidates.firstOrNull() ?: Bytes32.ZERO
  }

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
   * @param topics The the array of topics attached to the log.
   * @param topicCount The number of the topics. Valid values are between 0 and 4 inclusively.
   */
  override fun emitLog(address: Address, data: Bytes, topics: Array<Bytes>, topicCount: Int) {
    logger.trace("Entering emitLog")
    logs.add(Log(Address.fromBytes(Bytes.wrap(address)), Bytes.wrap(data), topics.map { Bytes32.wrap(it) }))
  }
}
