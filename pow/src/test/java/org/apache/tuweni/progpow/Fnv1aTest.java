// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.progpow;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.units.bigints.UInt32;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for https://github.com/ifdefelse/ProgPOW/blob/master/test-vectors.md#fnv1a
 */
class Fnv1aTest {


  @ParameterizedTest()
  @MethodSource("vectorSupplier")
  void testVector(UInt32 h, UInt32 d, UInt32 expected) {
    UInt32 result = ProgPoW.fnv1a(h, d);
    assertEquals(expected, result);
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> vectorSupplier() {
    return Stream
        .of(
            Arguments
                .arguments(
                    UInt32.fromHexString("0x811C9DC5"),
                    UInt32.fromHexString("0xDDD0A47B"),
                    UInt32.fromHexString("0xD37EE61A")));
  }

}
