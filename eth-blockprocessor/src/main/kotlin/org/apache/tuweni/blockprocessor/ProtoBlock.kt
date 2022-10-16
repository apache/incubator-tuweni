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
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.eth.repository.TransientStateRepository
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.bigints.UInt64
import org.apache.tuweni.units.ethereum.Gas
import java.time.Instant

/**
 * A block header that has not finished being sealed.
 */
data class SealableHeader(
  val parentHash: Hash,
  val stateRoot: Hash,
  val transactionsRoot: Hash,
  val receiptsRoot: Hash,
  val logsBloom: Bytes,
  val number: UInt256,
  val gasLimit: Gas,
  val gasUsed: Gas
) {

  /**
   * Seals the header into a block header
   */
  fun toHeader(
    ommersHash: Hash,
    coinbase: Address,
    difficulty: UInt256,
    timestamp: Instant,
    extraData: Bytes,
    mixHash: Hash,
    nonce: UInt64
  ): BlockHeader {
    return BlockHeader(
      parentHash,
      ommersHash,
      coinbase,
      stateRoot,
      transactionsRoot,
      receiptsRoot,
      logsBloom,
      difficulty,
      number,
      gasLimit,
      gasUsed,
      timestamp,
      extraData,
      mixHash,
      nonce
    )
  }
}

/**
 * A proto-block body is the representation of the intermediate form of a block body before being sealed.
 */
data class ProtoBlockBody(val transactions: List<Transaction>) {
  /**
   * Transforms the proto-block body into a valid block body by adding ommers.
   */
  fun toBlockBody(ommers: List<BlockHeader>): BlockBody {
    return BlockBody(transactions, ommers)
  }
}

/**
 * A proto-block is a block that has been executed but has not been sealed.
 * The header is missing the nonce and mixhash, and can still accept extra data.
 *
 * Proto-blocks are produced when transactions are executed, and can be turned into full valid blocks.
 */
class ProtoBlock(
  val header: SealableHeader,
  val body: ProtoBlockBody,
  val transactionReceipts: List<TransactionReceipt>,
  val stateChanges: TransientStateRepository
) {

  fun toBlock(
    ommers: List<BlockHeader>,
    coinbase: Address,
    difficulty: UInt256,
    timestamp: Instant,
    extraData: Bytes,
    mixHash: Hash,
    nonce: UInt64
  ): Block {
    val ommersHash = Hash.hash(RLP.encodeList { writer -> ommers.forEach { writer.writeValue(it.hash) } })
    return Block(
      header.toHeader(ommersHash, coinbase, difficulty, timestamp, extraData, mixHash, nonce),
      body.toBlockBody(ommers)
    )
  }
}
