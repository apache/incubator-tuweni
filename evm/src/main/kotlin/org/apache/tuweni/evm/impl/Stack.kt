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

import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.bytes.MutableBytes
import org.apache.tuweni.units.bigints.UInt256

class Stack(private val maxSize: Int = 1025) {

  private val mutableStack = MutableBytes.create(maxSize * 32)
  private var size = 0

  fun get(i: Int): UInt256? {
    if (i >= size) {
      return null
    }
    return mutableStack.slice((size - i - 1) * 32, 32)?.let { UInt256.fromBytes(it) }
  }

  fun pop(): UInt256? {
    if (size <= 0) {
      return null
    }
    size--
    return mutableStack.slice(size * 32, 32)?.let { UInt256.fromBytes(it) }
  }

  fun push(value: Bytes32): Boolean {
    if (size >= maxSize) {
      return false
    }
    mutableStack.set((size * 32), value)
    size++
    return true
  }

  fun size(): Int = this.size

  fun overflowed(): Boolean = size >= maxSize

  fun set(i: Int, elt: UInt256) {
    mutableStack.set((size - i - 1) * 32, elt)
  }
}
