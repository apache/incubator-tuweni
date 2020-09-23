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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.tuweni.bytes.Bytes;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class Base58Test {

  @Test
  void testHelloWorld() {
    String result = Base58.encode(Bytes.wrap("Hello World!".getBytes(StandardCharsets.US_ASCII)));
    assertEquals("2NEpo7TZRRrLZSi2U", result);
  }

  @Test
  void testQuickBrownFox() {
    String result =
        Base58.encode(Bytes.wrap("The quick brown fox jumps over the lazy dog.".getBytes(StandardCharsets.US_ASCII)));
    assertEquals("USm3fpXnKG5EUBx2ndxBDMPVciP5hGey2Jh4NDv6gmeo1LkMeiKrLJUUBk6Z", result);
  }

  @Test
  void testHex() {
    Bytes value = Bytes.fromHexString("1220BA8632EF1A07986B171B3C8FAF0F79B3EE01B6C30BBE15A13261AD6CB0D02E3A");
    assertEquals("QmatmE9msSfkKxoffpHwNLNKgwZG8eT9Bud6YoPab52vpy", Base58.encode(value));
  }

  @Test
  void testHexDecode() {
    Bytes value = Bytes.fromHexString("00000000");
    assertEquals(value, Bytes.wrap(Base58.decode(Base58.encode(value))));
  }

  @Test
  void testHexDecodeOne() {
    Bytes value = Bytes.fromHexString("01");
    assertEquals(value, Bytes.wrap(Base58.decode(Base58.encode(value))));
  }

  @Test
  void testHexDecode256() {
    Bytes value = Bytes.fromHexString("0100");
    assertEquals(value, Bytes.wrap(Base58.decode(Base58.encode(value))));
  }

  @Test
  void testZeros() {
    Bytes value = Bytes.fromHexString("000000");
    assertEquals("111", Base58.encode(value));
  }

  @Test
  void testZerosThenOne() {
    Bytes value = Bytes.fromHexString("00000001");
    assertEquals("1112", Base58.encode(value));
  }

  @Test
  void testBadCharacter() {
    assertThrows(IllegalArgumentException.class, () -> {
      Base58.decode("%^");
    });
  }
}
