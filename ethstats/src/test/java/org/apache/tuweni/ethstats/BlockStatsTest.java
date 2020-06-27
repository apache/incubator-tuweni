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
package org.apache.tuweni.ethstats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.eth.Address;
import org.apache.tuweni.eth.EthJsonModule;
import org.apache.tuweni.eth.Hash;
import org.apache.tuweni.units.bigints.UInt256;

import java.util.Collections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BlockStatsTest {

  @Test
  void toJson() throws JsonProcessingException {
    BlockStats stats = new BlockStats(
        UInt256.ONE,
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        32L,
        Address.fromBytes(Bytes.random(20)),
        23L,
        4000L,
        UInt256.ZERO,
        UInt256.ONE,
        Collections.singletonList(new TxStats(Hash.fromBytes(Bytes32.random()))),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Collections.emptyList());
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new EthJsonModule());
    assertEquals(
        "{\"number\":1,\"hash\":\""
            + stats.getHash()
            + "\",\"parentHash\":\""
            + stats.getParentHash()
            + "\",\"timestamp\":32,\"miner\":\""
            + stats.getMiner().toHexString()
            + "\",\"gasUsed\":23,\"gasLimit\":4000,\"difficulty\":\"0\",\"totalDifficulty\":\"1\",\"transactions\":[{\"hash\":\""
            + stats.getTransactions().get(0).getHash().toHexString()
            + "\"}],\"transactionsRoot\":\""
            + stats.getTransactionsRoot()
            + "\",\"stateRoot\":\""
            + stats.getStateRoot()
            + "\",\"uncles\":[]}",
        mapper.writeValueAsString(stats));
  }
}
