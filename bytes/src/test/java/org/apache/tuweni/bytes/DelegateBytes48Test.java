// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DelegateBytes48Test {

  @Test
  void failsWhenWrappingArraySmallerThan48() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> Bytes48.wrap(Bytes.wrap(new byte[47])));
    assertEquals("Expected 48 bytes but got 47", exception.getMessage());
  }

  @Test
  void failsWhenWrappingArrayLargerThan48() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> Bytes48.wrap(Bytes.wrap(new byte[49])));
    assertEquals("Expected 48 bytes but got 49", exception.getMessage());
  }

  @Test
  void failsWhenLeftPaddingValueLargerThan48() {
    Throwable exception =
        assertThrows(
            IllegalArgumentException.class, () -> Bytes48.leftPad(MutableBytes.create(49)));
    assertEquals("Expected at most 48 bytes but got 49", exception.getMessage());
  }

  @Test
  void failsWhenRightPaddingValueLargerThan48() {
    Throwable exception =
        assertThrows(
            IllegalArgumentException.class, () -> Bytes48.rightPad(MutableBytes.create(49)));
    assertEquals("Expected at most 48 bytes but got 49", exception.getMessage());
  }

  @Test
  void testSize() {
    assertEquals(48, new DelegatingBytes48(Bytes48.random()).size());
  }

  @Test
  void testCopy() {
    Bytes bytes = new DelegatingBytes48(Bytes48.random()).copy();
    assertEquals(bytes, bytes.copy());
    assertEquals(bytes, bytes.mutableCopy());
  }

  @Test
  void testSlice() {
    Bytes bytes = new DelegatingBytes48(Bytes48.random()).copy();
    assertEquals(
        Bytes.wrap(new byte[] {bytes.get(2), bytes.get(3), bytes.get(4)}), bytes.slice(2, 3));
  }
}
