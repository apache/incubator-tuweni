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
