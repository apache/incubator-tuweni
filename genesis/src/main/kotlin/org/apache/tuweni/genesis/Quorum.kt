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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.Address
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.units.bigints.UInt256

class QuorumConfig(val genesis: Genesis, val validators: List<SECP256K1.KeyPair>, val allocations: List<Allocation>) {

  companion object {
    val validatorHeader = "Address,Public key,Secret key\n"
    val header = "User,Public key,Address,Secret key\n"

    fun generate(
      nonce: Bytes = Bytes.ofUnsignedLong(0),
      difficulty: UInt256 = UInt256.ONE.shiftLeft(252),
      mixHash: Bytes32,
      coinbase: Address = Address.ZERO,
      timestamp: Long = 0,
      gasLimit: Long = 0,
      parentHash: Bytes32 = Bytes32.ZERO,
      vanity: Bytes32 = Bytes32.ZERO,
      config: QuorumGenesisConfig,
      numberValidators: Int,
      numberAllocations: Int,
      amount: UInt256
    ): QuorumConfig {
      val allocations = AllocationGenerator().createAllocations(numberAllocations, amount)

      val validators = (0 until numberValidators).map {
        SECP256K1.KeyPair.random()
      }

      return generate(
        nonce = nonce,
        difficulty = difficulty,
        mixHash = mixHash,
        coinbase = coinbase,
        timestamp = timestamp,
        gasLimit = gasLimit,
        parentHash = parentHash,
        config = config,
        vanity = vanity,
        allocations = allocations,
        validators = validators
      )
    }

    fun generate(
      nonce: Bytes = Bytes.ofUnsignedLong(0),
      difficulty: UInt256 = UInt256.ONE.shiftLeft(252),
      coinbase: Address = Address.ZERO,
      timestamp: Long = 0,
      gasLimit: Long = 0,
      parentHash: Bytes32 = Bytes32.ZERO,
      config: QuorumGenesisConfig,
      vanity: Bytes32 = Bytes32.ZERO,
      mixHash: Bytes32,
      allocations: List<Allocation>,
      validators: List<SECP256K1.KeyPair>
    ): QuorumConfig {
      val allocs = mutableMapOf<Address, Map<String, UInt256>>()
      for (alloc in allocations) {
        allocs[alloc.address] = mapOf(Pair("balance", alloc.amount))
      }
      val genesis = Genesis(
        nonce = nonce,
        difficulty = difficulty,
        mixHash = mixHash,
        coinbase = coinbase,
        timestamp = timestamp,
        gasLimit = gasLimit,
        parentHash = parentHash,
        alloc = allocs,
        extraData = QBFTGenesisExtraData(vanity, validators).toBytes(),
        config = config
      )

      return QuorumConfig(genesis, validators, allocations)
    }
  }

  fun validatorsToCsv(): String {
    return validatorHeader + validators.map {
      Address.fromPublicKey(it.publicKey()).toHexString() + "," + it.publicKey().toHexString() + "," + it.secretKey().bytes().toHexString()
    }.joinToString("\n")
  }

  fun allocsToCsv(): String {
    val lines = allocations.map {
      "Unclaimed,${
      it.keyPair.publicKey().toHexString()
      },${it.address.toHexString()},${it.keyPair.secretKey().bytes()}"
    }

    return header + lines.joinToString("\n")
  }
}

data class QBFTGenesisExtraData(val vanity: Bytes32, val validators: List<SECP256K1.KeyPair>) {

  /**
   * A RLP serialization of the extradata for QBFT:
   * RLP([32 bytes Vanity, List<Validators>, No Vote, Round=Int(0), 0 Seals]).
   */
  fun toBytes(): Bytes = RLP.encodeList {
    it.writeValue(vanity)
    it.writeList { valWriter ->
      for (validator in validators) {
        valWriter.writeValue(Address.fromPublicKey(validator.publicKey()))
      }
    }
    it.writeList {}
    it.writeString("")
    it.writeList {}
  }
}

class IstanbulConfigOptions(
  val epoch: Int = 3000,
  val policy: Int = 0,
  val testQBFTBlock: Int = 0,
  val ceil2Nby3Block: Int = 0
)

@JsonPropertyOrder(alphabetic = true)
class QuorumGenesisConfig(
  chainId: Int,
  homesteadBlock: Int = 0,
  eip150Block: Int = 0,
  eip155Block: Int = 0,
  eip158Block: Int = 0,
  byzantiumBlock: Int = 0,
  constantinopleBlock: Int = 0,
  val istanbul: IstanbulConfigOptions = IstanbulConfigOptions(),
  val txnSizeLimit: Int = 64,
  val maxCodeSize: Int = 0
) : GenesisConfig(chainId, homesteadBlock, eip150Block, eip155Block, eip158Block, byzantiumBlock, constantinopleBlock) {

  @JsonProperty("isQuorum")
  fun isQuorum(): Boolean = true
}
