// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.evmdsl

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.units.bigints.UInt256

/**
 * Creates just enough EVM opcodes to wrap a deployment of a smart contract.
 *
 * Pushes the contract bytecode into memory, and returns the whole bytecode as the result of the execution of the deployment execution.
 *
 */
class Deployment(val code: Code) {

  companion object {
    /**
     * Generates a bytecode of a deployment of code of an exact size.
     * @param size the size of the contract deployed
     */
    fun generate(size: Int) = Deployment(Code.generate(size))
  }

  fun toBytes(): Bytes {
    val codeBytes = code.toBytes()
    val deployment = Code(
      buildList {
        var location = UInt256.ZERO
        println(Bytes.segment(codeBytes))
        for (segment in Bytes.segment(codeBytes)) {
          this.add(Push(segment)) // push a segment of code to store
          this.add(Push(location)) // set the location of the memory to store
          this.add(Mstore)
          location += UInt256.valueOf(32)
        }
        this.add(Push(Bytes.ofUnsignedInt(codeBytes.size().toLong()))) // length
        this.add(Push(Bytes.fromHexString("0x00"))) // location
        this.add(Return) // return the code
      },
    )
    return deployment.toBytes()
  }
}
