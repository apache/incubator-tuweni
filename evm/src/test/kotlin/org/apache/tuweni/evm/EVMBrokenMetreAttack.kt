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

import kotlinx.coroutines.runBlocking
import org.apache.lucene.index.IndexWriter
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.AccountState
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.precompiles.Registry
import org.apache.tuweni.eth.repository.BlockchainIndex
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.evm.impl.EvmVmImpl
import org.apache.tuweni.genesis.Genesis
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.LuceneIndexWriter
import org.apache.tuweni.junit.LuceneIndexWriterExtension
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.trie.MerklePatriciaTrie
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(LuceneIndexWriterExtension::class, BouncyCastleExtension::class)
class EVMBrokenMetreAttack {

  @Disabled("Expensive")
  @Test
  fun testBrokenMetreAttackBalance(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    runAttack("31", writer, EVMExecutionStatusCode.OUT_OF_GAS)
  }

  @Disabled("Expensive")
  @Test
  fun testBrokenMetreAttackExtcodeSize(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    runAttack("3B", writer, EVMExecutionStatusCode.STACK_OVERFLOW)
  }

  @Disabled("Expensive")
  @Test
  fun testBrokenMetreAttackExtcodeHash(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    runAttack("3F", writer, EVMExecutionStatusCode.STACK_OVERFLOW)
  }

  @Disabled("Not implemented yet")
  @Test
  fun testBrokenMetreAttackStaticCall(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    runAttack("FA", writer, EVMExecutionStatusCode.STACK_OVERFLOW)
  }

  private suspend fun runAttack(opcode: String, writer: IndexWriter, expectedStatusCode: EVMExecutionStatusCode) {
    val address = Address.fromHexString("0x5a31505a31505a31505a31505a31505a31505a31")
    val code = createAttack(opcode)
    val stateStore = MapKeyValueStore<Bytes, Bytes>()
    val repository = BlockchainRepository.init(
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      stateStore,
      BlockchainIndex(writer),
      Genesis.dev()
    )
    val tree = MerklePatriciaTrie.storingBytes()
    val accountState =
      AccountState(UInt256.valueOf(223), Wei.valueOf(200000), Hash.fromBytes(tree.rootHash()), Hash.hash(code))
    repository.storeAccount(address, accountState)
    repository.storeCode(code)

    val vm = EthereumVirtualMachine(repository, repository, Registry.istanbul, EvmVmImpl::create)
    vm.start()
    val result = vm.execute(
      address,
      address,
      Bytes.fromHexString("0x"),
      code,
      Bytes.fromHexString("0x"),
      Gas.valueOf(10_000_000),
      Wei.valueOf(1),
      address,
      UInt256.valueOf(123L),
      UInt256.valueOf(123L),
      10_000_000,
      UInt256.valueOf(1234),
      UInt256.valueOf(1),
      CallKind.CALL,
      HardFork.ISTANBUL
    )
    assertEquals(expectedStatusCode, result.statusCode)
  }

  private fun createAttack(opcode: String): Bytes {
    val code_repetitions = 8000
    val start = Bytes.of(0x5b.toByte()) // JUMPDEST
    val end = Bytes.fromHexString("600056") // PUSH1 0x0 JUMP

    return if (opcode.equals("fa", ignoreCase = true)) {
      Bytes.concatenate(
        start,
        Bytes.fromHexString("60008080805a5a${opcode}50".repeat(code_repetitions)),
        end
      ) //  (PUSH 0 DUP1 DUP1 DUP1 GAS GAS STATICCALL POP) * 8'000 PUSH1 0x0 JUMP
    } else {
      Bytes.concatenate(
        start,
        Bytes.fromHexString("5a${opcode}50".repeat(code_repetitions)),
        end
      ) // JUMPDEST (GAS BALANCE/EXTCODESIZE/EXTCODEHASH POP) * 8'000 PUSH1 0x0 JUMP
    }
  }
}
