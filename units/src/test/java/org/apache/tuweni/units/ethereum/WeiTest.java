// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.units.ethereum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.units.bigints.UInt256;

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
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          Wei.valueOf(-1L);
        });
  }

  @Test
  void testNegativeBigInteger() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          Wei.valueOf(BigInteger.valueOf(-123L));
        });
  }

  @Test
  void testFromEth() {
    assertEquals(Wei.valueOf((long) Math.pow(10, 18)), Wei.fromEth(1));
  }

  @Test
  void testToInt() {
    assertTrue(Wei.valueOf(100L).fitsInt());
    assertEquals(100, Wei.valueOf(100L).toInt());
  }

  @Test
  void testToLong() {
    assertTrue(Wei.valueOf(100L).fitsLong());
    assertEquals(100L, Wei.valueOf(100L).toLong());
  }

  @Test
  void toBigIntegerIsPositive() {
    assertEquals(1, Wei.valueOf(UInt256.MAX_VALUE).toBigInteger().signum());
  }
}
