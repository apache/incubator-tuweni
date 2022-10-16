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

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.junit.jupiter.api.Test

data class StorageEntry @JsonCreator constructor(
  @JsonProperty("key") val key: Bytes,
  @JsonProperty("value") val value: Bytes
)

data class AccountInfo @JsonCreator constructor(
  @JsonProperty("address") val address: Address,
  @JsonProperty("balance") val balance: Wei,
  @JsonProperty("code") val code: Bytes,
  @JsonProperty("nonce") val nonce: UInt256,
  @JsonProperty("storage") val storage: List<StorageEntry>
)

data class Before @JsonCreator constructor(
  @JsonProperty("accounts") val accounts: List<AccountInfo>,
  @JsonProperty("stack") val stack: List<Bytes>,
  @JsonProperty("memory") val memory: List<Bytes32>
)

data class After @JsonCreator constructor(
  @JsonProperty("accounts") val accounts: List<AccountInfo>,
  @JsonProperty("stack") val stack: List<Bytes>,
  @JsonProperty("memory") val memory: List<Bytes32>,
  @JsonProperty("logs") val logs: List<Log>
)

data class Operation @JsonCreator constructor(
  @JsonProperty("name") val name: String,
  @JsonProperty("opcode") val opcode: Bytes
)

data class Log @JsonCreator constructor(
  @JsonProperty("logger") val logger: Address,
  @JsonProperty("data") val data: Bytes,
  @JsonProperty("topics") val topics: List<Bytes32>
)

/**
 * Model representing a test of an EVM opcode.
 *
 *
 * This model can be serialized into a YAML document, to be consumed by implementers.
 */
data class OpcodeTestModel @JsonCreator constructor(
  @JsonProperty("name") val name: String,
  @JsonProperty("hardFork") val hardFork: String,
  @JsonProperty("after") val after: After,
  @JsonProperty("before") val before: Before,
  @JsonProperty("inputData") val inputData: Bytes,
  @JsonProperty("gasPrice") val gasPrice: Wei,
  @JsonProperty("gasAvailable") val gas: Gas,
  @JsonProperty("haltReason") val exceptionalHaltReason: String,
  @JsonProperty("gasLimit") val gasLimit: Long,
  @JsonProperty("number") val number: Long,
  @JsonProperty("timestamp") val timestamp: Long,
  @JsonProperty("baseFee") val baseFee: Long,
  @JsonProperty("difficultyBytes") val difficultyBytes: Bytes,
  @JsonProperty("mixHashOrPrevRandao") val mixHashOrPrevRandao: Bytes,
  @JsonProperty("chainId") val chainId: UInt256,
  @JsonProperty("index") val index: Int,
  @JsonProperty("sender") val sender: Address,
  @JsonProperty("receiver") val receiver: Address,
  @JsonProperty("coinbase") val coinbase: Address,
  @JsonProperty("code") val code: Bytes,
  @JsonProperty("value") val value: UInt256,
  @JsonProperty("gasUsed") val gasUsed: Gas,
  @JsonProperty("allGasUsed") val allGasUsed: Gas,
  @JsonProperty("refunds") val refunds: Map<Address, Wei>
)

class OpcodeTestModelTest {

  @Test
  fun testReadFromFile() {
    val mapper = ObjectMapper(YAMLFactory())
    mapper.registerModule(EthJsonModule())
    val modelBytes =
      OpcodeTestModelTest::class.java.getResourceAsStream("/certification/frontier/PUSH10-4.yaml").readAllBytes()
    mapper.readValue(modelBytes, OpcodeTestModel::class.java)
  }
}
