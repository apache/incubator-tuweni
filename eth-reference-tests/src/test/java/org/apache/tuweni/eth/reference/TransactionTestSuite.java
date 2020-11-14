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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.eth.Address;
import org.apache.tuweni.eth.Transaction;
import org.apache.tuweni.io.Resources;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.rlp.RLPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
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
class TransactionTestSuite {

  private static ObjectMapper mapper = new ObjectMapper();

  @ParameterizedTest(name = "{index}. tx {0}/{1}")
  @MethodSource("readTransactionTests")
  void testTransaction(String name, String milestone, String rlp, String hash, String sender) {
    if (hash == null || sender == null) {
      assertTrue(hash == null && sender == null, "Invalid test case");
      testInvalidTransaction(rlp, milestone);
    } else {
      testValidTransaction(rlp, hash, sender);
    }
  }

  private void testValidTransaction(String rlp, String hash, String sender) {
    Bytes rlpBytes = Bytes.fromHexString(rlp);
    Transaction tx = Transaction.fromBytes(rlpBytes);
    assertEquals(Address.fromBytes(Bytes.fromHexString(sender)), tx.getSender());
    assertEquals(rlpBytes, tx.toBytes());
    assertEquals(Bytes.fromHexString(hash), tx.getHash());
  }

  private void testInvalidTransaction(String rlp, String milestone) {
    Bytes rlpBytes;
    try {
      rlpBytes = Bytes.fromHexString(rlp);
    } catch (IllegalArgumentException e) {
      return;
    }

    Transaction tx;
    try {
      tx = Transaction.fromBytes(rlpBytes);
    } catch (RLPException e) {
      return;
    }

    if (tx.getSender() == null) {
      return;
    }

    if ("Constantinople".equals(milestone) || "Byzantium".equals(milestone) || "EIP158".equals(milestone)) {
      if (tx.getChainId() == null) {
        return;
      }
    } else {
      if (tx.getChainId() != null) {
        return;
      }
    }

    fail("Expected an invalid transaction but it was successfully read");
  }

  @SuppressWarnings("UnusedMethod")
  @MustBeClosed
  private static Stream<Arguments> readTransactionTests() throws IOException {
    return Resources
        .find("**/TransactionTests/**/*.json")
        .filter(
            url -> !url.getPath().contains("GasLimitOverflow")
                && !url.getPath().contains("GasLimitxPriceOverflow")
                && !url.getPath().contains("NotEnoughGas")
                && !url.getPath().contains("NotEnoughGAS")
                && !url.getPath().contains("EmptyTransaction"))
        .flatMap(url -> {
          try (InputStream in = url.openConnection().getInputStream()) {
            return readTestCase(in);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        })
        .sorted(Comparator.comparing(a -> ((String) a.get()[0])));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Stream<Arguments> readTestCase(InputStream in) throws IOException {
    Map<String, Map> tests = mapper.readerFor(Map.class).readValue(in);
    return tests.entrySet().stream().flatMap(entry -> {
      String name = entry.getKey();
      Map testData = entry.getValue();
      String rlp = (String) testData.get("rlp");
      List<Arguments> arguments = new ArrayList<>();
      for (String milestone : new String[] {
          "Byzantium",
          "Constantinople",
          "EIP150",
          "EIP158",
          "Frontier",
          "Homestead"}) {
        Map milestoneData = (Map) testData.get(milestone);
        if (!milestoneData.isEmpty()) {
          arguments.add(Arguments.of(name, milestone, rlp, milestoneData.get("hash"), milestoneData.get("sender")));
        }
      }
      if (arguments.isEmpty()) {
        arguments.add(Arguments.of(name, "(no milestone)", rlp, null, null));
      }
      return arguments.stream();
    });
  }
}
