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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.io.Resources;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.trie.MerklePatriciaTrie;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.MustBeClosed;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BouncyCastleExtension.class)
class MerkleTrieTestSuite {

  private Bytes readFromString(String value) {
    if (value.startsWith("0x")) {
      return Bytes.fromHexString(value);
    } else {
      return Bytes.wrap(value.getBytes(UTF_8));
    }
  }

  @ParameterizedTest(name = "{index}. {0}")
  @MethodSource("readAnyOrderTrieTests")
  @SuppressWarnings({"unchecked", "rawtypes"})
  void testAnyOrderTrieTrees(String name, Map input, String root) throws Exception {
    MerklePatriciaTrie<String> trie = MerklePatriciaTrie.create(this::readFromString);
    for (Object entry : input.entrySet()) {
      Map.Entry keyValue = (Map.Entry) entry;
      trie.putAsync(readFromString((String) keyValue.getKey()), (String) keyValue.getValue()).join();
    }
    assertEquals(Bytes.fromHexString(root), trie.rootHash());
  }

  @ParameterizedTest(name = "{index}. {0}")
  @MethodSource("readTrieTests")
  @SuppressWarnings({"unchecked", "rawtypes"})
  void testTrieTrees(String name, List input, String root) throws Exception {
    MerklePatriciaTrie<String> trie = MerklePatriciaTrie.create(this::readFromString);
    for (Object entry : input) {
      List keyValue = (List) entry;
      trie.putAsync(readFromString((String) keyValue.get(0)), (String) keyValue.get(1)).join();
    }
    assertEquals(Bytes.fromHexString(root), trie.rootHash());
  }

  @SuppressWarnings("UnusedMethod")
  @MustBeClosed
  private static Stream<Arguments> readTrieTests() throws IOException {
    return findTests("**/TrieTests/trietest.json");
  }

  @SuppressWarnings("UnusedMethod")
  @MustBeClosed
  private static Stream<Arguments> readAnyOrderTrieTests() throws IOException {
    return findTests("**/TrieTests/trieanyorder.json");
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
    Map<String, Map> allTests = new ObjectMapper().readerFor(Map.class).readValue(in);
    return allTests
        .entrySet()
        .stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue().get("in"), entry.getValue().get("root")));
  }
}
