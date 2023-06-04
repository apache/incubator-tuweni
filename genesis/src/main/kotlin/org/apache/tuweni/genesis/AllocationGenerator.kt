// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
