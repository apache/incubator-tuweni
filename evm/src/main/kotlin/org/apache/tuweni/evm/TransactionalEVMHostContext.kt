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
import org.apache.tuweni.eth.repository.StateRepository
import org.apache.tuweni.eth.repository.StateRepository.Companion.EMPTY_CODE_HASH
import org.apache.tuweni.eth.repository.StateRepository.Companion.EMPTY_STORAGE_HASH
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.slf4j.LoggerFactory
import java.math.BigInteger

/**
 * EVM context that records changes to the world state, so they can be applied atomically.
 */
class TransactionalEVMHostContext(
  val blockchainRepository: BlockchainRepository,
  val transientRepository: StateRepository,
  val ethereumVirtualMachine: EthereumVirtualMachine,
  val sender: Address,
  val destination: Address,
  val value: Bytes,
  val code: Bytes,
  val gas: Gas,
  private val gasPrice: Wei,
  val currentCoinbase: Address,
  val currentNumber: UInt256,
  val currentTimestamp: UInt256,
  val currentGasLimit: Long,
  val currentDifficulty: UInt256,
  val chainId: UInt256
) : HostContext, ExecutionChanges {

  companion object {
    private val logger = LoggerFactory.getLogger(TransactionalEVMHostContext::class.java)
  }

  private val logs = mutableListOf<Log>()
  val accountsToDestroy = mutableListOf<Address>()
  val warmedUpStorage = HashSet<Bytes>()

  init {
    warmedUpStorage.add(sender)
    warmedUpStorage.add(destination)
  }

  override fun getLogs(): List<Log> = logs

  override fun accountsToDestroy(): List<Address> = accountsToDestroy

  private val refunds = mutableMapOf<Address, BigInteger>()

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
    return transientRepository.accountsExists(address)
  }

  override suspend fun getRepositoryStorage(address: Address, key: Bytes32): Bytes? {
    logger.trace("Entering getRepositoryStorage")
    val value = blockchainRepository.getAccountStoreValue(address, key)
    logger.trace("key $key, value $value")
    return value
  }

  override suspend fun isEmptyAccount(address: Address): Boolean {
    logger.trace("Entering isEmptyAccount")
    val accountState = transientRepository.getAccount(address)
    return null == accountState || (accountState.balance.isZero && accountState.nonce.isZero && EMPTY_CODE_HASH.equals(accountState.codeHash) && EMPTY_STORAGE_HASH.equals(accountState.storageRoot))
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
  override suspend fun getStorage(address: Address, key: Bytes): Bytes32? {
    logger.trace("Entering getStorage")
    val value = transientRepository.getAccountStoreValue(address, Hash.hash(key))?.let { UInt256.fromBytes(RLP.decodeValue(it)) }
    logger.trace("key $key value $value")
    return value
  }

  /**
   * Increments the nonce of the account associated with the address.
   */
  override suspend fun incrementNonce(address: Address): UInt256 {
    val account = transientRepository.getAccount(address) ?: transientRepository.newAccountState()
    val newNonce = account.nonce.add(1)
    transientRepository.storeAccount(address, AccountState(newNonce, account.balance, account.storageRoot, account.codeHash))
    return newNonce
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
  override suspend fun setStorage(address: Address, key: Bytes, value: Bytes): Int {
    logger.trace("Entering setStorage {} {} {}", address, key, value)

    val hashKey = Hash.hash(key)
    val newAccount = transientRepository.accountsExists(address)
    val repositoryValue = blockchainRepository.getAccountStoreValue(address, hashKey)
    val oldValue = transientRepository.getAccountStoreValue(address, hashKey)
    val storageAdded = newAccount || oldValue == null
    val storageWasModifiedBefore = !(repositoryValue?.equals(oldValue) == true || oldValue == null)
    val storageModified = !(value == UInt256.ZERO && oldValue == null) && !value.equals(oldValue)
    if (!storageModified) {
      return 0
    }
    if (value.isEmpty) {
      transientRepository.deleteAccountStore(address, hashKey)
    } else {
      transientRepository.storeAccountValue(address, hashKey, RLP.encodeValue(value))
    }
    if (value.size() == 0) {
      return 4
    }
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
  override suspend fun getBalance(address: Address): Wei {
    logger.trace("Entering getBalance")

    val account = transientRepository.getAccount(address)
    return account?.balance ?: Wei.valueOf(0)
  }

  override suspend fun setBalance(address: Address, balance: Wei) {
    logger.trace("Entering setBalance $address with $balance")

    val account = transientRepository.getAccount(address) ?: transientRepository.newAccountState()
    val newAccount = AccountState(account.nonce, balance, account.storageRoot, account.codeHash, account.version)
    transientRepository.storeAccount(address, newAccount)
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
    val code = transientRepository.getAccountCode(address)
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
    val account = transientRepository.getAccount(address)

    return account?.codeHash ?: Bytes32.ZERO
  }

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
  override suspend fun getNonce(address: Address): UInt256 {
    logger.trace("Entering getNonce")
    val account = transientRepository.getAccount(address)

    return account?.nonce ?: UInt256.ONE
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
    val code = transientRepository.getAccountCode(address)
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
    val account = transientRepository.getAccount(address)
    account?.apply {
      val beneficiaryAccountState = transientRepository.getAccount(beneficiary) ?: transientRepository.newAccountState()
      transientRepository.storeAccount(
        beneficiary,
        AccountState(
          beneficiaryAccountState.nonce,
          beneficiaryAccountState.balance.add(account.balance),
          beneficiaryAccountState.storageRoot,
          beneficiaryAccountState.codeHash
        )
      )
      val resetBalance = AccountState(this.nonce, Wei.valueOf(0), this.storageRoot, this.codeHash, this.version)
      transientRepository.storeAccount(address, resetBalance)
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
    logger.trace("Entering call ${evmMessage.kind}")
    val code = transientRepository.getAccountCode(evmMessage.contract)
    val result = ethereumVirtualMachine.executeInternal(
      evmMessage.origin,
      evmMessage.sender,
      evmMessage.destination,
      evmMessage.contract,
      evmMessage.value,
      code ?: Bytes.EMPTY,
      evmMessage.inputData,
      evmMessage.gas,
      evmMessage.kind,
      depth = evmMessage.depth,
      hostContext = this
    )
    return result
  }

  override suspend fun create(evmMessage: EVMMessage, code: Bytes): EVMResult {
    logger.trace("Entering create ${evmMessage.kind}")

    val result = ethereumVirtualMachine.executeInternal(
      evmMessage.origin,
      evmMessage.sender,
      evmMessage.destination,
      evmMessage.contract,
      evmMessage.value,
      code,
      evmMessage.inputData,
      evmMessage.gas,
      evmMessage.kind,
      depth = evmMessage.depth,
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
      sender,
      currentCoinbase,
      currentNumber,
      currentTimestamp,
      Bytes.ofUnsignedLong(currentGasLimit),
      currentDifficulty.toBytes(),
      UInt256.ONE.toBytes()
    )
  }

  override fun getBlockHash(number: UInt256): Bytes32 {
    logger.trace("Entering getBlockHash")
    val listOfCandidates = blockchainRepository.findBlockByHashOrNumber(number)
    return listOfCandidates.firstOrNull() ?: Bytes32.ZERO
  }

  override fun emitLog(address: Address, data: Bytes, topics: List<Bytes32>) {
    logger.trace("Entering emitLog")
    val log = Log(Address.fromBytes(Bytes.wrap(address)), data, topics)
    logs.add(log)
  }

  override fun warmUpAccount(address: Address): Boolean =
    !ethereumVirtualMachine.precompiles.contains(address) && warmedUpStorage.add(address)

  override fun warmUpStorage(address: Address, key: UInt256): Boolean {
    logger.trace("entering warmUpStorage $address $key")
    if (ethereumVirtualMachine.precompiles.contains(address)) {
      return false
    }
    return warmedUpStorage.add(Bytes.concatenate(address, Bytes.fromHexString("0x0f"), key))
  }

  override fun getGasPrice() = gasPrice

  override fun getGasLimit() = currentGasLimit

  override fun getBlockNumber() = currentNumber

  override fun getBlockHash() = getBlockHash(currentNumber)

  override fun getCoinbase() = currentCoinbase

  override fun timestamp() = currentTimestamp

  override fun getDifficulty() = currentDifficulty

  override fun getChaindId(): UInt256 = chainId

  override fun addRefund(address: Address, refund: Wei) {
    refunds[address] = (refunds[address] ?: BigInteger.ZERO).add(refund.toBigInteger())
  }

  override fun addRefund(address: Address, refund: Long) {
    refunds[address] = (refunds[address] ?: BigInteger.ZERO).add(BigInteger.valueOf(refund))
  }
}
