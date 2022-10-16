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
package org.apache.tuweni.blockprocessor

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.AccountState
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.LogsBloomFilter
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.TransientStateRepository
import org.apache.tuweni.evm.CallKind
import org.apache.tuweni.evm.EVMExecutionStatusCode
import org.apache.tuweni.evm.EthereumVirtualMachine
import org.apache.tuweni.evm.HardFork
import org.apache.tuweni.trie.MerkleTrie
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei

data class TransactionProcessorResult(val receipt: TransactionReceipt? = null, val success: Boolean = false, val gasUsed: Gas = Gas.ZERO)

class TransactionProcessor(val vm: EthereumVirtualMachine, val hardFork: HardFork, val repository: BlockchainRepository, val stateChanges: TransientStateRepository) {

  fun calculateTransactionCost(payload: Bytes, hardFork: HardFork): Gas {
    var zeros = 0
    val zeroByte = 0.toByte()
    for (i in 0 until payload.size()) {
      if (payload.get(i) == zeroByte) {
        ++zeros
      }
    }
    val nonZeros = payload.size() - zeros

    return if (hardFork.number < HardFork.ISTANBUL.number) {
      Gas.valueOf(21000L)
        .add(Gas.valueOf(4L * zeros))
        .add(Gas.valueOf(68L * nonZeros))
    } else {
      Gas.valueOf(21000L)
        .add(Gas.valueOf(4L * zeros))
        .add(Gas.valueOf(16L * nonZeros))
    } // TODO add create cost.
  }

  suspend fun execute(
    tx: Transaction,
    timestamp: UInt256,
    chainId: UInt256,
    parentBlock: BlockHeader,
    gasLimit: Gas,
    gasUsed: Gas,
    allGasUsed: Gas,
    coinbase: Address,
    blockBloomFilter: LogsBloomFilter
  ): TransactionProcessorResult {
    val sender = tx.sender
    if (sender === null) {
      return TransactionProcessorResult()
    }
    val senderAccount = repository.getAccount(sender)
    if (senderAccount === null || !Hash.hash(Bytes.EMPTY).equals(senderAccount.codeHash)) {
      return TransactionProcessorResult()
    }

    if (tx.getGasLimit() > gasLimit.subtract(gasUsed).subtract(allGasUsed)) {
      return TransactionProcessorResult()
    }

    val code: Bytes
    val to: Address
    val inputData: Bytes
    if (null == tx.to) {
      val contractAddress = Address.fromTransaction(tx)
      to = contractAddress
      code = tx.payload
      inputData = Bytes.EMPTY
      val state = AccountState(
        UInt256.ONE,
        Wei.valueOf(0),
        Hash.fromBytes(MerkleTrie.EMPTY_TRIE_ROOT_HASH),
        Hash.hash(tx.payload)
      )
      stateChanges.storeAccount(contractAddress, state)
      stateChanges.storeCode(tx.payload)
    } else {
      code = stateChanges.getAccountCode(tx.to!!) ?: Bytes.EMPTY
      to = tx.to!!
      inputData = tx.payload
    }
    val gasCost = calculateTransactionCost(tx.payload, hardFork)
    val result = vm.execute(
      tx.sender!!,
      to,
      tx.value,
      code,
      inputData,
      tx.gasLimit.subtract(gasCost),
      tx.gasPrice,
      coinbase,
      parentBlock.number.add(1),
      timestamp,
      tx.gasLimit.toLong(),
      parentBlock.difficulty,
      chainId,
      CallKind.CALL,
      hardFork
    )
    result.state.gasManager.add(gasCost)
    if (result.statusCode != EVMExecutionStatusCode.SUCCESS) {
      BlockProcessor.logger.info("EVM execution failed with status ${result.statusCode}")
      return TransactionProcessorResult()
    }

    val pay = result.state.gasManager.gasCost.priceFor(tx.gasPrice)
    if (pay < tx.gasPrice) { // indicates an overflow.
      return TransactionProcessorResult()
    }
    val senderAccountAfter = repository.getAccount(sender)!!
    val newSenderAccountState = AccountState(
      senderAccountAfter.nonce.add(1),
      senderAccountAfter.balance.subtract(pay),
      senderAccountAfter.storageRoot,
      senderAccountAfter.codeHash
    )
    stateChanges.storeAccount(sender, newSenderAccountState)

    val coinbaseAccountState = stateChanges.getAccount(coinbase) ?: stateChanges.newAccountState()
    val newCoinbaseAccountState = AccountState(
      coinbaseAccountState.nonce,
      coinbaseAccountState.balance.add(pay),
      coinbaseAccountState.storageRoot,
      coinbaseAccountState.codeHash
    )
    stateChanges.storeAccount(coinbase, newCoinbaseAccountState)

    for (accountToDestroy in result.changes.accountsToDestroy()) {
      stateChanges.destroyAccount(accountToDestroy)
    }

    val txLogsBloomFilter = LogsBloomFilter()
    for (log in result.changes.getLogs()) {
      blockBloomFilter.insertLog(log)
      txLogsBloomFilter.insertLog(log)
    }
    val receipt = TransactionReceipt(
      1,
      result.state.gasManager.gasCost.toLong(),
      txLogsBloomFilter,
      result.changes.getLogs()
    )
    return TransactionProcessorResult(receipt = receipt, success = true, gasUsed = gasUsed)
  }
}
