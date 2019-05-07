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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;

import org.junit.jupiter.api.Test;

class Base32Test {

  @Test
  void shouldEncodeByteArray() {
    String s = Base32.encodeBytes(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals("AEBAGBAFAYDQQ===", s);
  }

  @Test
  void shouldEncodeBytesValue() {
    String s = Base32.encode(Bytes.of(1, 2, 3, 4, 5, 6, 7, 8));
    assertEquals("AEBAGBAFAYDQQ===", s);
  }

  @Test
  void shouldDecodeToByteArray() {
    byte[] bytes = Base32.decodeBytes("AEBAGBAFAYDQQ===");
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, bytes);
  }

  @Test
  void shouldDecodeToBytesValue() {
    Bytes bytes = Base32.decode("AEBAGBAFAYDQQ===");
    assertEquals(Bytes.of(1, 2, 3, 4, 5, 6, 7, 8), bytes);
  }
}
