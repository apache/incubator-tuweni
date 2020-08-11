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

import org.apache.tuweni.bytes.Bytes32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class Keccakf800 {

  private static int[] keccakRoundConstants = {
      0x00000001,
      0x00008082,
      0x0000808A,
      0x80008000,
      0x0000808B,
      0x80000001,
      0x80008081,
      0x00008009,
      0x0000008A,
      0x00000088,
      0x80008009,
      0x8000000A,
      0x8000808B,
      0x0000008B,
      0x00008089,
      0x00008003,
      0x00008002,
      0x00000080,
      0x0000800A,
      0x8000000A,
      0x80008081,
      0x00008080,};

  /**
   * Derived from {#link org.bouncycastle.crypto.digests.KeccakDigest#KeccakPermutation()}. Copyright (c) 2000-2017 The
   * Legion Of The Bouncy Castle Inc. (http://www.bouncycastle.org) The original source is licensed under a MIT license.
   */
  static void keccakf800(int[] state) {
    int a00 = state[0], a01 = state[1], a02 = state[2], a03 = state[3], a04 = state[4];
    int a05 = state[5], a06 = state[6], a07 = state[7], a08 = state[8], a09 = state[9];
    int a10 = state[10], a11 = state[11], a12 = state[12], a13 = state[13], a14 = state[14];
    int a15 = state[15], a16 = state[16], a17 = state[17], a18 = state[18], a19 = state[19];
    int a20 = state[20], a21 = state[21], a22 = state[22], a23 = state[23], a24 = state[24];

    for (int i = 0; i < 22; i++) {
      // theta
      int c0 = a00 ^ a05 ^ a10 ^ a15 ^ a20;
      int c1 = a01 ^ a06 ^ a11 ^ a16 ^ a21;
      int c2 = a02 ^ a07 ^ a12 ^ a17 ^ a22;
      int c3 = a03 ^ a08 ^ a13 ^ a18 ^ a23;
      int c4 = a04 ^ a09 ^ a14 ^ a19 ^ a24;

      int d1 = (c1 << 1 | c1 >>> 31) ^ c4;
      int d2 = (c2 << 1 | c2 >>> 31) ^ c0;
      int d3 = (c3 << 1 | c3 >>> 31) ^ c1;
      int d4 = (c4 << 1 | c4 >>> 31) ^ c2;
      int d0 = (c0 << 1 | c0 >>> 31) ^ c3;

      a00 ^= d1;
      a05 ^= d1;
      a10 ^= d1;
      a15 ^= d1;
      a20 ^= d1;
      a01 ^= d2;
      a06 ^= d2;
      a11 ^= d2;
      a16 ^= d2;
      a21 ^= d2;
      a02 ^= d3;
      a07 ^= d3;
      a12 ^= d3;
      a17 ^= d3;
      a22 ^= d3;
      a03 ^= d4;
      a08 ^= d4;
      a13 ^= d4;
      a18 ^= d4;
      a23 ^= d4;
      a04 ^= d0;
      a09 ^= d0;
      a14 ^= d0;
      a19 ^= d0;
      a24 ^= d0;

      // rho/pi
      c1 = a01 << 1 | a01 >>> 31;
      a01 = a06 << 12 | a06 >>> 20;
      a06 = a09 << 20 | a09 >>> 12;
      a09 = a22 << 29 | a22 >>> 3;
      a22 = a14 << 7 | a14 >>> 25;
      a14 = a20 << 18 | a20 >>> 14;
      a20 = a02 << 30 | a02 >>> 2;
      a02 = a12 << 11 | a12 >>> 21;
      a12 = a13 << 25 | a13 >>> 7;
      a13 = a19 << 8 | a19 >>> 24;
      a19 = a23 << 24 | a23 >>> 8;
      a23 = a15 << 9 | a15 >>> 23;
      a15 = a04 << 27 | a04 >>> 5;
      a04 = a24 << 14 | a24 >>> 18;
      a24 = a21 << 2 | a21 >>> 30;
      a21 = a08 << 23 | a08 >>> 9;
      a08 = a16 << 13 | a16 >>> 19;
      a16 = a05 << 4 | a05 >>> 28;
      a05 = a03 << 28 | a03 >>> 4;
      a03 = a18 << 21 | a18 >>> 11;
      a18 = a17 << 15 | a17 >>> 17;
      a17 = a11 << 10 | a11 >>> 22;
      a11 = a07 << 6 | a07 >>> 26;
      a07 = a10 << 3 | a10 >>> 29;
      a10 = c1;

      // chi
      c0 = a00 ^ (~a01 & a02);
      c1 = a01 ^ (~a02 & a03);
      a02 ^= ~a03 & a04;
      a03 ^= ~a04 & a00;
      a04 ^= ~a00 & a01;
      a00 = c0;
      a01 = c1;

      c0 = a05 ^ (~a06 & a07);
      c1 = a06 ^ (~a07 & a08);
      a07 ^= ~a08 & a09;
      a08 ^= ~a09 & a05;
      a09 ^= ~a05 & a06;
      a05 = c0;
      a06 = c1;

      c0 = a10 ^ (~a11 & a12);
      c1 = a11 ^ (~a12 & a13);
      a12 ^= ~a13 & a14;
      a13 ^= ~a14 & a10;
      a14 ^= ~a10 & a11;
      a10 = c0;
      a11 = c1;

      c0 = a15 ^ (~a16 & a17);
      c1 = a16 ^ (~a17 & a18);
      a17 ^= ~a18 & a19;
      a18 ^= ~a19 & a15;
      a19 ^= ~a15 & a16;
      a15 = c0;
      a16 = c1;

      c0 = a20 ^ (~a21 & a22);
      c1 = a21 ^ (~a22 & a23);
      a22 ^= ~a23 & a24;
      a23 ^= ~a24 & a20;
      a24 ^= ~a20 & a21;
      a20 = c0;
      a21 = c1;

      // iota
      a00 ^= keccakRoundConstants[i];
    }

    state[0] = a00;
    state[1] = a01;
    state[2] = a02;
    state[3] = a03;
    state[4] = a04;
    state[5] = a05;
    state[6] = a06;
    state[7] = a07;
    state[8] = a08;
    state[9] = a09;
    state[10] = a10;
    state[11] = a11;
    state[12] = a12;
    state[13] = a13;
    state[14] = a14;
    state[15] = a15;
    state[16] = a16;
    state[17] = a17;
    state[18] = a18;
    state[19] = a19;
    state[20] = a20;
    state[21] = a21;
    state[22] = a22;
    state[23] = a23;
    state[24] = a24;
  }

  static Bytes32 keccakF800Progpow(Bytes32 header, long seed, Bytes32 digest) {
    int[] state = new int[25];

    for (int i = 0; i < 8; i++) {
      state[i] = header.getInt(i * 4, ByteOrder.LITTLE_ENDIAN);
    }
    state[8] = (int) seed;
    state[9] = (int) (seed >> 32);
    for (int i = 0; i < 8; i++) {
      state[10 + i] = digest.getInt(i * 4);
    }

    keccakf800(state);

    ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);

    for (int i = 0; i < 8; i++) {
      buffer.putInt(i * 4, state[i]);
    }
    return Bytes32.wrap(buffer.array());
  }
}
