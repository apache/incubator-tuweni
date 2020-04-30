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

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class Keccakf800Test {

  @Test
  void testKeccak() {
    int[] vector =
        new int[] {0xCCDDEEFF, 0x8899AABB, 0x44556677, 0x00112233, 0x33221100, 0x77665544, 0xBBAA9988, 0xFFEEDDCC};
    Bytes32 test = Bytes32
        .wrap(
            Bytes
                .concatenate(
                    IntStream
                        .of(vector)
                        .mapToObj(i -> Bytes.wrap(ByteBuffer.allocate(4).putInt(i).array()).reverse())
                        .toArray(Bytes[]::new)));

    int[] expectedResult =
        new int[] {0x464830EE, 0x7BA4D0DD, 0x969E1798, 0xCEC50EB6, 0x7872E2EA, 0x597E3634, 0xE380E73D, 0x2F89D1E6};
    Bytes32 expectedResultBytes = Bytes32
        .wrap(
            Bytes
                .concatenate(
                    IntStream
                        .of(expectedResult)
                        .mapToObj(i -> Bytes.wrap(ByteBuffer.allocate(4).putInt(i).array()).reverse())
                        .toArray(Bytes[]::new)));

    Bytes32 result = Keccakf800.keccakF800Progpow(test, 0x123456789ABCDEF0L, Bytes32.ZERO);
    assertEquals(expectedResultBytes, result);
  }

  @Test
  void testKeccakWithDigest() {
    int[] vector =
        new int[] {0xCCDDEEFF, 0x8899AABB, 0x44556677, 0x00112233, 0x33221100, 0x77665544, 0xBBAA9988, 0xFFEEDDCC};
    Bytes32 test = Bytes32
        .wrap(
            Bytes
                .concatenate(
                    IntStream
                        .of(vector)
                        .mapToObj(i -> Bytes.wrap(ByteBuffer.allocate(4).putInt(i).array()).reverse())
                        .toArray(Bytes[]::new)));
    int[] expectedResult =
        new int[] {0x47CD7C5B, 0xD9FDBE2D, 0xAC5C895B, 0xFF67CE8E, 0x6B5AEB0D, 0xE1C6ECD2, 0x003D3862, 0xCE8E72C3};
    Bytes32 expectedResultBytes = Bytes32
        .wrap(
            Bytes
                .concatenate(
                    IntStream
                        .of(expectedResult)
                        .mapToObj(i -> Bytes.wrap(ByteBuffer.allocate(4).putInt(i).array()).reverse())
                        .toArray(Bytes[]::new)));

    Bytes32 result = Keccakf800
        .keccakF800Progpow(
            test,
            0xEE304846DDD0A47BL,
            Bytes32.fromHexString("0x0598F11166B48AC5719CFF105F0ACF9D162FFA18EF8E790521470C777D767492"));
    assertEquals(expectedResultBytes, result);
  }
}
