// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;

import org.junit.jupiter.api.Test;

class Base32Test {

  @Test
  void shouldEncodeByteArray() {
    String s = Base32.encodeBytes(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals("AEBAGBAFAYDQQ===", s);
  }

  @Test
  void shouldEncodeBytesValue() {
    String s = Base32.encode(Bytes.of(1, 2, 3, 4, 5, 6, 7, 8));
    assertEquals("AEBAGBAFAYDQQ===", s);
  }

  @Test
  void shouldDecodeToByteArray() {
    byte[] bytes = Base32.decodeBytes("AEBAGBAFAYDQQ===");
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, bytes);
  }

  @Test
  void shouldDecodeToBytesValue() {
    Bytes bytes = Base32.decode("AEBAGBAFAYDQQ===");
    assertEquals(Bytes.of(1, 2, 3, 4, 5, 6, 7, 8), bytes);
  }
}
