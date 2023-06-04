// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.repository

/**
 * Transaction receipt index fields.
 * @param fieldName the name to use when indexing the field with Lucene.
 */
enum class TransactionReceiptFields(val fieldName: String) {
  /**
   * Index of a transaction in a block
   */
  INDEX("index"),

  /**
   * Hash of a transaction
   */
  TRANSACTION_HASH("txHash"),

  /**
   * Block hash
   */
  BLOCK_HASH("blockHash"),

  /**
   * Logger address
   */
  LOGGER("logger"),

  /**
   * Log topic
   */
  LOG_TOPIC("logTopic"),

  /**
   * Bloom filter value
   */
  BLOOM_FILTER("bloomFilter"),

  /**
   * State root
   */
  STATE_ROOT("stateRoot"),

  /**
   * Cumulative gas used
   */
  CUMULATIVE_GAS_USED("cumulativeGasUsed"),

  /**
   * Status of the transaction
   */
  STATUS("status")
}
