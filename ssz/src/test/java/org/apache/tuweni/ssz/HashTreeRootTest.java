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
package org.apache.tuweni.ssz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.Hash;
import org.apache.tuweni.junit.BouncyCastleExtension;

import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class HashTreeRootTest {

  @Test
  void hashBoolean() {
    assertEquals(
        Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000"),
        SSZ.hashTreeRoot(SSZ.encodeBoolean(false)));
    assertEquals(
        Bytes.fromHexString("0x0100000000000000000000000000000000000000000000000000000000000000"),
        SSZ.hashTreeRoot(SSZ.encodeBoolean(true)));
  }

  @Test
  void hashUint8() {
    assertEquals(
        Bytes.fromHexString("0x0400000000000000000000000000000000000000000000000000000000000000"),
        SSZ.hashTreeRoot(SSZ.encodeUInt(4, 8)));
  }

  @Test
  void hashBytes32() {
    Bytes32 someBytes = Bytes32.random();
    assertEquals(someBytes, SSZ.hashTreeRoot(someBytes));
  }

  @Test
  void hashBytes34() {
    Bytes someBytes = Bytes.random(34);
    assertEquals(Hash.keccak256(someBytes), SSZ.hashTreeRoot(someBytes));
  }

  @Test
  void list() {
    Bytes[] list = new Bytes[] {
        Bytes.wrap(new byte[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}),
        Bytes.wrap(new byte[] {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2}),
        Bytes.wrap(new byte[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3}),
        Bytes.wrap(new byte[] {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4}),
        Bytes.wrap(new byte[] {5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5}),
        Bytes.wrap(new byte[] {6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6}),
        Bytes.wrap(new byte[] {7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7}),
        Bytes.wrap(new byte[] {8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8}),
        Bytes.wrap(new byte[] {9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9}),
        Bytes.wrap(new byte[] {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10})};
    assertEquals(
        Bytes.fromHexString("0x839D98509E2EFC53BD1DEA17403921A89856E275BBF4D56C600CC3F6730AAFFA"),
        SSZ.hashTreeRoot(list));
  }

  @Test
  void list2() {
    byte[] _1s = new byte[32];
    byte[] _2s = new byte[32];
    byte[] _3s = new byte[32];
    byte[] _4s = new byte[32];
    byte[] _5s = new byte[32];
    byte[] _6s = new byte[32];
    byte[] _7s = new byte[32];
    byte[] _8s = new byte[32];
    byte[] _9s = new byte[32];
    byte[] _as = new byte[32];
    Arrays.fill(_1s, (byte) 1);
    Arrays.fill(_2s, (byte) 2);
    Arrays.fill(_3s, (byte) 3);
    Arrays.fill(_4s, (byte) 4);
    Arrays.fill(_5s, (byte) 5);
    Arrays.fill(_6s, (byte) 6);
    Arrays.fill(_7s, (byte) 7);
    Arrays.fill(_8s, (byte) 8);
    Arrays.fill(_9s, (byte) 9);
    Arrays.fill(_as, (byte) 10);

    assertEquals(
        Bytes.fromHexString("0x55DC6699E7B5713DD9102224C302996F931836C6DAE9A4EC6AB49C966F394685"),
        SSZ
            .hashTreeRoot(
                Bytes.wrap(_1s),
                Bytes.wrap(_2s),
                Bytes.wrap(_3s),
                Bytes.wrap(_4s),
                Bytes.wrap(_5s),
                Bytes.wrap(_6s),
                Bytes.wrap(_7s),
                Bytes.wrap(_8s),
                Bytes.wrap(_9s),
                Bytes.wrap(_as)));
  }
}
