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
package org.apache.tuweni.progpow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.units.bigints.UInt32;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ProgPoWMathTest {

  @ParameterizedTest
  @MethodSource("mergeVectors")
  void testMath(UInt32 a, UInt32 b, UInt32 r, UInt32 expected, String path) {
    UInt32 result = ProgPoWMath.math(a, b, r);
    assertEquals(expected, result, expected.toHexString() + " <> " + result.toHexString());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> mergeVectors() {
    return Stream
        .of(
            Arguments
                .of(
                    UInt32.fromHexString("0x8626BB1F"),
                    UInt32.fromHexString("0xBBDFBC4E"),
                    UInt32.fromHexString("0x883E5B49"),
                    UInt32.fromHexString("0x4206776D"),
                    "add"),
            Arguments
                .of(
                    UInt32.fromHexString("0x3F4BDFAC"),
                    UInt32.fromHexString("0xD79E414F"),
                    UInt32.fromHexString("0x36B71236"),
                    UInt32.fromHexString("0x4C5CB214"),
                    "mul"),
            Arguments
                .of(
                    UInt32.fromHexString("0x6D175B7E"),
                    UInt32.fromHexString("0xC4E89D4C"),
                    UInt32.fromHexString("0x944ECABB"),
                    UInt32.fromHexString("0x53E9023F"),
                    "mul_hi32"),
            Arguments
                .of(
                    UInt32.fromHexString("0x2EDDD94C"),
                    UInt32.fromHexString("0x7E70CB54"),
                    UInt32.fromHexString("0x3F472A85"),
                    UInt32.fromHexString("0x2EDDD94C"),
                    "min"),
            Arguments
                .of(
                    UInt32.fromHexString("0x61AE0E62"),
                    UInt32.fromHexString("0xe0596b32"),
                    UInt32.fromHexString("0x3F472A85"),
                    UInt32.fromHexString("0x61AE0E62"),
                    "min again (unsigned)"),
            Arguments
                .of(
                    UInt32.fromHexString("0x8A81E396"),
                    UInt32.fromHexString("0x3F4BDFAC"),
                    UInt32.fromHexString("0xCEC46E67"),
                    UInt32.fromHexString("0x1E3968A8"),
                    "rotl32"),
            Arguments
                .of(
                    UInt32.fromHexString("0x8A81E396"),
                    UInt32.fromHexString("0x7E70CB54"),
                    UInt32.fromHexString("0xDBE71FF7"),
                    UInt32.fromHexString("0x1E3968A8"),
                    "rotr32"),
            Arguments
                .of(
                    UInt32.fromHexString("0xA7352F36"),
                    UInt32.fromHexString("0xA0EB7045"),
                    UInt32.fromHexString("0x59E7B9D8"),
                    UInt32.fromHexString("0xA0212004"),
                    "bitwise and"),
            Arguments
                .of(
                    UInt32.fromHexString("0xC89805AF"),
                    UInt32.fromHexString("0x64291E2F"),
                    UInt32.fromHexString("0x1BDC84A9"),
                    UInt32.fromHexString("0xECB91FAF"),
                    "bitwise or"),
            Arguments
                .of(
                    UInt32.fromHexString("0x760726D3"),
                    UInt32.fromHexString("0x79FC6A48"),
                    UInt32.fromHexString("0xC675CAC5"),
                    UInt32.fromHexString("0x0FFB4C9B"),
                    "bitwise xor"),
            Arguments
                .of(
                    UInt32.fromHexString("0x75551D43"),
                    UInt32.fromHexString("0x3383BA34"),
                    UInt32.fromHexString("0x2863AD31"),
                    UInt32.fromHexString("0x00000003"),
                    "clz (leading zeros)"),
            Arguments
                .of(
                    UInt32.fromHexString("0xEA260841"),
                    UInt32.fromHexString("0xE92C44B7"),
                    UInt32.fromHexString("0xF83FFE7D"),
                    UInt32.fromHexString("0x0000001B"),
                    "popcount (number of 1s)"));
  }
}
