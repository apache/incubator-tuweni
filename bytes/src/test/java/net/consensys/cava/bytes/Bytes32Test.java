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
package net.consensys.cava.bytes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class Bytes32Test {

  @Test
  void failsWhenWrappingArraySmallerThan32() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes32.wrap(new byte[31]));
    assertEquals("Expected 32 bytes but got 31", exception.getMessage());
  }

  @Test
  void failsWhenWrappingArrayLargerThan32() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes32.wrap(new byte[33]));
    assertEquals("Expected 32 bytes but got 33", exception.getMessage());
  }

  @Test
  void leftPadAValueToBytes32() {
    Bytes32 b32 = Bytes32.leftPad(Bytes.of(1, 2, 3));
    assertEquals(32, b32.size());
    for (int i = 0; i < 28; ++i) {
      assertEquals((byte) 0, b32.get(i));
    }
    assertEquals((byte) 1, b32.get(29));
    assertEquals((byte) 2, b32.get(30));
    assertEquals((byte) 3, b32.get(31));
  }

  @Test
  void rightPadAValueToBytes32() {
    Bytes32 b32 = Bytes32.rightPad(Bytes.of(1, 2, 3));
    assertEquals(32, b32.size());
    for (int i = 3; i < 32; ++i) {
      assertEquals((byte) 0, b32.get(i));
    }
    assertEquals((byte) 1, b32.get(0));
    assertEquals((byte) 2, b32.get(1));
    assertEquals((byte) 3, b32.get(2));
  }

  @Test
  void failsWhenLeftPaddingValueLargerThan32() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes32.leftPad(MutableBytes.create(33)));
    assertEquals("Expected at most 32 bytes but got 33", exception.getMessage());
  }

  @Test
  void failsWhenRightPaddingValueLargerThan32() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes32.rightPad(MutableBytes.create(33)));
    assertEquals("Expected at most 32 bytes but got 33", exception.getMessage());
  }
}
