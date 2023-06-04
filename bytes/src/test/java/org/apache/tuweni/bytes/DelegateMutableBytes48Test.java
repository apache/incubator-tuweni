// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DelegateMutableBytes48Test {

  @Test
  void failsWhenWrappingArraySmallerThan48() {
    Throwable exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> MutableBytes48.wrap(MutableBytes.wrap(new byte[47])));
    assertEquals("Expected 48 bytes but got 47", exception.getMessage());
  }

  @Test
  void failsWhenWrappingArrayLargerThan48() {
    Throwable exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> MutableBytes48.wrap(MutableBytes.wrap(new byte[49])));
    assertEquals("Expected 48 bytes but got 49", exception.getMessage());
  }

  @Test
  void testSize() {
    assertEquals(48, DelegatingMutableBytes48.delegateTo(MutableBytes48.create()).size());
  }

  @Test
  void testCopy() {
    Bytes bytes = DelegatingMutableBytes48.delegateTo(MutableBytes48.create()).copy();
    assertEquals(bytes, bytes.copy());
    assertEquals(bytes, bytes.mutableCopy());
  }

  @Test
  void testSlice() {
    Bytes bytes = DelegatingMutableBytes48.delegateTo(MutableBytes48.create()).copy();
    assertEquals(
        Bytes.wrap(new byte[] {bytes.get(2), bytes.get(3), bytes.get(4)}), bytes.slice(2, 3));
  }
}
