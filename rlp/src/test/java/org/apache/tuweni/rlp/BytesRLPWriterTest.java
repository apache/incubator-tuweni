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
package org.apache.tuweni.rlp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.tuweni.bytes.Bytes.fromHexString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class BytesRLPWriterTest {

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
    Bytes bytes = RLP.encode(writer -> {
      writer.writeString(bob.name);
      writer.writeInt(bob.number);
      writer.writeBigInteger(bob.longNumber);
    });

    assertTrue(RLP.<Boolean>decode(bytes, reader -> {
      assertEquals("Bob", reader.readString());
      assertEquals(4, reader.readInt());
      assertEquals(BigInteger.valueOf(1234563434344L), reader.readBigInteger());
      return true;
    }));
  }

  @Test
  void shouldWriteSmallIntegers() {
    assertEquals(fromHexString("80"), RLP.encode(writer -> writer.writeInt(0)));
    assertEquals(fromHexString("01"), RLP.encode(writer -> writer.writeInt(1)));
    assertEquals(fromHexString("0f"), RLP.encode(writer -> writer.writeInt(15)));
    assertEquals(fromHexString("8203e8"), RLP.encode(writer -> writer.writeInt(1000)));
    assertEquals(fromHexString("820400"), RLP.encode(writer -> writer.writeInt(1024)));
    assertEquals(fromHexString("830186a0"), RLP.encode(writer -> writer.writeInt(100000)));
  }

  @Test
  void shouldWriteLongIntegers() {
    assertEquals(fromHexString("80"), RLP.encode(writer -> writer.writeLong(0L)));
    assertEquals(fromHexString("01"), RLP.encode(writer -> writer.writeLong(1)));
    assertEquals(fromHexString("0f"), RLP.encode(writer -> writer.writeLong(15)));
    assertEquals(fromHexString("8203e8"), RLP.encode(writer -> writer.writeLong(1000)));
    assertEquals(fromHexString("820400"), RLP.encode(writer -> writer.writeLong(1024)));
    assertEquals(fromHexString("830186a0"), RLP.encode(writer -> writer.writeLong(100000L)));
  }

  @Test
  void shouldWriteUInt256Integers() {
    assertEquals(fromHexString("80"), RLP.encode(writer -> writer.writeUInt256(UInt256.valueOf(0L))));
    assertEquals(fromHexString("830186a0"), RLP.encode(writer -> writer.writeUInt256(UInt256.valueOf(100000L))));
    assertEquals(
        fromHexString("a00400000000000000000000000000000000000000000000000000f100000000ab"),
        RLP
            .encode(
                writer -> writer
                    .writeUInt256(
                        UInt256.fromHexString("0x0400000000000000000000000000000000000000000000000000f100000000ab"))));
  }

  @Test
  void shouldWriteBigIntegers() {
    assertEquals(fromHexString("830186a0"), RLP.encode(writer -> writer.writeBigInteger(BigInteger.valueOf(100000))));
    assertEquals(
        fromHexString("8ee1ceefa5bbd9ed1c97f17a1df801"),
        RLP.encode(writer -> writer.writeBigInteger(BigInteger.valueOf(127).pow(16))));
  }

  @Test
  void shouldWriteEmptyStrings() {
    assertEquals(fromHexString("80"), RLP.encode(writer -> writer.writeString("")));
  }

  @Test
  void shouldWriteOneCharactersStrings() {
    assertEquals(fromHexString("64"), RLP.encode(writer -> writer.writeString("d")));
  }

  @Test
  void shouldWriteStrings() {
    assertEquals(fromHexString("83646f67"), RLP.encode(writer -> writer.writeString("dog")));
  }

  @Test
  void shouldWriteShortLists() {
    List<String> strings =
        Arrays.asList("asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer");

    assertEquals(
        fromHexString(
            "f784617364668471776572847a78637684617364668471776572847a"
                + "78637684617364668471776572847a78637684617364668471776572"),
        RLP.encodeList(listWriter -> strings.forEach(listWriter::writeString)));
  }

  @Test
  void shouldWriteShortListWithAFunction() {
    List<String> strings =
        Arrays.asList("asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer");

    assertEquals(
        fromHexString(
            "f784617364668471776572847a78637684617364668471776572847a"
                + "78637684617364668471776572847a78637684617364668471776572"),
        RLP.encodeList(strings, RLPWriter::writeString));
  }

  @Test
  void shouldWriteNestedLists() {
    Bytes bytes = RLP.encodeList(listWriter -> {
      listWriter.writeString("asdf");
      listWriter.writeString("qwer");
      for (int i = 30; i >= 0; --i) {
        listWriter.writeList(subListWriter -> {
          subListWriter.writeString("zxcv");
          subListWriter.writeString("asdf");
          subListWriter.writeString("qwer");
        });
      }
    });

    assertTrue(RLP.<Boolean>decodeList(bytes, listReader -> {
      assertEquals("asdf", listReader.readString());
      assertEquals("qwer", listReader.readString());

      for (int i = 30; i >= 0; --i) {
        assertTrue(listReader.<Boolean>readList(subListReader -> {
          assertEquals("zxcv", subListReader.readString());
          assertEquals("asdf", subListReader.readString());
          assertEquals("qwer", subListReader.readString());
          return true;
        }));
      }

      return true;
    }));
  }

  @Test
  void shouldWritePreviouslyEncodedValues() {
    Bytes output = RLP.encode(writer -> writer.writeRLP(RLP.encodeByteArray("abc".getBytes(UTF_8))));
    assertEquals("abc", RLP.decodeString(output));
  }
}
