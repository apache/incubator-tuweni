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
    topics.add(Bytes32.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000"));

    Log log = new Log(address, data, topics);
    LogsBloomFilter bloom = new LogsBloomFilter();
    bloom.insertLog(log);

    assertEquals(
        Bytes
            .fromHexString(
                "0x00000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000040000000000000000000000000000000000000000000000000000000"),
        bloom.toBytes());
  }

}
