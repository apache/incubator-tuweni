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

import static org.apache.tuweni.bytes.Bytes.fromHexString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.bytes.Bytes48;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BytesSSZReaderTest {

  private static final Bytes SHORT_LIST = fromHexString(
      "5800000004000000617364660400000071776572040000007A78637604000000617364660400000071776572040000007A78637604000000617364660400000071776572040000007A78637604000000617364660400000071776572");

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

  private static class AnotherObject {
    private final long number1;
    private final long number2;

    public AnotherObject(long number1, long number2) {
      this.number1 = number1;
      this.number2 = number2;
    }
  }

  @Test
  void shouldParseFullObjects() {
    Bytes bytes = fromHexString("0x03000000426F62046807B7711F010000000000000000000000000000000000000000000000000000");
    SomeObject readObject = SSZ.decode(bytes, r -> new SomeObject(r.readString(), r.readInt8(), r.readBigInteger(256)));

    assertEquals("Bob", readObject.name);
    assertEquals(4, readObject.number);
    assertEquals(BigInteger.valueOf(1234563434344L), readObject.longNumber);
  }

  @ParameterizedTest
  @CsvSource({
      "00, 0",
      "01, 1",
      "10, 16",
      "4f, 79",
      "7f, 127",
      "8000, 128",
      "e803, 1000",
      "a0860100, 100000",
      "a08601000000, 100000"})
  void shouldReadIntegers(String hex, int value) {
    assertTrue(SSZ.<Boolean>decode(fromHexString(hex), reader -> {
      assertEquals(value, reader.readInt(hex.length() * 4));
      return true;
    }));
  }

  /**
   * Related to the bug when {@link BytesSSZReader#readLong(int)} calculates lead zeroes from beginning of whole content
   * instead of the current value
   */
  @Test
  void shouldCorrectlyParseLongs() {
    Bytes bytes = fromHexString("7b00000000000000" + "ffffffffff7f0000");
    AnotherObject readObject = SSZ.decode(bytes, r -> new AnotherObject(r.readLong(64), r.readLong(64)));

    assertEquals(123, readObject.number1);
    assertEquals(140737488355327L, readObject.number2);
  }

  @Test
  void shouldThrowWhenReadingOversizedInt() {
    InvalidSSZTypeException ex = assertThrows(InvalidSSZTypeException.class, () -> {
      SSZ.decode(fromHexString("1122334455667788"), r -> r.readInt(64));
    });
    assertEquals("decoded integer is too large for an int", ex.getMessage());
  }

  @ParameterizedTest
  // @formatter:off
  @CsvSource({
    "00000000, ''",
    "0100000000, '\u0000'",
    "0100000001, '\u0001'",
    "010000007f, '\u007F'",
    "03000000646f67, dog",
    "370000004C6F72656D20697073756D20646F6C6F722073697420616D65742C20636F6E7365637465747572206164697069736963696E6720656C69" +
    ", 'Lorem ipsum dolor sit amet, consectetur adipisicing eli'",
    "380000004C6F72656D20697073756D20646F6C6F722073697420616D65742C20636F6E7365637465747572206164697069736963696E6720656C6974" +
    ", 'Lorem ipsum dolor sit amet, consectetur adipisicing elit'",
    "000400004C6F72656D20697073756D20646F6C6F722073697420616D65742C20636F6E73656374657475722061646970697363696E6720656C69742E20437572616269747572206D6175726973206D61676E612C20737573636970697420736564207665686963756C61206E6F6E2C20696163756C697320666175636962757320746F72746F722E2050726F696E20737573636970697420756C74726963696573206D616C6573756164612E204475697320746F72746F7220656C69742C2064696374756D2071756973207472697374697175652065752C20756C7472696365732061742072697375732E204D6F72626920612065737420696D70657264696574206D6920756C6C616D636F7270657220616C6971756574207375736369706974206E6563206C6F72656D2E2041656E65616E2071756973206C656F206D6F6C6C69732C2076756C70757461746520656C6974207661726975732C20636F6E73657175617420656E696D2E204E756C6C6120756C74726963657320747572706973206A7573746F2C20657420706F73756572652075726E6120636F6E7365637465747572206E65632E2050726F696E206E6F6E20636F6E76616C6C6973206D657475732E20446F6E65632074656D706F7220697073756D20696E206D617572697320636F6E67756520736F6C6C696369747564696E2E20566573746962756C756D20616E746520697073756D207072696D697320696E206661756369627573206F726369206C756374757320657420756C74726963657320706F737565726520637562696C69612043757261653B2053757370656E646973736520636F6E76616C6C69732073656D2076656C206D617373612066617563696275732C2065676574206C6163696E6961206C616375732074656D706F722E204E756C6C61207175697320756C747269636965732070757275732E2050726F696E20617563746F722072686F6E637573206E69626820636F6E64696D656E74756D206D6F6C6C69732E20416C697175616D20636F6E73657175617420656E696D206174206D65747573206C75637475732C206120656C656966656E6420707572757320656765737461732E20437572616269747572206174206E696268206D657475732E204E616D20626962656E64756D2C206E6571756520617420617563746F72207472697374697175652C206C6F72656D206C696265726F20616C697175657420617263752C206E6F6E20696E74657264756D2074656C6C7573206C65637475732073697420616D65742065726F732E20437261732072686F6E6375732C206D65747573206163206F726E617265206375727375732C20646F6C6F72206A7573746F20756C747269636573206D657475732C20617420756C6C616D636F7270657220766F6C7574706174" +
    ", 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur mauris magna, suscipit sed vehicula non, iaculis faucibus tortor. Proin suscipit ultricies malesuada. Duis tortor elit, dictum quis tristique eu, ultrices at risus. Morbi a est imperdiet mi ullamcorper aliquet suscipit nec lorem. Aenean quis leo mollis, vulputate elit varius, consequat enim. Nulla ultrices turpis justo, et posuere urna consectetur nec. Proin non convallis metus. Donec tempor ipsum in mauris congue sollicitudin. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Suspendisse convallis sem vel massa faucibus, eget lacinia lacus tempor. Nulla quis ultricies purus. Proin auctor rhoncus nibh condimentum mollis. Aliquam consequat enim at metus luctus, a eleifend purus egestas. Curabitur at nibh metus. Nam bibendum, neque at auctor tristique, lorem libero aliquet arcu, non interdum tellus lectus sit amet eros. Cras rhoncus, metus ac ornare cursus, dolor justo ultrices metus, at ullamcorper volutpat'"
  })
  // @formatter:on
  void shouldReadStrings(String hex, String value) {
    System.out.println(SSZ.encodeString(value));
    assertTrue(SSZ.<Boolean>decode(fromHexString(hex), reader -> {
      assertEquals(value, reader.readString());
      return true;
    }));
  }

  @Test
  void shouldThrowWhenInputExhausted() {
    EndOfSSZException ex =
        assertThrows(EndOfSSZException.class, () -> SSZ.decode(Bytes.EMPTY, reader -> reader.readInt(16)));
    assertEquals("End of SSZ source reached", ex.getMessage());
  }

  @Test
  void shouldThrowWheSourceIsTruncated() {
    InvalidSSZTypeException ex = assertThrows(
        InvalidSSZTypeException.class,
        () -> SSZ.decode(fromHexString("0000000f830186"), SSZReader::readBytes));
    assertEquals("SSZ encoded data has insufficient bytes for decoded byte array length", ex.getMessage());
  }

  @Test
  void shouldReadShortVaragsList() {
    List<String> expected =
        Arrays.asList("asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer");

    System.out
        .println(
            SSZ
                .encodeStringList(
                    "asdf",
                    "qwer",
                    "zxcv",
                    "asdf",
                    "qwer",
                    "zxcv",
                    "asdf",
                    "qwer",
                    "zxcv",
                    "asdf",
                    "qwer"));

    List<String> result = SSZ.decodeStringList(SHORT_LIST);
    assertEquals(expected, result);
  }

  @Test
  void shouldReadShortUtilList() {
    List<String> expected =
        Arrays.asList("asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer", "zxcv", "asdf", "qwer");

    System.out
        .println(
            SSZ
                .encodeStringList(
                    Arrays
                        .asList(
                            "asdf",
                            "qwer",
                            "zxcv",
                            "asdf",
                            "qwer",
                            "zxcv",
                            "asdf",
                            "qwer",
                            "zxcv",
                            "asdf",
                            "qwer")));

    List<String> result = SSZ.decodeStringList(SHORT_LIST);
    assertEquals(expected, result);
  }

  @Test
  void shouldAcceptStringListOfVariableLengths() {
    List<String> expected = Arrays.asList("one", "three", "four");

    List<String> result =
        SSZ.decodeStringList(Bytes.fromHexString("0x18000000030000006F6E6505000000746872656504000000666F7572"));
    assertEquals(expected, result);
  }

  @Test
  void shouldRoundtripBytesVararg() {
    List<Bytes> toWrite = Arrays.asList(Bytes48.random(), Bytes48.random(), Bytes48.random());
    Bytes encoded = SSZ.encode(writer -> writer.writeBytesList(toWrite.toArray(new Bytes[0])));
    assertEquals(toWrite, SSZ.decodeBytesList(encoded));

  }

  @Test
  void shouldRoundtripBytesList() {
    List<Bytes> toWrite = Arrays.asList(Bytes48.random(), Bytes48.random(), Bytes48.random());
    Bytes encoded = SSZ.encode(writer -> writer.writeBytesList(toWrite));
    assertEquals(toWrite, SSZ.decodeBytesList(encoded));
  }

  @Test
  void shouldRoundtripBytesVector() {
    List<Bytes> toWrite = Arrays.asList(Bytes48.random(), Bytes48.random(), Bytes48.random());
    Bytes encoded = SSZ.encode(writer -> writer.writeFixedBytesVector(toWrite));
    assertEquals(toWrite, SSZ.decode(encoded, reader -> reader.readFixedBytesVector(3, 48)));
  }

  @Test
  void shouldRoundtripHomogenousBytesList() {
    List<Bytes32> toWrite = Arrays.asList(Bytes32.random(), Bytes32.random(), Bytes32.random());
    Bytes encoded = SSZ.encode(writer -> writer.writeFixedBytesList(toWrite));
    assertEquals(toWrite, SSZ.decode(encoded, reader -> reader.readFixedBytesList(Bytes32.SIZE)));
  }
}
