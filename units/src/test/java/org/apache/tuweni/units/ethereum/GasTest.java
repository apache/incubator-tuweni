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
package org.apache.tuweni.units.ethereum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GasTest {

  @Test
  void testOverflowThroughAddition() {
    Gas max = Gas.valueOf(Long.MAX_VALUE);
    assertTrue(max.add(Gas.valueOf(1L)).tooHigh());
  }

  @Test
  void testOverflow() {
    Gas value = Gas.valueOf(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
    assertTrue(value.tooHigh());
  }

  @Test
  void testOverflowAndAddMore() {
    Gas first = Gas.valueOf(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
    Gas second = Gas.valueOf(1);
    assertTrue(first.add(second).tooHigh());
  }

  @Test
  void testToLong() {
    Gas gas = Gas.valueOf(32L);
    assertEquals(32L, gas.toLong());
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

  @Test
  void testConstantReuse() {
    Gas gas = Gas.valueOf(UInt256.valueOf(1L));
    Gas otherGas = Gas.valueOf(UInt256.valueOf(1L));
    assertSame(gas, otherGas);
  }

  @Test
  void testOverflowUInt256() {
    assertTrue(Gas.valueOf(UInt256.MAX_VALUE).tooHigh());
  }

  @Test
  void testToBytes() {
    assertEquals(
        Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001"),
        Gas.valueOf(1L).toBytes());
  }

  @Test
  void testToMinimalBytes() {
    assertEquals(Bytes.fromHexString("0x01"), Gas.valueOf(1L).toMinimalBytes());
  }

  @Test
  void testMinimum() {
    assertEquals(Gas.valueOf(1), Gas.minimum(Gas.valueOf(1), Gas.valueOf(2)));
  }
}
