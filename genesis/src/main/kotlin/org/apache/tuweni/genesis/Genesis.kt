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
package org.apache.tuweni.genesis

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.Address
import org.apache.tuweni.units.bigints.UInt256

@JsonPropertyOrder(alphabetic = true)
open class GenesisConfig(
  val chainId: Int,
  val homesteadBlock: Int = 0,
  val eip150Block: Int = 0,
  val eip155Block: Int = 0,
  val eip158Block: Int = 0,
  val byzantiumBlock: Int = 0,
  val constantinopleBlock: Int = 0,
)

/**
 * Genesis block representation
 *
 * The block contains the information about the chain, and the initial state of the chain, such as account balances.
 */
@JsonPropertyOrder("config", "nonce", "timestamp", "extraData", "gasLimit", "difficulty", "number", "gasUsed", "parentHash", "mixHash", "coinbase", "alloc")
class Genesis(
  val nonce: Bytes,
  val difficulty: UInt256,
  val mixHash: Bytes32,
  val coinbase: Address,
  private val timestamp: Long,
  val extraData: Bytes,
  private val gasLimit: Long,
  val parentHash: Bytes32,
  val alloc: Map<Address, UInt256>,
  val config: GenesisConfig,
) {

  fun getTimestamp(): String {
    if (timestamp == 0L) {
      return "0x0"
    }
    return Bytes.ofUnsignedLong(timestamp).toHexString()
  }

  fun getGasLimit(): String {
    return Bytes.ofUnsignedLong(gasLimit).toHexString()
  }

  fun getNumber(): String = "0x0"
}
