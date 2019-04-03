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
package org.apache.tuweni.trie;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;

import org.junit.jupiter.api.Test;

class CompactEncodingTest {

  @Test
  void bytesToPath() {
    Bytes path = CompactEncoding.bytesToPath(Bytes.of(0xab, 0xcd, 0xff));
    assertEquals(Bytes.of(0xa, 0xb, 0xc, 0xd, 0xf, 0xf, 0x10), path);
  }

  @Test
  void encodePath() {
    assertEquals(Bytes.of(0x11, 0x23, 0x45), CompactEncoding.encode(Bytes.of(0x01, 0x02, 0x03, 0x04, 0x05)));
    assertEquals(
        Bytes.of(0x00, 0x01, 0x23, 0x45),
        CompactEncoding.encode(Bytes.of(0x00, 0x01, 0x02, 0x03, 0x04, 0x05)));
    assertEquals(
        Bytes.of(0x20, 0x0f, 0x1c, 0xb8),
        CompactEncoding.encode(Bytes.of(0x00, 0x0f, 0x01, 0x0c, 0x0b, 0x08, 0x10)));
    assertEquals(Bytes.of(0x3f, 0x1c, 0xb8), CompactEncoding.encode(Bytes.of(0x0f, 0x01, 0x0c, 0x0b, 0x08, 0x10)));
  }

  @Test
  void decode() {
    assertEquals(Bytes.of(0x01, 0x02, 0x03, 0x04, 0x05), CompactEncoding.decode(Bytes.of(0x11, 0x23, 0x45)));
    assertEquals(
        Bytes.of(0x00, 0x01, 0x02, 0x03, 0x04, 0x05),
        CompactEncoding.decode(Bytes.of(0x00, 0x01, 0x23, 0x45)));
    assertEquals(
        Bytes.of(0x00, 0x0f, 0x01, 0x0c, 0x0b, 0x08, 0x10),
        CompactEncoding.decode(Bytes.of(0x20, 0x0f, 0x1c, 0xb8)));
    assertEquals(Bytes.of(0x0f, 0x01, 0x0c, 0x0b, 0x08, 0x10), CompactEncoding.decode(Bytes.of(0x3f, 0x1c, 0xb8)));
  }
}
