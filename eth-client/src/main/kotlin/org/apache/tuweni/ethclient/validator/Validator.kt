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

import org.apache.tuweni.eth.Block
import org.apache.tuweni.units.bigints.UInt256

/**
 * Validator that checks a particular block.
 */
abstract class Validator(val from: UInt256?, val to: UInt256?) {

  /**
   * Validates the block
   * @param block the block to validate
   * @return true if the block is deemed valid, false otherwise
   */
  fun validate(block: Block): Boolean {
    if (from?.let { it <= block.header.number } != false && to?.let { it >= block.header.number } != false) {
      return _validate(block)
    }
    return true
  }

  protected abstract fun _validate(block: Block): Boolean
}
