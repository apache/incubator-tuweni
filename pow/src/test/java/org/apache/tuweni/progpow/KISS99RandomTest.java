// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.progpow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.units.bigints.UInt32;

import org.junit.jupiter.api.Test;

class KISS99RandomTest {

  @Test
  void testRounds() {
    KISS99Random random = new KISS99Random(
        UInt32.valueOf(362436069),
        UInt32.valueOf(521288629),
        UInt32.valueOf(123456789),
        UInt32.valueOf(380116160));
    assertEquals(UInt32.valueOf(769445856), random.generate());
    assertEquals(UInt32.valueOf(742012328), random.generate());
    assertEquals(UInt32.valueOf(2121196314), random.generate());
    assertEquals(UInt32.fromHexString("0xa73a60ce"), random.generate());
    for (int i = 0; i < 99995; i++) {
      random.generate();
    }
    assertEquals(UInt32.valueOf(941074834), random.generate());
  }
}
