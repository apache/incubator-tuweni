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
import org.apache.tuweni.rlp.RLP;
import org.apache.tuweni.rlp.RLPException;
import org.apache.tuweni.rlp.RLPReader;
import org.apache.tuweni.rlp.RLPWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.MustBeClosed;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RLPReferenceTestSuite {
  private static void writePayload(RLPWriter writer, Object in) {
    if (in instanceof String && ((String) in).startsWith("#")) {
      writer.writeBigInteger(new BigInteger(((String) in).substring(1)));
    } else if (in instanceof String) {
      writer.writeString((String) in);
    } else if (in instanceof BigInteger) {
      writer.writeBigInteger((BigInteger) in);
    } else if (in instanceof Integer) {
      writer.writeValue(Bytes.minimalBytes((Integer) in));
    } else if (in instanceof List) {
      writer.writeList((listWriter) -> {
        for (Object elt : (List) in) {
          writePayload(listWriter, elt);
        }
      });
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private static Object readPayload(RLPReader reader, Object in) {
    if (in instanceof List) {
      return reader.readList((listReader, list) -> {
        for (Object elt : ((List) in)) {
          list.add(readPayload(listReader, elt));
        }
      });
    } else if (in instanceof BigInteger) {
      return reader.readBigInteger();
    } else if (in instanceof String) {
      return reader.readString();
    } else if (in instanceof Integer) {
      return reader.readInt();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @ParameterizedTest(name = "{index}. write {0}")
  @MethodSource("readRLPTests")
  void testWriteRLP(String name, Object in, String out) throws IOException {
    Bytes encoded = RLP.encode((writer) -> writePayload(writer, in));
    assertEquals(Bytes.fromHexString(out), encoded, "Input was of type " + in.getClass());
  }

  @ParameterizedTest(name = "{index}. read {0}")
  @MethodSource("readRLPTests")
  void testReadRLP(String name, Object in, String out) {
    if (in instanceof String && ((String) in).startsWith("#")) {
      in = new BigInteger(((String) in).substring(1));
    }
    Object payload = in;
    Object decoded = RLP.decode(Bytes.fromHexString(out), (reader) -> readPayload(reader, payload));
    assertEquals(in, decoded);
  }

  @ParameterizedTest(name = "{index}. invalid {0}")
  @MethodSource("readInvalidRLPTests")
  void testReadInvalidRLP(String name, Object in, String out) {
    assertThrows(RLPException.class, () -> {
      if ("incorrectLengthInArray".equals(name)) {
        RLP.decodeToList(Bytes.fromHexString(out), (reader, list) -> {
        });
      } else {
        RLP.decodeValue(Bytes.fromHexString(out));
      }
    });
  }

  @SuppressWarnings("UnusedMethod")
  @MustBeClosed
  private static Stream<Arguments> readRLPTests() throws IOException {
    return findTests("**/RLPTests/rlptest.json");
  }

  @SuppressWarnings("UnusedMethod")
  @MustBeClosed
  private static Stream<Arguments> readInvalidRLPTests() throws IOException {
    return findTests("**/RLPTests/invalidRLPTest.json");
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
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Map> allTests = mapper.readerFor(Map.class).readValue(in);
    return allTests
        .entrySet()
        .stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue().get("in"), entry.getValue().get("out")));
  }
}
