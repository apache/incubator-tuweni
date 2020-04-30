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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Charsets;
import org.junit.jupiter.api.Test;

class BytesSSZWriterTest {

  private static class SomeObject {
    private final String name;
    private final int number;
    private final BigInteger longNumber;

    SomeObject(String name, int number, BigInteger longNumber) {
      this.name = name;
      this.number = number;
      this.longNumber = longNumber;
    }
  }

  @Test
  void shouldWriteFullObjects() {
    SomeObject bob = new SomeObject("Bob", 4, BigInteger.valueOf(1234563434344L));

    Bytes bytes = SSZ.encode(writer -> {
      writer.writeString(bob.name);
      writer.writeInt(bob.number, 8);
      writer.writeBigInteger(bob.longNumber, 256);
    });

    assertTrue(SSZ.<Boolean>decode(bytes, reader -> {
      assertEquals("Bob", reader.readString());
      assertEquals(4, reader.readInt(8));
      assertEquals(BigInteger.valueOf(1234563434344L), reader.readBigInteger(256));
      return true;
    }));
  }

  @Test
  void shouldWriteEmptyStrings() {
    assertEquals(fromHexString("00000000"), SSZ.encode(writer -> writer.writeString("")));
  }

  @Test
  void shouldWriteOneCharactersStrings() {
    assertEquals(fromHexString("0100000064"), SSZ.encode(writer -> writer.writeString("d")));
  }

  @Test
  void shouldWriteStrings() {
    assertEquals(fromHexString("03000000646f67"), SSZ.encode(writer -> writer.writeString("dog")));
  }

  @Test
  void shouldWriteSignedIntegers() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeInt(0, 8)));
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeInt8(0)));

    assertEquals(fromHexString("0000"), SSZ.encode(writer -> writer.writeInt(0, 16)));
    assertEquals(fromHexString("0000"), SSZ.encode(writer -> writer.writeInt16(0)));

    assertEquals(fromHexString("000000"), SSZ.encode(writer -> writer.writeInt(0, 24)));

    assertEquals(fromHexString("00000000"), SSZ.encode(writer -> writer.writeInt(0, 32)));
    assertEquals(fromHexString("00000000"), SSZ.encode(writer -> writer.writeInt32(0)));

    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeInt(1, 8)));
    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeInt8(1)));

    assertEquals(fromHexString("0100"), SSZ.encode(writer -> writer.writeInt(1, 16)));
    assertEquals(fromHexString("0100"), SSZ.encode(writer -> writer.writeInt16(1)));

    assertEquals(fromHexString("01000000"), SSZ.encode(writer -> writer.writeInt(1, 32)));
    assertEquals(fromHexString("01000000"), SSZ.encode(writer -> writer.writeInt32(1)));

    assertEquals(fromHexString("0f"), SSZ.encode(writer -> writer.writeInt8(15)));
    assertEquals(fromHexString("0f00"), SSZ.encode(writer -> writer.writeInt16(15)));
    assertEquals(fromHexString("E803"), SSZ.encode(writer -> writer.writeInt16(1000)));
    assertEquals(fromHexString("0004"), SSZ.encode(writer -> writer.writeInt16(1024)));
    assertEquals(fromHexString("A08601"), SSZ.encode(writer -> writer.writeInt(100000, 24)));

    assertEquals(fromHexString("FF"), SSZ.encode(writer -> writer.writeInt(-1, 8)));
    assertEquals(fromHexString("FFFF"), SSZ.encode(writer -> writer.writeInt(-1, 16)));
    assertEquals(fromHexString("80"), SSZ.encode(writer -> writer.writeInt(-128, 8)));
    assertEquals(fromHexString("80"), SSZ.encode(writer -> writer.writeInt8(-128)));
    assertEquals(fromHexString("0080"), SSZ.encode(writer -> writer.writeInt(-32768, 16)));
    assertEquals(fromHexString("0080"), SSZ.encode(writer -> writer.writeInt16(-32768)));
  }

  @Test
  void shouldWriteSignedLongs() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeLong(0, 8)));
    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeLong(1, 8)));
    assertEquals(fromHexString("0f"), SSZ.encode(writer -> writer.writeLong(15, 8)));

    assertEquals(fromHexString("e803"), SSZ.encode(writer -> writer.writeLong(1000, 16)));
    assertEquals(fromHexString("0004"), SSZ.encode(writer -> writer.writeLong(1024, 16)));
    assertEquals(fromHexString("A08601"), SSZ.encode(writer -> writer.writeLong(100000L, 24)));
    assertEquals(fromHexString("A0860100"), SSZ.encode(writer -> writer.writeLong(100000L, 32)));
    assertEquals(fromHexString("A086010000000000"), SSZ.encode(writer -> writer.writeLong(100000L, 64)));
    assertEquals(fromHexString("A086010000000000"), SSZ.encode(writer -> writer.writeInt64(100000L)));

    assertEquals(fromHexString("FF"), SSZ.encode(writer -> writer.writeLong(-1, 8)));
    assertEquals(fromHexString("FFFF"), SSZ.encode(writer -> writer.writeLong(-1, 16)));
    assertEquals(fromHexString("80"), SSZ.encode(writer -> writer.writeLong(-128, 8)));
    assertEquals(fromHexString("0080"), SSZ.encode(writer -> writer.writeLong(-32768, 16)));
    assertEquals(fromHexString("0000000000000080"), SSZ.encode(writer -> writer.writeInt64(-9223372036854775808L)));
  }

  @Test
  void shouldWriteSignedBigIntegers() {
    assertEquals(fromHexString("A08601"), SSZ.encode(writer -> writer.writeBigInteger(BigInteger.valueOf(100000), 24)));
    assertEquals(fromHexString("16EB"), SSZ.encode(writer -> writer.writeBigInteger(BigInteger.valueOf(-5354), 16)));
    assertEquals(fromHexString("0080"), SSZ.encode(writer -> writer.writeBigInteger(BigInteger.valueOf(-32768), 16)));
    assertEquals(
        fromHexString("01F81D7AF1971CEDD9BBA5EFCEE1"),
        SSZ.encode(writer -> writer.writeBigInteger(BigInteger.valueOf(127).pow(16), 112)));
  }

  @Test
  void shouldWriteUnsignedIntegers() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeUInt(0, 8)));
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeUInt8(0)));

    assertEquals(fromHexString("0000"), SSZ.encode(writer -> writer.writeUInt(0, 16)));
    assertEquals(fromHexString("0000"), SSZ.encode(writer -> writer.writeUInt16(0)));

    assertEquals(fromHexString("000000"), SSZ.encode(writer -> writer.writeUInt(0, 24)));

    assertEquals(fromHexString("00000000"), SSZ.encode(writer -> writer.writeUInt(0, 32)));
    assertEquals(fromHexString("00000000"), SSZ.encode(writer -> writer.writeUInt32(0)));

    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeUInt(1, 8)));
    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeUInt8(1)));

    assertEquals(fromHexString("0100"), SSZ.encode(writer -> writer.writeUInt(1, 16)));
    assertEquals(fromHexString("0100"), SSZ.encode(writer -> writer.writeUInt16(1)));

    assertEquals(fromHexString("01000000"), SSZ.encode(writer -> writer.writeUInt(1, 32)));
    assertEquals(fromHexString("01000000"), SSZ.encode(writer -> writer.writeUInt32(1)));

    assertEquals(fromHexString("0f"), SSZ.encode(writer -> writer.writeUInt8(15)));
    assertEquals(fromHexString("0f00"), SSZ.encode(writer -> writer.writeUInt16(15)));
    assertEquals(fromHexString("e803"), SSZ.encode(writer -> writer.writeUInt16(1000)));
    assertEquals(fromHexString("0004"), SSZ.encode(writer -> writer.writeUInt16(1024)));
    assertEquals(fromHexString("A08601"), SSZ.encode(writer -> writer.writeUInt(100000, 24)));

    assertEquals(fromHexString("FF"), SSZ.encode(writer -> writer.writeUInt(255, 8)));
    assertEquals(fromHexString("FFFF"), SSZ.encode(writer -> writer.writeUInt(65535, 16)));
    assertEquals(fromHexString("80"), SSZ.encode(writer -> writer.writeUInt(128, 8)));
    assertEquals(fromHexString("80"), SSZ.encode(writer -> writer.writeUInt8(128)));
    assertEquals(fromHexString("0080"), SSZ.encode(writer -> writer.writeUInt(32768, 16)));
    assertEquals(fromHexString("0080"), SSZ.encode(writer -> writer.writeUInt16(32768)));
  }

  @Test
  void shouldWriteUnsignedLongs() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeULong(0, 8)));
    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeULong(1, 8)));
    assertEquals(fromHexString("0f"), SSZ.encode(writer -> writer.writeULong(15, 8)));

    assertEquals(fromHexString("e803"), SSZ.encode(writer -> writer.writeULong(1000, 16)));
    assertEquals(fromHexString("0004"), SSZ.encode(writer -> writer.writeULong(1024, 16)));
    assertEquals(fromHexString("A08601"), SSZ.encode(writer -> writer.writeULong(100000L, 24)));
    assertEquals(fromHexString("A0860100"), SSZ.encode(writer -> writer.writeULong(100000L, 32)));
    assertEquals(fromHexString("A086010000000000"), SSZ.encode(writer -> writer.writeULong(100000L, 64)));
    assertEquals(fromHexString("A086010000000000"), SSZ.encode(writer -> writer.writeUInt64(100000L)));

    assertEquals(fromHexString("FF"), SSZ.encode(writer -> writer.writeULong(255, 8)));
    assertEquals(fromHexString("FFFF"), SSZ.encode(writer -> writer.writeULong(65535, 16)));
    assertEquals(fromHexString("80"), SSZ.encode(writer -> writer.writeULong(128, 8)));
    assertEquals(fromHexString("0080"), SSZ.encode(writer -> writer.writeULong(32768, 16)));
    assertEquals(fromHexString("0000000000000080"), SSZ.encode(writer -> {
      writer.writeUInt64(Long.parseUnsignedLong("9223372036854775808"));
    }));
  }

  @Test
  void shouldWriteUInt256Integers() {
    assertEquals(
        fromHexString("0000000000000000000000000000000000000000000000000000000000000000"),
        SSZ.encode(writer -> writer.writeUInt256(UInt256.valueOf(0L))));
    assertEquals(
        fromHexString("A086010000000000000000000000000000000000000000000000000000000000"),
        SSZ.encode(writer -> writer.writeUInt256(UInt256.valueOf(100000L))));
    assertEquals(
        fromHexString("AB00000000F10000000000000000000000000000000000000000000000000004"),
        SSZ
            .encode(
                writer -> writer
                    .writeUInt256(
                        UInt256.fromHexString("0x0400000000000000000000000000000000000000000000000000f100000000ab"))));
  }

  @Test
  void shouldWriteBooleans() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeBoolean(false)));
    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeBoolean(true)));
  }

  @Test
  void shouldWriteAddresses() {
    assertEquals(
        fromHexString("8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C97"),
        SSZ.encode(writer -> writer.writeAddress(Bytes.fromHexString("8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C97"))));
    assertThrows(
        IllegalArgumentException.class,
        () -> SSZ.encode(writer -> writer.writeAddress(Bytes.fromHexString("beef"))));
  }

  @Test
  void shouldWriteHashes() {
    assertEquals(
        fromHexString("ED1C978EE1CEEFA5BBD9ED1C8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C97"),
        SSZ
            .encode(
                writer -> writer
                    .writeHash(
                        Bytes32.fromHexString("ED1C978EE1CEEFA5BBD9ED1C8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C97"))));
    assertThrows(
        IllegalArgumentException.class,
        () -> SSZ.encode(writer -> writer.writeAddress(Bytes.fromHexString("beef"))));
  }

  @Test
  void shouldWriteVarargsListsOfInts() {
    assertEquals(fromHexString("03000000030405"), SSZ.encodeIntList(8, 3, 4, 5));
  }

  @Test
  void shouldWriteUtilListsOfInts() {
    assertEquals(fromHexString("03000000030405"), SSZ.encodeIntList(8, Arrays.asList(3, 4, 5)));
  }

  @Test
  void shouldWriteVarargsListsOfLongInts() {
    assertEquals(fromHexString("03000000030405"), SSZ.encodeLongIntList(8, 3, 4, 5));
  }

  @Test
  void shouldWriteUtilListsOfLongInts() {
    assertEquals(
        fromHexString("03000000030405"),
        SSZ.encodeLongIntList(8, Arrays.asList((long) 3, (long) 4, (long) 5)));
  }

  @Test
  void shouldWriteVarargsListsOfBigIntegers() {
    assertEquals(
        fromHexString("03000000030405"),
        SSZ.encodeBigIntegerList(8, BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(5)));
  }

  @Test
  void shouldWriteUtilListsOfBigIntegers() {
    assertEquals(
        fromHexString("03000000030405"),
        SSZ
            .encodeBigIntegerList(
                8,
                Arrays.asList(BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(5))));
  }

  @Test
  void shouldWriteVarargsListsOfUnsignedInts() {
    assertEquals(fromHexString("03000000FDFEFF"), SSZ.encodeUIntList(8, 253, 254, 255));
  }

  @Test
  void shouldWriteUtilListsOfUnsignedInts() {
    assertEquals(fromHexString("03000000FDFEFF"), SSZ.encodeUIntList(8, Arrays.asList(253, 254, 255)));
  }

  @Test
  void shouldWriteVarargsListsOfUnsignedLongs() {
    assertEquals(fromHexString("03000000FDFEFF"), SSZ.encodeULongIntList(8, 253, 254, 255));
  }

  @Test
  void shouldWriteUtilListsOfUnsignedLongs() {
    assertEquals(
        fromHexString("03000000FDFEFF"),
        SSZ.encodeULongIntList(8, Arrays.asList((long) 253, (long) 254, (long) 255)));
  }

  @Test
  void shouldWriteVarargsListsOfUInt256() {
    assertEquals(
        fromHexString(
            "0x60000000030000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000500000000000000000000000000000000000000000000000000000000000000"),
        SSZ.encodeUInt256List(UInt256.valueOf(3L), UInt256.valueOf(4L), UInt256.valueOf(5L)));
  }

  @Test
  void shouldWriteUtilListsOfUInt256() {
    assertEquals(
        fromHexString(
            "0x60000000030000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000500000000000000000000000000000000000000000000000000000000000000"),
        SSZ.encodeUInt256List(Arrays.asList(UInt256.valueOf(3L), UInt256.valueOf(4L), UInt256.valueOf(5L))));
  }

  @Test
  void shouldWriteVaragsListsOfStrings() {
    assertEquals(
        fromHexString("1800000003000000626F62040000006A616E65050000006A616E6574"),
        SSZ.encodeStringList("bob", "jane", "janet"));
  }

  @Test
  void shouldWriteUtilListsOfStrings() {
    assertEquals(
        fromHexString("1800000003000000626F62040000006A616E65050000006A616E6574"),
        SSZ.encodeStringList(Arrays.asList("bob", "jane", "janet")));
  }

  @Test
  void shouldWriteVarargsListsOfBytes() {
    assertEquals(
        fromHexString("1800000003000000626F62040000006A616E65050000006A616E6574"),
        SSZ
            .encodeBytesList(
                Bytes.wrap("bob".getBytes(Charsets.UTF_8)),
                Bytes.wrap("jane".getBytes(Charsets.UTF_8)),
                Bytes.wrap("janet".getBytes(Charsets.UTF_8))));
  }

  @Test
  void shouldWriteUtilListOfBytes() {
    assertEquals(
        fromHexString("1800000003000000626F62040000006A616E65050000006A616E6574"),
        SSZ
            .encodeBytesList(
                Arrays
                    .asList(
                        Bytes.wrap("bob".getBytes(Charsets.UTF_8)),
                        Bytes.wrap("jane".getBytes(Charsets.UTF_8)),
                        Bytes.wrap("janet".getBytes(Charsets.UTF_8)))));
  }

  @Test
  void shouldWriteUtilListOfExtendedBytesType() {
    assertEquals(
        fromHexString(
            "0x6C000000200000000000000000000000000000000000000000000000000000000000000000626F6220000000000000000000000000000000000000000000000000000000000000006A616E65200000000000000000000000000000000000000000000000000000000000006A616E6574"),
        SSZ
            .encodeBytesList(
                Arrays
                    .asList(
                        Bytes32.leftPad(Bytes.wrap("bob".getBytes(Charsets.UTF_8))),
                        Bytes32.leftPad(Bytes.wrap("jane".getBytes(Charsets.UTF_8))),
                        Bytes32.leftPad(Bytes.wrap("janet".getBytes(Charsets.UTF_8))))));
  }

  @Test
  void shouldWriteVarargsListsOfHashes() {
    assertEquals(
        fromHexString(
            "0x60000000ED1C978EE1CEEFA5BBD9ED1C8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C978B40CA3893681B062BC06760A4863AFAFA6C4D6226D4C6AFAFA3684A06760CB26B30567B2281FF3BD582B0A633B33A376B95BD3333DB59B673A33B336A0B285D"),
        SSZ
            .encodeHashList(
                fromHexString("0xED1C978EE1CEEFA5BBD9ED1C8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C97"),
                fromHexString("0x8B40CA3893681B062BC06760A4863AFAFA6C4D6226D4C6AFAFA3684A06760CB2"),
                fromHexString("0x6B30567B2281FF3BD582B0A633B33A376B95BD3333DB59B673A33B336A0B285D")));
  }

  @Test
  void shouldWriteUtilListsOfHashes() {
    assertEquals(
        fromHexString(
            "0x60000000ED1C978EE1CEEFA5BBD9ED1C8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C978B40CA3893681B062BC06760A4863AFAFA6C4D6226D4C6AFAFA3684A06760CB26B30567B2281FF3BD582B0A633B33A376B95BD3333DB59B673A33B336A0B285D"),
        SSZ
            .encodeHashList(
                Arrays
                    .asList(
                        fromHexString("0xED1C978EE1CEEFA5BBD9ED1C8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C97"),
                        fromHexString("0x8B40CA3893681B062BC06760A4863AFAFA6C4D6226D4C6AFAFA3684A06760CB2"),
                        fromHexString("0x6B30567B2281FF3BD582B0A633B33A376B95BD3333DB59B673A33B336A0B285D"))));
  }

  @Test
  void shouldWriteVaragsListsOfAddresses() {
    assertEquals(
        fromHexString(
            "0x3C0000008EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C9779C1DE9DBB5AFEEC1EE8BBD9ED1C978EE1CEEFA5BBD9ED1C978EE1CEEFA5"),
        SSZ
            .encodeAddressList(
                fromHexString("0x8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C97"),
                fromHexString("0x8EE1CEEFA5BBD9ED1C9779C1DE9DBB5AFEEC1EE8"),
                fromHexString("0xBBD9ED1C978EE1CEEFA5BBD9ED1C978EE1CEEFA5")));
  }

  @Test
  void shouldWriteUtilListsOfAddresses() {
    assertEquals(
        fromHexString(
            "0x3C0000008EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C9779C1DE9DBB5AFEEC1EE8BBD9ED1C978EE1CEEFA5BBD9ED1C978EE1CEEFA5"),
        SSZ
            .encodeAddressList(
                Arrays
                    .asList(
                        fromHexString("0x8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C97"),
                        fromHexString("0x8EE1CEEFA5BBD9ED1C9779C1DE9DBB5AFEEC1EE8"),
                        fromHexString("0xBBD9ED1C978EE1CEEFA5BBD9ED1C978EE1CEEFA5"))));
  }

  @Test
  void shouldWriteVaragsListsOfBooleans() {
    assertEquals(fromHexString("0400000000010100"), SSZ.encodeBooleanList(false, true, true, false));
  }

  @Test
  void shouldWriteUtilListsOfBooleans() {
    assertEquals(fromHexString("0400000000010100"), SSZ.encodeBooleanList(Arrays.asList(false, true, true, false)));
  }

  @Test
  void shouldWriteVectorOfHomogeneousBytes() {
    // Per the pre-SOS SSZ spec, neither the vector nor the bytes should have a mixin.
    List<Bytes32> elements = Arrays
        .asList(
            Bytes32.fromHexString("0x01"),
            Bytes32.fromHexString("0x02"),
            Bytes32.fromHexString("0x03"),
            Bytes32.fromHexString("0x04"),
            Bytes32.fromHexString("0x05"));
    assertEquals(
        fromHexString(
            "0x0000000000000000000000000000000000000000000000000000000000000001"
                + "0000000000000000000000000000000000000000000000000000000000000002"
                + "0000000000000000000000000000000000000000000000000000000000000003"
                + "0000000000000000000000000000000000000000000000000000000000000004"
                + "0000000000000000000000000000000000000000000000000000000000000005"),
        SSZ.encode(writer -> writer.writeFixedBytesVector(elements)));
  }

  @Test
  void shouldWriteVectorOfNonHomogeneousBytes() {
    // Per the pre-SOS SSZ spec, the vector itself should not have a mixin, but the individual bytes elements should.
    List<Bytes32> elements = Arrays
        .asList(
            Bytes32.fromHexString("0x01"),
            Bytes32.fromHexString("0x02"),
            Bytes32.fromHexString("0x03"),
            Bytes32.fromHexString("0x04"),
            Bytes32.fromHexString("0x05"));
    assertEquals(
        fromHexString(
            "0x200000000000000000000000000000000000000000000000000000000000000000000001"
                + "200000000000000000000000000000000000000000000000000000000000000000000002"
                + "200000000000000000000000000000000000000000000000000000000000000000000003"
                + "200000000000000000000000000000000000000000000000000000000000000000000004"
                + "200000000000000000000000000000000000000000000000000000000000000000000005"),
        SSZ.encode(writer -> writer.writeVector(elements)));
  }

  @Test
  void shouldWriteListOfHomogeneousBytes() {
    // Per the pre-SOS SSZ spec, the list iself should have a mixin, but the bytes elements should not.
    List<Bytes32> elements = Arrays
        .asList(
            Bytes32.fromHexString("0x01"),
            Bytes32.fromHexString("0x02"),
            Bytes32.fromHexString("0x03"),
            Bytes32.fromHexString("0x04"),
            Bytes32.fromHexString("0x05"));
    assertEquals(
        fromHexString(
            "0xA0000000"
                + "0000000000000000000000000000000000000000000000000000000000000001"
                + "0000000000000000000000000000000000000000000000000000000000000002"
                + "0000000000000000000000000000000000000000000000000000000000000003"
                + "0000000000000000000000000000000000000000000000000000000000000004"
                + "0000000000000000000000000000000000000000000000000000000000000005"),
        SSZ.encode(writer -> writer.writeFixedBytesList(elements)));
  }

  @Test
  void shouldWriteListOfNonHomogeneousBytes() {
    // Per the pre-SOS SSZ spec, both the vector itself and the individual bytes elements should have a length mixin.
    List<Bytes32> elements = Arrays
        .asList(
            Bytes32.fromHexString("0x01"),
            Bytes32.fromHexString("0x02"),
            Bytes32.fromHexString("0x03"),
            Bytes32.fromHexString("0x04"),
            Bytes32.fromHexString("0x05"));
    assertEquals(
        fromHexString(
            "0xB4000000"
                + "200000000000000000000000000000000000000000000000000000000000000000000001"
                + "200000000000000000000000000000000000000000000000000000000000000000000002"
                + "200000000000000000000000000000000000000000000000000000000000000000000003"
                + "200000000000000000000000000000000000000000000000000000000000000000000004"
                + "200000000000000000000000000000000000000000000000000000000000000000000005"),
        SSZ.encode(writer -> writer.writeBytesList(elements)));
  }

  @Test
  void shouldWritePreviouslyEncodedValues() {
    Bytes output = SSZ.encode(writer -> writer.writeSSZ(SSZ.encodeByteArray("abc".getBytes(UTF_8))));
    assertEquals("abc", SSZ.decodeString(output));
  }
}
