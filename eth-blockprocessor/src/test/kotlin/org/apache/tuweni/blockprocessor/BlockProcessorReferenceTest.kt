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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.apache.lucene.index.IndexWriter
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.AccountState
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.genesis.Genesis
import org.apache.tuweni.io.Resources
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.LuceneIndexWriter
import org.apache.tuweni.junit.LuceneIndexWriterExtension
import org.apache.tuweni.trie.MerkleTrie
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.IOException
import java.io.InputStream
import java.io.UncheckedIOException
import java.util.stream.Collectors
import java.util.stream.Stream

@ExtendWith(LuceneIndexWriterExtension::class, BouncyCastleExtension::class)
class BlockProcessorReferenceTest {

  companion object {

    val mapper = ObjectMapper()

    init {
      mapper.registerModule(EthJsonModule())
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun findGeneralStateTests(): Stream<Arguments> {
      return findTests("/GeneralStateTests/**/*.json").filter {
        !(it.get()[0] as String).contains("loop")
      }
    }

    @Throws(IOException::class)
    private fun findTests(glob: String): Stream<Arguments> {
      return Resources.find(glob).flatMap { url ->
        try {
          url.openConnection().getInputStream().use { input -> prepareTests(input) }
        } catch (e: IOException) {
          throw UncheckedIOException("Could not read $url", e)
        }
      }
    }

    @Throws(IOException::class)
    private fun prepareTests(input: InputStream): Stream<Arguments> {

      val typeRef = object : TypeReference<HashMap<String, JsonReferenceTest>>() {}
      val allTests: Map<String, JsonReferenceTest> = mapper.readValue(input, typeRef)
      return allTests
        .entries
        .stream()
        .map { entry ->
          val test = entry.value
          var index = 0
          val secretKey = SECP256K1.SecretKey.fromBytes(test.transaction!!.secretKey!!)
          val keyPair = SECP256K1.KeyPair.fromSecretKey(secretKey)
          val berlinTests = test.post?.get("Berlin")?.map { exec ->
            val txFn = {

              Transaction(
                test.transaction!!.nonce!!,
                Wei.valueOf(20),
                test.transaction!!.gasLimit!!.get(exec.indexes!!.gas!!),
                test.transaction!!.to,
                Wei.valueOf(UInt256.fromBytes(test.transaction!!.value!!.get(exec.indexes!!.value!!))),
                test.transaction!!.data!!.get(exec.indexes!!.data!!),
                keyPair
              )
            }
            val arg = Arguments.of(entry.key, "Berlin", index, entry.value, txFn, exec)
            index++
            arg
          }
          berlinTests ?: listOf()
        }.collect(Collectors.toList()).flatten().stream()
    }
  }

  private var writer: IndexWriter? = null

  @BeforeEach
  fun setUp(@LuceneIndexWriter newWriter: IndexWriter) {
    writer = newWriter
  }

  @ParameterizedTest(name = "{index}: {0} {1} {2}")
  @MethodSource("findGeneralStateTests")
  fun runGeneralStateTests(
    testName: String,
    hardFork: String,
    testIndex: Int,
    test: JsonReferenceTest,
    tx: () -> Transaction,
    exec: TransactionExecution,
  ) {
    runReferenceTests(testName, hardFork, testIndex, test, tx, exec)
  }

  private fun runReferenceTests(
    testName: String,
    hardFork: String,
    testIndex: Int,
    test: JsonReferenceTest,
    tx: () -> Transaction,
    exec: TransactionExecution,
  ) = runBlocking {
    assertNotNull(testName)
    assertNotNull(hardFork)
    assertNotNull(testIndex)
    println(testName)
    assertNotNull(test)
    assertNotNull(exec)
    val repository = BlockchainRepository.inMemory(Genesis.dev())
    test.pre!!.forEach { address, state ->
      runBlocking {
        val accountState = AccountState(
          state.nonce!!,
          state.balance!!,
          Hash.fromBytes(MerkleTrie.EMPTY_TRIE_ROOT_HASH),
          Hash.hash(state.code!!)
        )
        repository.storeAccount(address, accountState)
        repository.storeCode(state.code!!)
        val accountStorage = state.storage

        if (accountStorage != null) {
          for (entry in accountStorage) {
            repository.storeAccountValue(address, Bytes32.leftPad(entry.key), Bytes32.leftPad(entry.value))
          }
        }
      }
    }
    val processor = BlockProcessor(UInt256.ONE)

    try {
      val protoBlock =
        processor.execute(Genesis.dev(), test.env!!.currentGasLimit!!, Gas.ZERO, listOf(tx()), repository)
      assertNotNull(protoBlock)
    } catch (e: Exception) {
      if (e.message == "invalid transaction result REVERT") {
        // carry on.
      } else if (testName.startsWith("badOpcodes")) {
        assertEquals("invalid transaction result INVALID_INSTRUCTION", e.message)
      } else if (testName.startsWith("stackOverflow")) {
        assertEquals("invalid transaction result STACK_OVERFLOW", e.message)
      } else if (testName.startsWith("shallowStack")) {
        assertEquals("invalid transaction result STACK_UNDERFLOW", e.message)
      } else if (testName.contains("OOG")) {
        assertEquals("invalid transaction result OUT_OF_GAS", e.message)
      } else {
        throw e
      }
    }

    // assertEquals(exec.logs, protoBlock!!.header.receiptsRoot)
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonAccountState(
  var balance: Wei? = null,
  var code: Bytes? = null,
  var nonce: UInt256? = null,
  var storage: Map<UInt256, UInt256>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransactionExecutionIndex(
  var data: Int? = null,
  var gas: Int? = null,
  var value: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransactionExecution(
  var hash: Hash? = null,
  var indexes: TransactionExecutionIndex? = null,
  var logs: Bytes? = null,
  var txbytes: Bytes? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonReferenceTest(
  var env: Env? = null,
  var post: Map<String, List<TransactionExecution>>? = null,
  var pre: Map<Address, JsonAccountState>? = null,
  var transaction: TransactionStep? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransactionStep(
  var data: List<Bytes>? = null,
  val gasLimit: List<Gas>? = null,
  var nonce: UInt256? = null,
  var secretKey: Bytes32? = null,
  var sender: Address? = null,
  var to: Address? = null,
  val value: List<Bytes>? = null,
) {
  fun combinedData(): Bytes? =
    data?.let {
      Bytes.concatenate(it)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Env(
  var currentBasefee: Address? = null,
  var currentCoinbase: Address? = null,
  var currentDifficulty: UInt256? = null,
  var currentGasLimit: Gas? = null,
  var currentNumber: UInt256? = null,
  var currentTimestamp: UInt256? = null,
  var previousHash: Hash? = null,
)
