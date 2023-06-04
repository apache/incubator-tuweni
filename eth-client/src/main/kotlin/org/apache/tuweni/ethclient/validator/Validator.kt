// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
