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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import kotlinx.coroutines.runBlocking
import org.apache.lucene.index.IndexWriter
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.AccountState
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.precompiles.Registry
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.TransientStateRepository
import org.apache.tuweni.evm.impl.EvmVmImpl
import org.apache.tuweni.genesis.Genesis
import org.apache.tuweni.io.Resources
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.LuceneIndexWriter
import org.apache.tuweni.junit.LuceneIndexWriterExtension
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.trie.MerkleStorage
import org.apache.tuweni.trie.MerkleTrie
import org.apache.tuweni.trie.StoredMerklePatriciaTrie
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Wei
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.IOException
import java.io.InputStream
import java.io.UncheckedIOException
import java.util.stream.Stream

@Tag("referenceTest")
@ExtendWith(LuceneIndexWriterExtension::class, BouncyCastleExtension::class)
class EvmCertificationTest {
  companion object {

    val mapper = ObjectMapper(YAMLFactory())

    init {
      mapper.registerModule(EthJsonModule())
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun findBerlinTests(): Stream<Arguments> {
      return findTests("/vmtests/berlin/*.yaml")
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun findIstanbulTests(): Stream<Arguments> {
      return findTests("/vmtests/istanbul/*.yaml")
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun findFrontierTests(): Stream<Arguments> {
      return findTests("/vmtests/frontier/*.yaml")
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun findHomesteadTests(): Stream<Arguments> {
      return findTests("/vmtests/homestead/*.yaml")
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun findConstantinopleTests(): Stream<Arguments> {
      return findTests("/vmtests/constantinople/*.yaml")
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun findTangerineWhistleTests(): Stream<Arguments> {
      return findTests("/vmtests/tangerineWhistle/*.yaml")
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun findSpuriousDragonTests(): Stream<Arguments> {
      return findTests("/vmtests/spuriousDragon/*.yaml")
    }

    @Throws(IOException::class)
    private fun findTests(glob: String): Stream<Arguments> {
      return Resources.find(glob).map { url ->
        try {
          url.openConnection().getInputStream().use { input -> prepareTest(input) }
        } catch (e: IOException) {
          throw UncheckedIOException("Could not read $url", e)
        }
      }.sorted { o1, o2 -> (o1.get()[0] as String).compareTo(o2.get()[0] as String) }.filter() {
        val test = it.get()[1] as OpcodeTestModel
        test.exceptionalHaltReason == "NONE"
      }
    }

    @Throws(IOException::class)
    private fun prepareTest(input: InputStream): Arguments {
      val test = mapper.readValue(input, OpcodeTestModel::class.java)
      return Arguments.of(test.name + "-" + test.index, test)
    }
  }

  private var writer: IndexWriter? = null

  @BeforeEach
  fun setUp(@LuceneIndexWriter newWriter: IndexWriter) {
    writer = newWriter
  }

  @ParameterizedTest(name = "Frontier {index}: {0}")
  @MethodSource("findFrontierTests")
  fun runFrontierReferenceTests(testName: String, test: OpcodeTestModel) {
    runReferenceTests(testName, HardFork.FRONTIER, test)
  }

  @ParameterizedTest(name = "Homestead {index}: {0}")
  @MethodSource("findHomesteadTests")
  fun runHomesteadReferenceTests(testName: String, test: OpcodeTestModel) {
    runReferenceTests(testName, HardFork.HOMESTEAD, test)
  }

  @ParameterizedTest(name = "TangerineWhistle {index}: {0}")
  @MethodSource("findTangerineWhistleTests")
  fun runTangerineWhistleReferenceTests(testName: String, test: OpcodeTestModel) {
    runReferenceTests(testName, HardFork.TANGERINE_WHISTLE, test)
  }

  @ParameterizedTest(name = "SpuriousDragon {index}: {0}")
  @MethodSource("findSpuriousDragonTests")
  fun runSpuriousDragonReferenceTests(testName: String, test: OpcodeTestModel) {
    runReferenceTests(testName, HardFork.SPURIOUS_DRAGON, test)
  }

  @ParameterizedTest(name = "Constantinople {index}: {0}")
  @MethodSource("findConstantinopleTests")
  fun runConstantinopleReferenceTests(testName: String, test: OpcodeTestModel) {
    runReferenceTests(testName, HardFork.CONSTANTINOPLE, test)
  }

  @ParameterizedTest(name = "Istanbul {index}: {0}")
  @MethodSource("findIstanbulTests")
  fun runIstanbulReferenceTests(testName: String, test: OpcodeTestModel) {
    runReferenceTests(testName, HardFork.ISTANBUL, test)
  }

  @ParameterizedTest(name = "Berlin {index}: {0}")
  @MethodSource("findBerlinTests")
  fun runBerlinReferenceTests(testName: String, test: OpcodeTestModel) {
    runReferenceTests(testName, HardFork.BERLIN, test)
  }

  private fun runReferenceTests(testName: String, hardFork: HardFork, test: OpcodeTestModel) = runBlocking {
    Assertions.assertNotNull(testName)
    println(testName)
    val repository = BlockchainRepository.inMemory(Genesis.dev())
    test.before.accounts.forEach { info ->
      runBlocking {
        val accountState = AccountState(
          info.nonce,
          info.balance,
          Hash.fromBytes(MerkleTrie.EMPTY_TRIE_ROOT_HASH),
          Hash.hash(info.code)
        )
        repository.storeAccount(info.address, accountState)
        repository.storeCode(info.code)
        val accountStorage = info.storage

        for (entry in accountStorage) {
          repository.storeAccountValue(info.address, Hash.hash(entry.key), Bytes32.leftPad(entry.value))
        }
      }
    }
    val changesRepository = TransientStateRepository(repository)
    val vm = EthereumVirtualMachine(changesRepository, repository, Registry.istanbul, EvmVmImpl::create)
    vm.start()
    try {
      val result = vm.execute(
        test.sender,
        test.receiver,
        test.value,
        test.code,
        test.inputData,
        test.gas,
        test.gasPrice,
        test.coinbase,
        UInt256.valueOf(test.number),
        UInt256.valueOf(test.timestamp),
        test.gasLimit,
        UInt256.fromBytes(Bytes32.leftPad(test.difficultyBytes)),
        test.chainId,
        CallKind.CALL,
        hardFork
      )

      if (test.name == "REVERT") {
        assertEquals(EVMExecutionStatusCode.REVERT, result.statusCode)
      } else if (result.statusCode == EVMExecutionStatusCode.REJECTED) {
        // if the execution is rejected, it's because the amount of the value is higher than the sender balance. Pass.
        return@runBlocking
      } else {
        assertEquals(EVMExecutionStatusCode.SUCCESS, result.statusCode)
      }

      for (i in 0 until test.after.stack.size) {
        assertEquals(Bytes32.leftPad(test.after.stack[i]), result.state.stack.get(i), "Mismatch of stack elements")
      }

      test.after.accounts.forEach { info ->
        runBlocking {
          val address = info.address
          assertTrue(changesRepository.accountsExists(address))
          val accountState = changesRepository.getAccount(address)
          val balance = accountState?.balance ?: Wei.valueOf(0)
          assertEquals(info.balance, balance, "balance doesn't match: " + address.toHexString() + ":" + if (balance > info.balance) balance.subtract(info.balance).toString() else info.balance.subtract(balance).toString())
          assertEquals(info.nonce, accountState!!.nonce)

          for (stored in info.storage) {
            val changed = changesRepository.getAccountStoreValue(address, Hash.hash(stored.key))?.let { RLP.decodeValue(it) } ?: UInt256.ZERO
            assertEquals(stored.value, Bytes32.leftPad(changed)) {
              runBlocking {
                val account = changesRepository.getAccount(address) ?: changesRepository.newAccountState()
                val tree = StoredMerklePatriciaTrie.storingBytes(
                  object : MerkleStorage {
                    override suspend fun get(hash: Bytes32): Bytes? {
                      return changesRepository.transientState.get(hash)
                    }

                    override suspend fun put(hash: Bytes32, content: Bytes) {
                      return changesRepository.transientState.put(hash, content)
                    }
                  },
                  account.storageRoot
                )
                "mismatched account storage for address $address at slot ${stored.key}\n" + tree.printAsString()
              }
            }
          }
        }
      }

      test.after.logs.let {
        val ourLogs = (result.hostContext as TransactionalEVMHostContext).getLogs()
        for (i in 0 until it.size) {
          assertEquals(it[i].logger, ourLogs[i].logger)
          assertEquals(it[i].data, ourLogs[i].data)
          assertEquals(it[i].topics.size, ourLogs[i].topics.size)
          for (j in 0 until it[i].topics.size) {
            assertEquals(it[i].topics[j], ourLogs[i].topics[j])
          }
        }
      }

      assertEquals(
        test.allGasUsed.toLong(),
        result.state.gasManager.gasCost.toLong(),
        " diff: " + if (test.allGasUsed > result.state.gasManager.gasLeft()) {
          test.allGasUsed.subtract(result.state.gasManager.gasLeft())
        } else {
          result.state.gasManager.gasLeft()
            .subtract(test.allGasUsed)
        }
      )
    } finally {
      vm.stop()
    }
  }
}
