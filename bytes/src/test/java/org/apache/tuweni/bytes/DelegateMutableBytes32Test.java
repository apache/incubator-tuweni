// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DelegateMutableBytes32Test {

  @Test
  void failsWhenWrappingArraySmallerThan32() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> MutableBytes32.wrap(MutableBytes.wrap(new byte[31])));
    assertEquals("Expected 32 bytes but got 31", exception.getMessage());
  }

  @Test
  void failsWhenWrappingArrayLargerThan32() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> MutableBytes32.wrap(MutableBytes.wrap(new byte[33])));
    assertEquals("Expected 32 bytes but got 33", exception.getMessage());
  }

  @Test
  void testSize() {
    assertEquals(32, DelegatingMutableBytes32.delegateTo(MutableBytes32.create()).size());
  }

  @Test
  void testCopy() {
    Bytes bytes = DelegatingMutableBytes32.delegateTo(MutableBytes32.create()).copy();
    assertEquals(bytes, bytes.copy());
    assertEquals(bytes, bytes.mutableCopy());
  }

  @Test
  void testSlice() {
    Bytes bytes = DelegatingMutableBytes32.delegateTo(MutableBytes32.create()).copy();
    assertEquals(Bytes.wrap(new byte[] {bytes.get(2), bytes.get(3), bytes.get(4)}), bytes.slice(2, 3));
  }
}
