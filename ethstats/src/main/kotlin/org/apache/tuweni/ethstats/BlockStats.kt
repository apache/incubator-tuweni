// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethstats

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.units.bigints.UInt256

/**
 * Block statistics reported to ethnetstats.
 *
 * @param number the block number
 * @param hash the block hash
 * @param parentHash the hash of the parent block, or null.
 * @param timestamp the timestamp of the block
 * @param miner the coinbase address of the block
 * @param gasUsed the gas used by the block
 * @param gasLimit the gas limit of the block
 * @param difficulty the difficulty of the block
 * @param totalDifficulty the total difficulty up to this block (including this block)
 * @param transactions the list of transaction hashes associated with the block
 * @param transactionsRoot the hash root of transactions
 * @param stateRoot the hash root of the state
 * @param uncles the block ommers associated with this block
 */
@JsonPropertyOrder(alphabetic = true)
data class BlockStats(
  val number: UInt256,
  val hash: Hash,
  val parentHash: Hash,
  val timestamp: Long,
  val miner: Address,
  val gasUsed: Long,
  val gasLimit: Long,
  val difficulty: UInt256,
  val totalDifficulty: UInt256,
  val transactions: List<TxStats>,
  val transactionsRoot: Hash,
  val stateRoot: Hash,
  val uncles: List<Hash>,
) {

  @JsonGetter("number")
  fun getBlockNumber() = number.toLong()
}
