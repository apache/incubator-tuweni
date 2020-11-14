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
package org.apache.tuweni.eth.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.io.Resources;
import org.apache.tuweni.ssz.SSZ;
import org.apache.tuweni.ssz.SSZException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.errorprone.annotations.MustBeClosed;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SSZTestSuite {

  private Bytes encodeValue(String value, int bitLength) {
    if (bitLength < 31) {
      return SSZ.encode(writer -> writer.writeUInt(Integer.valueOf(value), bitLength));
    } else if (bitLength < 62) {
      return SSZ.encode(writer -> writer.writeULong(Long.valueOf(value), bitLength));
    } else {
      return SSZ.encode(writer -> writer.writeUBigInteger(new BigInteger(value), bitLength));
    }
  }

  @ParameterizedTest(name = "{index}. value->ssz {0} {2}->{3}")
  @MethodSource("readUintBoundsTests")
  void testUintBoundsToSSZ(String type, boolean valid, String value, String ssz) {
    int bitLength = Integer.valueOf(type.substring("uint".length()));
    if (valid) {
      Bytes encoded = encodeValue(value, bitLength);
      assertEquals(Bytes.fromHexString(ssz), encoded);
    } else {
      assertThrows(IllegalArgumentException.class, () -> encodeValue(value, bitLength));
    }
  }

  @ParameterizedTest(name = "{index}. ssz->value {0} {3}->{2}")
  @MethodSource("readUintBoundsTests")
  void testUintBoundsFromSSZ(String type, boolean valid, String value, String ssz) {
    if (valid) {
      int bitLength = Integer.valueOf(type.substring("uint".length()));
      Bytes read;
      if (bitLength < 31) {
        read = Bytes.ofUnsignedLong((long) SSZ.decode(Bytes.fromHexString(ssz), reader -> reader.readUInt(bitLength)));
      } else if (bitLength < 62) {
        read = Bytes.ofUnsignedLong(SSZ.decode(Bytes.fromHexString(ssz), reader -> reader.readULong(bitLength)));
      } else {
        read = Bytes
            .wrap(
                SSZ.decode(Bytes.fromHexString(ssz), reader -> reader.readUnsignedBigInteger(bitLength)).toByteArray());
      }
      assertEquals(Bytes.wrap(new BigInteger(value).toByteArray()).toShortHexString(), read.toShortHexString());
    }
  }

  @ParameterizedTest(name = "{index}. random value->ssz {0} {2}->{3}")
  @MethodSource("readUintRandomTests")
  void testUintRandomToSSZ(String type, boolean valid, String value, String ssz) {
    int bitLength = Integer.valueOf(type.substring("uint".length()));
    if (valid) {
      Bytes encoded = encodeValue(value, bitLength);
      assertEquals(Bytes.fromHexString(ssz), encoded);
    } else {
      assertThrows(IllegalArgumentException.class, () -> encodeValue(value, bitLength));
    }
  }

  @ParameterizedTest(name = "{index}. ssz->value {0} {3}->{2}")
  @MethodSource("readUintRandomTests")
  void testUintRandomFromSSZ(String type, boolean valid, String value, String ssz) {
    if (valid) {
      int bitLength = Integer.valueOf(type.substring("uint".length()));
      Bytes read;
      if (bitLength < 31) {
        read = Bytes.ofUnsignedLong((long) SSZ.decode(Bytes.fromHexString(ssz), reader -> reader.readUInt(bitLength)));
      } else if (bitLength < 62) {
        read = Bytes.ofUnsignedLong(SSZ.decode(Bytes.fromHexString(ssz), reader -> reader.readULong(bitLength)));
      } else {
        read = Bytes
            .wrap(
                SSZ.decode(Bytes.fromHexString(ssz), reader -> reader.readUnsignedBigInteger(bitLength)).toByteArray());
      }
      assertEquals(Bytes.wrap(new BigInteger(value).toByteArray()).toShortHexString(), read.toShortHexString());
    }
  }

  @ParameterizedTest(name = "{index}. random value->ssz {0} {3}")
  @MethodSource("readUintWrongLength")
  void testUintWrongLengthFromSSZ(String type, boolean valid, String value, String ssz) {
    int bitLength = Integer.valueOf(type.substring("uint".length()));
    assertThrows(SSZException.class, () -> {
      if (bitLength < 31) {
        Bytes.ofUnsignedLong((long) SSZ.decode(Bytes.fromHexString(ssz), reader -> {
          int readInt = reader.readUInt(bitLength);
          if (!reader.isComplete()) {
            throw new SSZException("Should have read all bytes");
          }
          return readInt;
        }));
      } else if (bitLength < 62) {
        Bytes.ofUnsignedLong(SSZ.decode(Bytes.fromHexString(ssz), reader -> {
          long readLong = reader.readULong(bitLength);
          if (!reader.isComplete()) {
            throw new SSZException("Should have read all bytes");
          }
          return readLong;
        }));
      } else {
        Bytes.wrap(SSZ.decode(Bytes.fromHexString(ssz), reader -> {
          BigInteger readbi = reader.readUnsignedBigInteger(bitLength);
          if (!reader.isComplete()) {
            throw new SSZException("Should have read all bytes");
          }
          return readbi;
        }).toByteArray());
      }
    });
  }

  @SuppressWarnings("UnusedMethod")
  @MustBeClosed
  private static Stream<Arguments> readUintBoundsTests() throws IOException {
    return findTests("**/ssz/uint_bounds.yaml");
  }

  @SuppressWarnings("UnusedMethod")
  @MustBeClosed
  private static Stream<Arguments> readUintRandomTests() throws IOException {
    return findTests("**/ssz/uint_random.yaml");
  }

  @SuppressWarnings("UnusedMethod")
  @MustBeClosed
  private static Stream<Arguments> readUintWrongLength() throws IOException {
    return findTests("**/ssz/uint_wrong_length.yaml");
  }

  @MustBeClosed
  private static Stream<Arguments> findTests(String glob) throws IOException {
    return Resources.find(glob).flatMap(url -> {
      try (InputStream in = url.openConnection().getInputStream()) {
        return prepareTests(in);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Stream<Arguments> prepareTests(InputStream in) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Map allTests = mapper.readerFor(Map.class).readValue(in);

    return ((List<Map>) allTests.get("test_cases"))
        .stream()
        .map(
            testCase -> Arguments
                .of(testCase.get("type"), testCase.get("valid"), testCase.get("value"), testCase.get("ssz")));
  }
}
