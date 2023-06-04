// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
