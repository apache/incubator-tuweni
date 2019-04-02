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
package org.apache.tuweni.units.ethereum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class WeiTest {

  @Test
  void testReuseConstants() {
    List<Wei> oneTime = new ArrayList<>();
    for (int i = 0; i < 128; i++) {
      oneTime.add(Wei.valueOf((long) i));
    }
    List<Wei> secondTime = new ArrayList<>();
    for (int i = 0; i < 128; i++) {
      secondTime.add(Wei.valueOf((long) i));
    }
    for (int i = 0; i < 128; i++) {
      Wei first = oneTime.get(i);
      Wei second = secondTime.get(i);
      if (i <= 64) {
        assertSame(first, second);
      } else {
        assertNotSame(first, second);
        assertEquals(first, second);
      }
    }
  }

  @Test
  void testNegativeLong() {
    assertThrows(IllegalArgumentException.class, () -> {
      Wei.valueOf(-1L);
    });
  }

  @Test
  void testNegativeBigInteger() {
    assertThrows(IllegalArgumentException.class, () -> {
      Wei.valueOf(BigInteger.valueOf(-123L));
    });
  }
}
