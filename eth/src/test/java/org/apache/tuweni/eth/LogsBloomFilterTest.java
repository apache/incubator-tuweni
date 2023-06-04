// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.junit.BouncyCastleExtension;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class LogsBloomFilterTest {

  @Test
  void logsBloomFilter() {
    Address address = Address.fromHexString("0x095e7baea6a6c7c4c2dfeb977efac326af552d87");
    Bytes data = Bytes.fromHexString("0x0102");
    List<Bytes32> topics = new ArrayList<>();
    topics.add(
        Bytes32.fromHexString(
            "0x0000000000000000000000000000000000000000000000000000000000000000"));

    Log log = new Log(address, data, topics);
    LogsBloomFilter bloom = new LogsBloomFilter();
    bloom.insertLog(log);

    assertEquals(
        Bytes.fromHexString(
            "0x00000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000040000000000000000000000000000000000000000000000000000000"),
        bloom.toBytes());
  }
}
