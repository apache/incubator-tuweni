// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.progpow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.units.bigints.UInt32;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MergeTest {

  @ParameterizedTest
  @MethodSource("mergeVectors")
  void testMerge(UInt32 a, UInt32 b, UInt32 r, UInt32 result, String path) {
    assertEquals(result, ProgPoW.merge(a, b, r));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> mergeVectors() {
    return Stream.of(
        Arguments.of(
            UInt32.fromHexString("0x3B0BB37D"),
            UInt32.fromHexString("0xA0212004"),
            UInt32.fromHexString("0x9BD26AB0"),
            UInt32.fromHexString("0x3CA34321"),
            "mul/add"),
        Arguments.of(
            UInt32.fromHexString("0x10C02F0D"),
            UInt32.fromHexString("0x870FA227"),
            UInt32.fromHexString("0xD4F45515"),
            UInt32.fromHexString("0x91C1326A"),
            "xor/mul"),
        Arguments.of(
            UInt32.fromHexString("0x24D2BAE4"),
            UInt32.fromHexString("0x0FFB4C9B"),
            UInt32.fromHexString("0x7FDBC2F2"),
            UInt32.fromHexString("0x2EDDD94C"),
            "rotl/xor"),
        Arguments.of(
            UInt32.fromHexString("0xDA39E821"),
            UInt32.fromHexString("0x089C4008"),
            UInt32.fromHexString("0x8B6CD8C3"),
            UInt32.fromHexString("0x8A81E396"),
            "rotr/xor"));
  }
}
