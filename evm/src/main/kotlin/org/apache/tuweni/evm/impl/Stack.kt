package org.apache.tuweni.evm.impl

import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.bytes.MutableBytes
import org.apache.tuweni.units.bigints.UInt256

class Stack(private val maxSize : Int = 1024) {

  private val mutableStack = MutableBytes.create(maxSize * 32)
  private var size = 0

  fun pop() : UInt256? {
    if (size <= 0) {
      return null
    }
    size--
    return mutableStack.slice(size * 32, 32)?.let { UInt256.fromBytes(it) }
  }

  fun push(value: Bytes32) : Boolean {
    if (size >= maxSize) {
      return false
    }
    mutableStack.set((size * 32), value)
    size++
    return true
  }

  fun size(): Int = this.size
}
