/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;

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
}
