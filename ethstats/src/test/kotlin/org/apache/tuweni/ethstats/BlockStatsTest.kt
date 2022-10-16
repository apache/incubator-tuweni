/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.ethstats

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.units.bigints.UInt256
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Collections

class BlockStatsTest {

  @Test
  fun toJson() {
    val stats = BlockStats(
      UInt256.ONE,
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      32L,
      Address.fromBytes(Bytes.random(20)),
      23L,
      4000L,
      UInt256.ZERO,
      UInt256.ONE,
      listOf(TxStats(Hash.fromBytes(Bytes32.random()))),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Collections.emptyList()
    )
    val mapper = ObjectMapper()
    mapper.registerModule(EthJsonModule())
    assertEquals(
      "{\"difficulty\":\"0x0000000000000000000000000000000000000000000000000000000000000000\"," +
        "\"gasLimit\":4000," +
        "\"gasUsed\":23," +
        "\"hash\":\"" + stats.hash + "\"," +
        "\"miner\":\"" + stats.miner.toHexString() + "\"," +
        "\"number\":1," +
        "\"parentHash\":\"" + stats.parentHash.toHexString() + "\"," +
        "\"stateRoot\":\"" + stats.stateRoot + "\"," +
        "\"timestamp\":32," +
        "\"totalDifficulty\":\"0x0000000000000000000000000000000000000000000000000000000000000001\"," +
        "\"transactions\":[{\"hash\":\"" + stats.transactions.get(0).hash.toHexString() + "\"}]," +
        "\"transactionsRoot\":\"" + stats.transactionsRoot + "\",\"uncles\":[]}",
      mapper.writeValueAsString(stats)
    )
  }
}
