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
package org.apache.tuweni.ethclient.validator

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.eth.Block
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.trie.MerklePatriciaTrie
import org.apache.tuweni.units.bigints.UInt256

/**
 * Validates all transactions have a chain id that matches our chain id
 *
 * @param chainId the chain id we want to see for all transactions
 */
class ChainIdValidator(from: UInt256? = null, to: UInt256? = null, private val chainId: Int) : Validator(from, to) {

  override fun _validate(block: Block): Boolean {
    for (tx in block.body.transactions) {
      if (chainId != tx.chainId) {
        return false
      }
    }
    return true
  }
}

/**
 * Checks that the block header has the right transactions root hash.
 */
class TransactionsHashValidator(from: UInt256? = null, to: UInt256? = null) : Validator(from, to) {

  override fun _validate(block: Block): Boolean = runBlocking {
    val transactionsTrie = MerklePatriciaTrie.storingBytes()
    var counter = 0L

    for (tx in block.body.transactions) {
      val indexKey = RLP.encodeValue(UInt256.valueOf(counter).trimLeadingZeros())
      transactionsTrie.put(indexKey, tx.toBytes())
      counter++
    }

    return@runBlocking transactionsTrie.rootHash().equals(block.header.transactionsRoot)
  }
}
