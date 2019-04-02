/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.eth.repository

/**
 * Block header index fields.
 *
 */
enum class BlockHeaderFields
/**
 * Default constructor.
 *
 * @param fieldName the name to use when indexing the field with Lucene.
 */
constructor(val fieldName: String) {
  PARENT_HASH("parentHash"),
  OMMERS_HASH("ommersHash"),
  COINBASE("coinbase"),
  STATE_ROOT("stateRoot"),
  DIFFICULTY("difficulty"),
  NUMBER("number"),
  GAS_LIMIT("gasLimit"),
  GAS_USED("gasUsed"),
  EXTRA_DATA("extraData"),
  TIMESTAMP("timestamp"),
  TOTAL_DIFFICULTY("totalDifficulty")
}
