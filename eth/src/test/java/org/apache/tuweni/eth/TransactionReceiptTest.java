// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.rlp.RLP;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class TransactionReceiptTest {

  @Test
  void testRLPRoundtrip() {
    List<Log> logs =
        Collections.singletonList(
            new Log(
                Address.fromBytes(Bytes.random(20)),
                Bytes.of(1, 2, 3),
                Arrays.asList(Bytes32.random(), Bytes32.random())));

    LogsBloomFilter filter = LogsBloomFilter.compute(logs);

    TransactionReceipt transactionReceipt =
        new TransactionReceipt(Bytes32.random(), 2, filter, logs);
    Bytes rlp = RLP.encode(transactionReceipt::writeTo);
    TransactionReceipt read = RLP.decode(rlp, TransactionReceipt::readFrom);
    assertEquals(transactionReceipt, read);
  }

  @Test
  void testRLPRoundtripWithStatus() {
    List<Log> logs =
        Arrays.asList(
            new Log(
                Address.fromBytes(Bytes.random(20)),
                Bytes.of(1, 2, 3),
                Arrays.asList(Bytes32.random(), Bytes32.random())));

    LogsBloomFilter filter = LogsBloomFilter.compute(logs);

    TransactionReceipt transactionReceipt = new TransactionReceipt(1, 2, filter, logs);
    Bytes rlp = RLP.encode(transactionReceipt::writeTo);
    TransactionReceipt read = RLP.decode(rlp, TransactionReceipt::readFrom);
    assertEquals(transactionReceipt, read);
  }
}
