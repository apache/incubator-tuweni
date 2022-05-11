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
