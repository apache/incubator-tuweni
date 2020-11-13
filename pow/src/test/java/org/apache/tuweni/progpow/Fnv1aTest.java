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
