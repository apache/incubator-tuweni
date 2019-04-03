/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.bytes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class Bytes48Test {

  @Test
  void failsWhenWrappingArraySmallerThan48() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes48.wrap(new byte[31]));
    assertEquals("Expected 48 bytes but got 31", exception.getMessage());
  }

  @Test
  void failsWhenWrappingArrayLargerThan48() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes48.wrap(new byte[33]));
    assertEquals("Expected 48 bytes but got 33", exception.getMessage());
  }

  @Test
  void rightPadAValueToBytes48() {
    Bytes48 b48 = Bytes48.rightPad(Bytes.of(1, 2, 3));
    assertEquals(48, b48.size());
    for (int i = 3; i < 48; ++i) {
      assertEquals((byte) 0, b48.get(i));
    }
    assertEquals((byte) 1, b48.get(0));
    assertEquals((byte) 2, b48.get(1));
    assertEquals((byte) 3, b48.get(2));
  }

  @Test
  void leftPadAValueToBytes48() {
    Bytes48 b48 = Bytes48.leftPad(Bytes.of(1, 2, 3));
    assertEquals(48, b48.size());
    for (int i = 0; i < 28; ++i) {
      assertEquals((byte) 0, b48.get(i));
    }
    assertEquals((byte) 1, b48.get(45));
    assertEquals((byte) 2, b48.get(46));
    assertEquals((byte) 3, b48.get(47));
  }

  @Test
  void failsWhenLeftPaddingValueLargerThan48() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes48.leftPad(MutableBytes.create(49)));
    assertEquals("Expected at most 48 bytes but got 49", exception.getMessage());
  }

  @Test
  void failsWhenRightPaddingValueLargerThan48() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes48.rightPad(MutableBytes.create(49)));
    assertEquals("Expected at most 48 bytes but got 49", exception.getMessage());
  }
}
