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

import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.LogsBloomFilter
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.eth.precompiles.PrecompileContract
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.TransientStateRepository
import org.apache.tuweni.evm.EthereumVirtualMachine
import org.apache.tuweni.evm.HardFork
import org.apache.tuweni.evm.impl.EvmVmImpl
import org.apache.tuweni.evm.impl.StepListener
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.trie.MerklePatriciaTrie
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.slf4j.LoggerFactory

/**
 * A block processor executing blocks, executing in sequence transactions
 * and committing data to a blockchain repository.
 *
 * @param chainId the chain identifier
 */
class BlockProcessor(val chainId: UInt256) {

  companion object {
    val logger = LoggerFactory.getLogger(BlockProcessor::class.java)
  }

  /**
   * Executes a state transition.
   *
   * @param parentBlock the parent block header
   * @param coinbase the coinbase of the block
   * @param gasLimit the gas limit of the block
   * @param gasUsed the gas used already
   * @param transactions the list of transactions to execute
   * @param repository the blockchain repository to execute against
   * @param stepListener an optional listener that can follow the steps of the execution
   * @param precompiles the map of precompiles
   * @param hardFork the current hard fork
   */
  suspend fun execute(
    parentBlock: BlockHeader,
    coinbase: Address,
    gasLimit: Gas,
    gasUsed: Gas,
    timestamp: UInt256,
    transactions: List<Transaction>,
    repository: BlockchainRepository,
    precompiles: Map<Address, PrecompileContract>,
    hardFork: HardFork,
    stepListener: StepListener? = null
  ): BlockProcessorResult {
    val stateChanges = TransientStateRepository(repository)
    val vm = EthereumVirtualMachine(stateChanges, repository, precompiles, { EvmVmImpl.create(stepListener) })
    vm.start()

    val bloomFilter = LogsBloomFilter()

    val transactionsTrie = MerklePatriciaTrie.storingBytes()
    val receiptsTrie = MerklePatriciaTrie.storingBytes()
    val allReceipts = mutableListOf<TransactionReceipt>()

    var counter = 0L
    var allGasUsed = Gas.ZERO
    var success = true
    for (tx in transactions) {
      val txProcessor = TransactionProcessor(vm, hardFork, repository, stateChanges)
      val indexKey = RLP.encodeValue(UInt256.valueOf(counter).trimLeadingZeros())
      transactionsTrie.put(indexKey, tx.toBytes())
      val txResult = txProcessor.execute(tx, timestamp, chainId, parentBlock, gasLimit, gasUsed, allGasUsed, coinbase, bloomFilter)
      if (txResult.success) {
        val receipt = txResult.receipt!!
        receiptsTrie.put(indexKey, receipt.toBytes())
        allReceipts.add(receipt)
        allGasUsed = allGasUsed.add(txResult.gasUsed)
      } else {
        success = false
      }
      counter++
    }

    val block = ProtoBlock(
      SealableHeader(
        parentBlock.hash,
        Hash.fromBytes(stateChanges.stateRootHash()),
        Hash.fromBytes(transactionsTrie.rootHash()),
        Hash.fromBytes(receiptsTrie.rootHash()),
        bloomFilter.toBytes(),
        parentBlock.number.add(1),
        parentBlock.gasLimit,
        allGasUsed
      ),
      ProtoBlockBody(transactions),
      allReceipts,
      stateChanges
    )
    return BlockProcessorResult(block, success)
  }
}

data class BlockProcessorResult(val block: ProtoBlock, val success: Boolean)
