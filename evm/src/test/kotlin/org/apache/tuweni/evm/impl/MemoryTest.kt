// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.evm.impl

import org.apache.tuweni.units.bigints.UInt256
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MemoryTest {

  @Test
  fun testNewMemorySize() {
    val mem = Memory()
    assertEquals(UInt256.valueOf(1), mem.newSize(UInt256.ZERO, UInt256.valueOf(4)))

    assertEquals(UInt256.valueOf(5), mem.newSize(UInt256.valueOf(128), UInt256.valueOf(32)))
  }

  @Test
  fun testMaxSize() {
    val mem = Memory()
    assertEquals(UInt256.MAX_VALUE, mem.newSize(UInt256.MAX_VALUE.subtract(2), UInt256.valueOf(4)))
  }
}
