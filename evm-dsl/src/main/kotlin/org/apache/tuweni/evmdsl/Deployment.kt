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
      }
    )
    return deployment.toBytes()
  }
}
