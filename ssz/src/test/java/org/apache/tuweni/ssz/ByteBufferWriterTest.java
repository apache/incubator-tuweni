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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.tuweni.bytes.Bytes.fromHexString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ByteBufferWriterTest {

  @ParameterizedTest
  @CsvSource({"E8030000, 1000", "A0860100, 100000"})
  void shouldWriteSmallIntegers(String expectedHex, int value) {
    ByteBuffer buffer = ByteBuffer.allocate(64);
    SSZ.encodeTo(buffer, writer -> writer.writeInt(value, 32));
    buffer.flip();
    assertEquals(fromHexString(expectedHex), Bytes.wrapByteBuffer(buffer));
  }

  @Test
  void shouldWriteLongIntegers() {
    ByteBuffer buffer = ByteBuffer.allocate(64);
    SSZ.encodeTo(buffer, writer -> writer.writeLong(100000L, 24));
    buffer.flip();
    assertEquals(fromHexString("A08601"), Bytes.wrapByteBuffer(buffer));
  }

  @Test
  void shouldWriteUInt256Integers() {
    ByteBuffer buffer = ByteBuffer.allocate(64);
    SSZ.encodeTo(buffer, writer -> writer.writeUInt256(UInt256.valueOf(100000L)));
    buffer.flip();
    assertEquals(
        fromHexString("A086010000000000000000000000000000000000000000000000000000000000"),
        Bytes.wrapByteBuffer(buffer));

    buffer.clear();
    SSZ
        .encodeTo(
            buffer,
            writer -> writer
                .writeUInt256(
                    UInt256.fromHexString("0x0400000000000000000000000000000000000000000000000000f100000000ab")));
    buffer.flip();
    assertEquals(
        fromHexString("AB00000000F10000000000000000000000000000000000000000000000000004"),
        Bytes.wrapByteBuffer(buffer));
  }

  @Test
  void shouldWriteBigIntegers() {
    ByteBuffer buffer = ByteBuffer.allocate(64);
    SSZ.encodeTo(buffer, writer -> writer.writeBigInteger(BigInteger.valueOf(100000), 24));
    buffer.flip();
    assertEquals(fromHexString("A08601"), Bytes.wrapByteBuffer(buffer));

    buffer.clear();
    SSZ.encodeTo(buffer, writer -> writer.writeBigInteger(BigInteger.valueOf(127).pow(16), 112));
    buffer.flip();
    assertEquals(fromHexString("01F81D7AF1971CEDD9BBA5EFCEE1"), Bytes.wrapByteBuffer(buffer));
  }

  @Test
  void shouldWriteEmptyStrings() {
    ByteBuffer buffer = ByteBuffer.allocate(64);
    SSZ.encodeTo(buffer, writer -> writer.writeString(""));
    buffer.flip();
    assertEquals(fromHexString("00000000"), Bytes.wrapByteBuffer(buffer));
  }

  @Test
  void shouldWriteOneCharactersStrings() {
    ByteBuffer buffer = ByteBuffer.allocate(64);
    SSZ.encodeTo(buffer, writer -> writer.writeString("d"));
    buffer.flip();
    assertEquals(fromHexString("0100000064"), Bytes.wrapByteBuffer(buffer));
  }

  @Test
  void shouldWriteStrings() {
    ByteBuffer buffer = ByteBuffer.allocate(64);
    SSZ.encodeTo(buffer, writer -> writer.writeString("dog"));
    buffer.flip();
    assertEquals(fromHexString("03000000646F67"), Bytes.wrapByteBuffer(buffer));
  }

  @Test
  void shouldWriteShortLists() {
    String[] strings =
        new String[] {"asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer"};

    ByteBuffer buffer = ByteBuffer.allocate(256);
    SSZ.encodeTo(buffer, w -> w.writeStringList(strings));
    buffer.flip();

    assertEquals(
        fromHexString(
            "5800000004000000617364660400000071776572040000007A78637604000000617364660400000071776572040000007A78637604000000617364660400000071776572040000007A78637604000000617364660400000071776572"),
        Bytes.wrapByteBuffer(buffer));
  }

  @Test
  void shouldWritePreviouslyEncodedValues() {
    ByteBuffer buffer = ByteBuffer.allocate(64);
    SSZ.encodeTo(buffer, writer -> writer.writeSSZ(SSZ.encodeByteArray("abc".getBytes(UTF_8))));
    buffer.flip();
    assertEquals("abc", SSZ.decodeString(Bytes.wrapByteBuffer(buffer)));
  }
}
