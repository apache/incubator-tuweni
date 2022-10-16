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
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlp.RLPWriter
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.bigints.UInt64
import org.apache.tuweni.units.ethereum.Gas
import java.time.Instant

@JsonPropertyOrder(alphabetic = true)
open class GenesisConfig(
  val chainId: Int,
  val homesteadBlock: Int = 0,
  val eip150Block: Int = 0,
  val eip155Block: Int = 0,
  val eip158Block: Int = 0,
  val byzantiumBlock: Int = 0,
  val constantinopleBlock: Int = 0
)

/**
 * Genesis block representation
 *
 * The block contains the information about the chain, and the initial state of the chain, such as account balances.
 */
@JsonPropertyOrder(
  "config",
  "nonce",
  "timestamp",
  "extraData",
  "gasLimit",
  "difficulty",
  "number",
  "gasUsed",
  "mixHash",
  "coinbase",
  "parentHash",
  "alloc"
)
class Genesis(
  val nonce: Bytes,
  val difficulty: UInt256,
  val mixHash: Bytes32,
  val coinbase: Address,
  val parentHash: Bytes32,
  private val timestamp: Long,
  val extraData: Bytes,
  private val gasLimit: Long,
  val alloc: Map<Address, Map<String, UInt256>>,
  val config: GenesisConfig
) {

  companion object {
    /**
     * A hash of a RLP-encoded list, useful to represent blocks with no ommers.
     */
    val emptyListHash = Hash.hash(RLP.encodeList { })

    /**
     * A hash of the RLP encoding of a zero-bytes long bytes array.
     */
    val emptyHash = Hash.hash(
      RLP.encode { writer: RLPWriter ->
        writer.writeValue(Bytes.EMPTY)
      }
    )

    /**
     * A hash of the RLP encoding of an empty trie
     */
    val emptyTrieHash = Hash.hash(RLP.encodeValue(Bytes.EMPTY))

    fun dev(
      genesis: Genesis = Genesis(
        Bytes.ofUnsignedLong(0),
        UInt256.ONE,
        Bytes32.ZERO,
        Address.ZERO,
        Bytes32.ZERO,
        0L,
        Bytes.EMPTY,
        1_000_000L,
        emptyMap(),
        GenesisConfig(1337, 0, 0, 0, 0)
      )
    ): Block {
      return Block(
        BlockHeader(
          Hash.fromBytes(genesis.parentHash),
          emptyListHash,
          genesis.coinbase,
          emptyTrieHash,
          emptyHash,
          emptyHash,
          Bytes.wrap(ByteArray(256)),
          genesis.difficulty,
          UInt256.ZERO,
          Gas.valueOf(genesis.gasLimit),
          Gas.valueOf(0L),
          Instant.ofEpochSecond(genesis.timestamp),
          genesis.extraData,
          Hash.fromBytes(genesis.mixHash),
          UInt64.fromBytes(genesis.nonce)
        ),
        BlockBody(ArrayList(), ArrayList())
      )
    }
  }

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
