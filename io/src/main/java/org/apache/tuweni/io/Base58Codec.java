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
package org.apache.tuweni.io;

import java.util.Arrays;

class Base58Codec {

  // @formatter:off
    private static final byte[] ENCODE_TABLE = {
            '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S',
            'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'i', 'j', 'k', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'
    };

    private static final byte[] DECODE_TABLE = {
        //   0   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 00-0f
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 10-1f
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 20-2f + - /
            -1,  0,  1,  2,  3,  4,  5,  6,  7,  8, -1, -1, -1, -1, -1, -1, // 30-3f 0-9
            -1,  9, 10, 11, 12, 13, 14, 15, 16, -1, 17, 18, 19, 20, 21, -1, // 40-4f A-O
            22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, -1, -1, -1, -1, -1, // 50-5f P-Z _
            -1, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, -1, 44, 45, 46, // 60-6f a-o
            47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57                      // 70-7a p-z
    };
    // @formatter:on

  static byte[] encode(byte[] decoded) {
    byte[] input = Arrays.copyOf(decoded, decoded.length);
    byte[] encoded = new byte[input.length * 2];
    int inputStart = 0;
    int outputStart = encoded.length;
    int zeros = 0;

    while (inputStart < input.length) {
      if (input[inputStart] == 0 && outputStart == encoded.length) {
        zeros++;
        inputStart++;
        continue;
      }
      int remainder = 0;
      for (int i = 0; i < input.length; i++) {
        int digit = (int) input[i] & 0xFF;
        int temp = remainder * 256 + digit;
        input[i] = (byte) (temp / 58);
        remainder = temp % 58;
      }
      encoded[--outputStart] = ENCODE_TABLE[remainder];
      if (input[inputStart] == 0) {
        inputStart++;
      }
    }
    Arrays.fill(encoded, outputStart - zeros, outputStart, ENCODE_TABLE[0]);
    return Arrays.copyOfRange(encoded, outputStart - zeros, encoded.length);
  }

  static byte[] decode(String encoded) {
    byte[] input = new byte[encoded.length()];
    byte[] decoded = new byte[input.length];
    for (int i = 0; i < input.length; i++) {
      input[i] = DECODE_TABLE[encoded.charAt(i)];
      if (input[i] == -1) {
        throw new IllegalArgumentException("Invalid character " + encoded.charAt(i));
      }
    }
    int inputStart = 0;
    int outputStart = input.length;
    int zeros = 0;

    while (inputStart < input.length) {
      if (input[inputStart] == 0 && outputStart == input.length) {
        zeros++;
        inputStart++;
        continue;
      }
      int remainder = 0;
      for (int i = 0; i < input.length; i++) {
        int digit = (int) input[i] & 0xFF;
        int temp = remainder * 58 + digit;
        input[i] = (byte) (temp / 256);
        remainder = temp % 256;
      }
      decoded[--outputStart] = (byte) remainder;
      if (input[inputStart] == 0) {
        inputStart++;
      }
    }
    Arrays.fill(decoded, outputStart - zeros, outputStart, (byte) 0);
    return Arrays.copyOfRange(decoded, outputStart - zeros, decoded.length);
  }
}
