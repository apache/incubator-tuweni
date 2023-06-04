// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.repository

/**
 * Block header index fields.
 * @param fieldName the name to use when indexing the field with Lucene.
 */
enum class BlockHeaderFields(val fieldName: String) {
  /**
   * Parent hash
   */
  PARENT_HASH("parentHash"),

  /**
   * Ommers hash
   */
  OMMERS_HASH("ommersHash"),

  /**
   * Coinbase address
   */
  COINBASE("coinbase"),

  /**
   * State root
   */
  STATE_ROOT("stateRoot"),

  /**
   * Difficulty
   */
  DIFFICULTY("difficulty"),

  /**
   * Block number
   */
  NUMBER("number"),

  /**
   * Gas limit
   */
  GAS_LIMIT("gasLimit"),

  /**
   * Gas used
   */
  GAS_USED("gasUsed"),

  /**
   * Extra data
   */
  EXTRA_DATA("extraData"),

  /**
   * Timestamp of the block
   */
  TIMESTAMP("timestamp"),

  /**
   * Total difficulty
   */
  TOTAL_DIFFICULTY("totalDifficulty")
}
