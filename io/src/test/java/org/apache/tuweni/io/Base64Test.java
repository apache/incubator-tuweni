// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;

import org.junit.jupiter.api.Test;

class Base64Test {

  @Test
  void shouldEncodeByteArray() {
    String s = Base64.encodeBytes(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals("AQIDBAUGBwg=", s);
  }

  @Test
  void shouldEncodeBytesValue() {
    String s = Base64.encode(Bytes.of(1, 2, 3, 4, 5, 6, 7, 8));
    assertEquals("AQIDBAUGBwg=", s);
  }

  @Test
  void shouldDecodeToByteArray() {
    byte[] bytes = Base64.decodeBytes("AQIDBAUGBwg=");
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, bytes);
  }

  @Test
  void shouldDecodeToBytesValue() {
    Bytes bytes = Base64.decode("AQIDBAUGBwg=");
    assertEquals(Bytes.of(1, 2, 3, 4, 5, 6, 7, 8), bytes);
  }

  @Test
  void shouldEncodeValueWithSlashes() {
    String value = Base64.encode(Bytes.fromHexString("deadbeefffffff"));
    assertEquals("3q2+7////w==", value);
  }
}
