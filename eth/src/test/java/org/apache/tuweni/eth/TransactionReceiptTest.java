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
    List<Log> logs = Collections
        .singletonList(
            new Log(
                Address.fromBytes(Bytes.random(20)),
                Bytes.of(1, 2, 3),
                Arrays.asList(Bytes32.random(), Bytes32.random())));

    LogsBloomFilter filter = LogsBloomFilter.compute(logs);

    TransactionReceipt transactionReceipt = new TransactionReceipt(Bytes32.random(), 2, filter, logs);
    Bytes rlp = RLP.encode(transactionReceipt::writeTo);
    TransactionReceipt read = RLP.decode(rlp, TransactionReceipt::readFrom);
    assertEquals(transactionReceipt, read);
  }

  @Test
  void testRLPRoundtripWithStatus() {
    List<Log> logs = Arrays
        .asList(
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
