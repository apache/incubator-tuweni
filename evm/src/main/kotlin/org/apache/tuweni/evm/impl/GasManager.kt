// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.evm.impl

import org.apache.tuweni.units.ethereum.Gas

class GasManager(val gas: Gas) {

  var gasCost = Gas.ZERO
  var lastGasCost = Gas.ZERO

  fun add(g: Long) {
    add(Gas.valueOf(g))
  }

  fun add(gas: Gas) {
    lastGasCost = gas
    gasCost = gasCost.addSafe(gas)
  }

  fun gasLeft(): Gas {
    if (gas < gasCost || gas.tooHigh() || gasCost.tooHigh()) {
      return Gas.ZERO
    }
    return gas.subtract(gasCost)
  }

  fun hasGasLeft(): Boolean {
    return !gasLeft().isZero
  }

  fun lastGasCost(): Gas = lastGasCost
}
