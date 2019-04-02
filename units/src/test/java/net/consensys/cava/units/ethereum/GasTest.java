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
package net.consensys.cava.units.ethereum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GasTest {

  @Test
  void testOverflowThroughAddition() {
    Gas max = Gas.valueOf(Long.MAX_VALUE);
    assertThrows(ArithmeticException.class, () -> {
      max.add(Gas.valueOf(1L));
    });
  }

  @Test
  void testOverflow() {
    assertThrows(IllegalArgumentException.class, () -> {
      Gas.valueOf(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
    });
  }

  @Test
  void testGetWeiPrice() {
    Gas gas = Gas.valueOf(5L);
    Wei result = gas.priceFor(Wei.valueOf(3L));
    assertEquals(15, result.intValue());
  }

  @Test
  void testReuseConstants() {
    List<Gas> oneTime = new ArrayList<>();
    for (int i = 0; i < 128; i++) {
      oneTime.add(Gas.valueOf((long) i));
    }
    List<Gas> secondTime = new ArrayList<>();
    for (int i = 0; i < 128; i++) {
      secondTime.add(Gas.valueOf((long) i));
    }
    for (int i = 0; i < 128; i++) {
      Gas first = oneTime.get(i);
      Gas second = secondTime.get(i);
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
      Gas.valueOf(-1L);
    });
  }

  @Test
  void testNegativeBigInteger() {
    assertThrows(IllegalArgumentException.class, () -> {
      Gas.valueOf(BigInteger.valueOf(-123L));
    });
  }

}
