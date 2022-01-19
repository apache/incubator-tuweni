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

import static org.apache.tuweni.bytes.Bytes.fromHexString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BytesRLPReaderTest {

  private static final Bytes SHORT_LIST = fromHexString(
      "f784617364668471776572847a78637684617364668471776572847a78637684617364668471776572847a78637684617364668471776572");
  private static final Bytes LONG_LIST = fromHexString(
      "f90200cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf8461736"
          + "4668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572"
          + "847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf8"
          + "4617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471"
          + "776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786"
          + "376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364"
          + "668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf846173646684717765728"
          + "47a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf84"
          + "617364668471776572847a786376cf84617364668471776572847a786376cf84617364668471776572847a786376cf846173646684717"
          + "76572847a786376cf84617364668471776572847a786376");

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
  void shouldParseFullObjects() {
    Bytes bytes = fromHexString("83426f620486011F71B70768");
    SomeObject readObject =
        RLP.decode(bytes, reader -> new SomeObject(reader.readString(), reader.readInt(), reader.readBigInteger()));

    assertEquals("Bob", readObject.name);
    assertEquals(4, readObject.number);
    assertEquals(BigInteger.valueOf(1234563434344L), readObject.longNumber);
  }

  @ParameterizedTest
  @CsvSource({"80, 0", "01, 1", "10, 16", "4f, 79", "7f, 127", "8180, 128", "8203e8, 1000", "830186a0, 100000"})
  void shouldReadIntegers(String hex, int value) {
    assertTrue(RLP.<Boolean>decode(fromHexString(hex), reader -> {
      assertEquals(value, reader.readInt());
      return true;
    }));
  }

  @ParameterizedTest
  // @formatter:off
  @CsvSource({
    "80, ''",
    "00, '\u0000'",
    "01, '\u0001'",
    "7f, '\u007F'",
    "83646f67, dog",
    "b74c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c69" +
    ", 'Lorem ipsum dolor sit amet, consectetur adipisicing eli'",
    "b8384c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c6974" +
    ", 'Lorem ipsum dolor sit amet, consectetur adipisicing elit'",
    "b904004c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e73656374657475722061646970697363696e6720656c69742e20437572616269747572206d6175726973206d61676e612c20737573636970697420736564207665686963756c61206e6f6e2c20696163756c697320666175636962757320746f72746f722e2050726f696e20737573636970697420756c74726963696573206d616c6573756164612e204475697320746f72746f7220656c69742c2064696374756d2071756973207472697374697175652065752c20756c7472696365732061742072697375732e204d6f72626920612065737420696d70657264696574206d6920756c6c616d636f7270657220616c6971756574207375736369706974206e6563206c6f72656d2e2041656e65616e2071756973206c656f206d6f6c6c69732c2076756c70757461746520656c6974207661726975732c20636f6e73657175617420656e696d2e204e756c6c6120756c74726963657320747572706973206a7573746f2c20657420706f73756572652075726e6120636f6e7365637465747572206e65632e2050726f696e206e6f6e20636f6e76616c6c6973206d657475732e20446f6e65632074656d706f7220697073756d20696e206d617572697320636f6e67756520736f6c6c696369747564696e2e20566573746962756c756d20616e746520697073756d207072696d697320696e206661756369627573206f726369206c756374757320657420756c74726963657320706f737565726520637562696c69612043757261653b2053757370656e646973736520636f6e76616c6c69732073656d2076656c206d617373612066617563696275732c2065676574206c6163696e6961206c616375732074656d706f722e204e756c6c61207175697320756c747269636965732070757275732e2050726f696e20617563746f722072686f6e637573206e69626820636f6e64696d656e74756d206d6f6c6c69732e20416c697175616d20636f6e73657175617420656e696d206174206d65747573206c75637475732c206120656c656966656e6420707572757320656765737461732e20437572616269747572206174206e696268206d657475732e204e616d20626962656e64756d2c206e6571756520617420617563746f72207472697374697175652c206c6f72656d206c696265726f20616c697175657420617263752c206e6f6e20696e74657264756d2074656c6c7573206c65637475732073697420616d65742065726f732e20437261732072686f6e6375732c206d65747573206163206f726e617265206375727375732c20646f6c6f72206a7573746f20756c747269636573206d657475732c20617420756c6c616d636f7270657220766f6c7574706174" +
    ", 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur mauris magna, suscipit sed vehicula non, iaculis faucibus tortor. Proin suscipit ultricies malesuada. Duis tortor elit, dictum quis tristique eu, ultrices at risus. Morbi a est imperdiet mi ullamcorper aliquet suscipit nec lorem. Aenean quis leo mollis, vulputate elit varius, consequat enim. Nulla ultrices turpis justo, et posuere urna consectetur nec. Proin non convallis metus. Donec tempor ipsum in mauris congue sollicitudin. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Suspendisse convallis sem vel massa faucibus, eget lacinia lacus tempor. Nulla quis ultricies purus. Proin auctor rhoncus nibh condimentum mollis. Aliquam consequat enim at metus luctus, a eleifend purus egestas. Curabitur at nibh metus. Nam bibendum, neque at auctor tristique, lorem libero aliquet arcu, non interdum tellus lectus sit amet eros. Cras rhoncus, metus ac ornare cursus, dolor justo ultrices metus, at ullamcorper volutpat'"
  })
  // @formatter:on
  void shouldReadStrings(String hex, String value) {
    assertTrue(RLP.<Boolean>decode(fromHexString(hex), reader -> {
      assertEquals(value, reader.readString());
      return true;
    }));
  }

  @Test
  void shouldThrowWhenInputExhausted() {
    EndOfRLPException ex = assertThrows(EndOfRLPException.class, () -> RLP.decode(Bytes.EMPTY, RLPReader::readInt));
    assertEquals("End of RLP source reached", ex.getMessage());
  }

  @Test
  void shouldThrowWhenNextItemIsAList() {
    InvalidRLPTypeException ex =
        assertThrows(InvalidRLPTypeException.class, () -> RLP.decode(SHORT_LIST, RLPReader::readInt));
    assertEquals("Attempted to read a value but next item is a list", ex.getMessage());
  }

  @Test
  void shouldThrowWheSourceIsTruncated() {
    InvalidRLPEncodingException ex =
        assertThrows(InvalidRLPEncodingException.class, () -> RLP.decode(fromHexString("830186"), RLPReader::readInt));
    assertEquals("Insufficient bytes in RLP encoding: expected 3 but have only 2", ex.getMessage());
  }

  @Test
  void shouldThrowWhenLowValueIsntEncodedToSingleByte() {
    Bytes bytes1 = fromHexString("8128");
    InvalidRLPEncodingException ex1 = assertThrows(InvalidRLPEncodingException.class, () -> RLP.decodeInt(bytes1));
    assertEquals("Value should have been encoded as a single byte 0x28", ex1.getMessage());
    assertEquals(40, RLP.decodeInt(bytes1, true));

    Bytes bytes2 = fromHexString("b80128");
    InvalidRLPEncodingException ex2 = assertThrows(InvalidRLPEncodingException.class, () -> RLP.decodeInt(bytes1));
    assertEquals("Value should have been encoded as a single byte 0x28", ex2.getMessage());
    assertEquals(40, RLP.decodeInt(bytes2, true));
  }

  @Test
  void shouldThrowWhenValueLengthContainsLeadingZeros() {
    Bytes bytes1 = fromHexString(
        "b900384c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c6974");
    InvalidRLPEncodingException ex1 = assertThrows(InvalidRLPEncodingException.class, () -> RLP.decodeString(bytes1));
    assertEquals("RLP value length contains leading zero bytes", ex1.getMessage());
    assertEquals("Lorem ipsum dolor sit amet, consectetur adipisicing elit", RLP.decodeString(bytes1, true));

    Bytes bytes2 = fromHexString("bb0000000028");
    InvalidRLPEncodingException ex2 = assertThrows(InvalidRLPEncodingException.class, () -> RLP.decodeInt(bytes2));
    assertEquals("RLP value length contains leading zero bytes", ex2.getMessage());
    InvalidRLPEncodingException ex3 =
        assertThrows(InvalidRLPEncodingException.class, () -> RLP.decodeInt(bytes2, true));
    assertEquals("RLP value length is zero", ex3.getMessage());

    Bytes bytes3 = fromHexString("bd00000000000128");
    InvalidRLPEncodingException ex4 = assertThrows(InvalidRLPEncodingException.class, () -> RLP.decodeInt(bytes3));
    assertEquals("RLP value length contains leading zero bytes", ex4.getMessage());
    assertEquals(40, RLP.decodeInt(bytes3, true));
  }

  @Test
  void shouldThrowWhenListLengthContainsLeadingZeros() {
    Bytes bytes1 = fromHexString("0xF9000101");
    InvalidRLPEncodingException ex1 =
        assertThrows(InvalidRLPEncodingException.class, () -> RLP.decodeList(bytes1, RLPReader::readInt));
    assertEquals("RLP list length contains leading zero bytes", ex1.getMessage());
    assertEquals(Integer.valueOf(1), RLP.decodeList(bytes1, true, RLPReader::readInt));

    Bytes bytes2 = fromHexString("0xF80101");
    InvalidRLPEncodingException ex2 =
        assertThrows(InvalidRLPEncodingException.class, () -> RLP.decodeList(bytes2, RLPReader::readInt));
    assertEquals("RLP list length of 1 was not minimally encoded", ex2.getMessage());
    assertEquals(Integer.valueOf(1), RLP.decodeList(bytes2, true, RLPReader::readInt));
  }

  @Test
  void shouldThrowWhenLengthIsOversized() {
    Bytes bytes1 = fromHexString("bc0aaaaaaaaa28");
    InvalidRLPEncodingException ex1 = assertThrows(InvalidRLPEncodingException.class, () -> RLP.decodeInt(bytes1));
    assertEquals("RLP value length is oversized", ex1.getMessage());

    Bytes bytes2 = fromHexString("bb8000000128");
    InvalidRLPEncodingException ex2 = assertThrows(InvalidRLPEncodingException.class, () -> RLP.decodeInt(bytes2));
    assertEquals("RLP value length is oversized", ex2.getMessage());
  }

  @Test
  void shouldReadShortList() {
    List<String> expected =
        Arrays.asList("asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer");

    List<Object> result = RLP.decodeToList(SHORT_LIST, (reader, list) -> {
      assertEquals(11, reader.remaining());
      for (int i = 10; i >= 0; --i) {
        list.add(reader.readString());
      }
    });
    assertEquals(expected, result);
  }

  @Test
  void shouldReadShortListContents() {
    List<String> expected =
        Arrays.asList("asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer");

    List<String> result = RLP.decodeToList(SHORT_LIST, reader -> reader.readString());
    assertEquals(expected, result);
  }

  @Test
  void shouldReadLongList() {
    List<List<String>> expected =
        Stream.generate(() -> Arrays.asList("asdf", "qwer", "zxcv")).limit(31).collect(Collectors.toList());

    List<Object> result = RLP.decodeToList(LONG_LIST, (reader, list) -> {
      for (int i = 30; i >= 0; --i) {
        list.add(reader.readList((subReader, subList) -> {
          subList.add(subReader.readString());
          subList.add(subReader.readString());
          subList.add(subReader.readString());
        }));
      }
    });
    assertEquals(expected, result);
  }

  @Test
  void shouldReadRemaining() {
    Bytes input = Bytes.fromHexString("83646f6783646f6783646f67");
    RLP.decode(input, reader -> {
      reader.readValue();
      assertEquals(4, reader.position());
      assertEquals(Bytes.fromHexString("83646f6783646f67"), reader.readRemaining());
      return null;
    });
  }
}
