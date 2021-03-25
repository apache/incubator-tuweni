package org.apache.tuweni.evm.impl

import org.apache.tuweni.units.ethereum.Gas

class GasManager(val gas: Gas) {

  var gasCost = 0L

  fun add(gas: Long) {
    gasCost += gas
  }

  fun gasLeft(): Long {
    return gas.toLong() - gasCost
  }
}
