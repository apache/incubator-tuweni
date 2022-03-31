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
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.precompiles.Registry
import org.apache.tuweni.eth.repository.BlockchainIndex
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.evm.impl.EvmVmImpl
import org.apache.tuweni.evm.impl.StepListener
import org.apache.tuweni.genesis.Genesis
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.LuceneIndexWriter
import org.apache.tuweni.junit.LuceneIndexWriterExtension
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.charset.StandardCharsets

@ExtendWith(LuceneIndexWriterExtension::class, BouncyCastleExtension::class)
class EthereumVirtualMachineTest {

  @Test
  fun testVersion(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    val repository = BlockchainRepository.init(
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      BlockchainIndex(writer),
      Genesis.dev()
    )
    val vm = EthereumVirtualMachine(repository, Registry.istanbul, EvmVmImpl::create)
    vm.start()
    assertEquals("0.0.1", vm.version())
    vm.stop()
  }

  @Test
  fun testExecuteCall(@LuceneIndexWriter writer: IndexWriter) {
    val result = runCode(writer, Bytes.fromHexString("0x30600052596000f3"))
    assertEquals(EVMExecutionStatusCode.SUCCESS, result.statusCode)
    assertEquals(Gas.valueOf(199984), result.state.gasManager.gasLeft())
  }

  @Test
  fun testExecuteCounter(@LuceneIndexWriter writer: IndexWriter) {
    val result = runCode(writer, Bytes.fromHexString("0x600160005401600055"))
    assertEquals(EVMExecutionStatusCode.SUCCESS, result.statusCode)
    assertEquals(Gas.valueOf(179488), result.state.gasManager.gasLeft())
  }

  @Test
  fun testExecuteReturnBlockNumber(@LuceneIndexWriter writer: IndexWriter) {
    val result = runCode(writer, Bytes.fromHexString("0x43600052596000f3"))
    assertEquals(EVMExecutionStatusCode.SUCCESS, result.statusCode)
    assertEquals(Gas.valueOf(199984), result.state.gasManager.gasLeft())
  }

  @Test
  fun testExecuteSaveReturnBlockNumber(@LuceneIndexWriter writer: IndexWriter) {
    val result = runCode(writer, Bytes.fromHexString("0x4360005543600052596000f3"))
    assertEquals(EVMExecutionStatusCode.SUCCESS, result.statusCode)
    assertEquals(Gas.valueOf(197779), result.state.gasManager.gasLeft())
  }

  @Disabled
  @Test
  @Throws(Exception::class)
  fun testGetCapabilities(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    val repository = BlockchainRepository(
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      BlockchainIndex(writer)
    )
    val vm = EthereumVirtualMachine(repository, Registry.istanbul, EvmVmImpl::create)
    vm.start()
    assertTrue(vm.capabilities() > 0)
    vm.stop()
  }

  @Disabled
  @Test
  @Throws(Exception::class)
  fun testSetOption(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    val repository = BlockchainRepository(
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      BlockchainIndex(writer)
    )
    val vm = EthereumVirtualMachine(repository, Registry.istanbul, EvmVmImpl::create, mapOf(Pair("verbose", "1")))
    vm.start()
    vm.stop()
  }

  @Test
  private fun testCreate(writer: IndexWriter) = runBlocking {
    val repository = BlockchainRepository(
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      BlockchainIndex(writer)
    )

    val vm = EthereumVirtualMachine(repository, Registry.istanbul, EvmVmImpl::create)
    vm.start()
    try {
      val sender = Address.fromHexString("0x3339626637316465316237643762653362353100")
      val destination = Address.fromBytes(Bytes.fromHexString("3533636637373230346545656639353265323500"))
      val value = Bytes.fromHexString("0x3100")
      val inputData = Bytes.wrap("hello w\u0000".toByteArray(StandardCharsets.UTF_8))
      val gas = Gas.valueOf(200000)
      val result = runBlocking {
        vm.execute(
          sender, destination, value, Bytes.fromHexString("0x00"), inputData, gas,
          Wei.valueOf(0),
          Address.fromBytes(Bytes.random(20)),
          0,
          0,
          2,
          UInt256.valueOf(1),
          UInt256.valueOf(1),
          CallKind.CREATE
        )
      }
      assertEquals(EVMExecutionStatusCode.SUCCESS, result.statusCode)
      assertEquals(20000, result.state.gasManager.gasLeft())
    } finally {
      vm.stop()
    }
  }

  private fun runCode(writer: IndexWriter, code: Bytes, vmFn: () -> EvmVm = { EvmVmImpl.create() }): EVMResult =
    runBlocking {
      val repository = BlockchainRepository.init(
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        BlockchainIndex(writer),
        Genesis.dev()
      )

      val vm = EthereumVirtualMachine(repository, Registry.istanbul, vmFn)
      vm.start()
      try {
        val sender = Address.fromHexString("0x3339626637316465316237643762653362353100")
        val destination = Address.fromBytes(Bytes.fromHexString("3533636637373230346545656639353265323500"))
        val value = Bytes.fromHexString("0x3100")
        val inputData = Bytes.wrap("hello w\u0000".toByteArray(StandardCharsets.UTF_8))
        val gas = Gas.valueOf(200000)
        vm.execute(
          sender, destination, value, code, inputData, gas,
          Wei.valueOf(0),
          Address.fromBytes(Bytes.random(20)),
          0,
          0,
          2,
          UInt256.valueOf(1),
          UInt256.valueOf(1)
        )
      } finally {
        vm.stop()
      }
    }

  @Test
  fun snapshotExecution(@LuceneIndexWriter writer: IndexWriter) {
    val listener = object : StepListener {
      override fun handleStep(executionPath: List<Byte>, state: EVMState): Boolean {
        if (executionPath.size > 3) {
          return true
        }
        return false
      }
    }
    val result = runCode(writer, Bytes.fromHexString("0x30600052596000f3"), { EvmVmImpl.create(listener) })
    assertEquals(EVMExecutionStatusCode.HALTED, result.statusCode)
    assertEquals(Gas.valueOf(199987), result.state.gasManager.gasLeft())
  }

  @Test
  fun snapshotExecutionTooFar(@LuceneIndexWriter writer: IndexWriter) {
    val listener = object : StepListener {
      override fun handleStep(executionPath: List<Byte>, state: EVMState): Boolean {
        if (executionPath.size > 255) {
          return true
        }
        return false
      }
    }
    val result = runCode(writer, Bytes.fromHexString("0x30600052596000f3"), { EvmVmImpl.create(listener) })
    assertEquals(EVMExecutionStatusCode.SUCCESS, result.statusCode)
    assertEquals(Gas.valueOf(199984), result.state.gasManager.gasLeft())
  }

  @Test
  fun testDump(@LuceneIndexWriter writer: IndexWriter) {
    val listener = object : StepListener {
      override fun handleStep(executionPath: List<Byte>, state: EVMState): Boolean {
        if (executionPath.size > 3) {
          return true
        }
        return false
      }
    }
    val result = runCode(writer, Bytes.fromHexString("0x30600052596000f3"), { EvmVmImpl.create(listener) })
    assertEquals(EVMExecutionStatusCode.HALTED, result.statusCode)
    assertEquals(
      Bytes.fromHexString("0xf86983676173880000000000030d40866d656d6f7279a0000000000000000000000000353363663737323034654565663935326532350085737461636ba00000000000000000000000000000000000000000000000000000000000000020866f757470757480846c6f6773"),
      result.state.toBytes()
    )
  }
}
