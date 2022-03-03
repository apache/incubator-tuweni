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
package org.apache.tuweni.genesis

import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.Address
import org.apache.tuweni.units.bigints.UInt256

data class Allocation(val address: Address, val amount: UInt256, val keyPair: SECP256K1.KeyPair)

class AllocationGenerator {

  fun createAllocations(numberAllocations: Int, amount: UInt256): List<Allocation> {
    val allocs = mutableListOf<Allocation>()
    for (i in 0..numberAllocations) {
      val keyPair = SECP256K1.KeyPair.random()
      allocs.add(Allocation(Address.fromPublicKey(keyPair.publicKey()), amount, keyPair))
    }
    return allocs
  }
}
