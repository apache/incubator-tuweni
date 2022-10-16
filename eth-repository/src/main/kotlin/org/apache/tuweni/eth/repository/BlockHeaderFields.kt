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
