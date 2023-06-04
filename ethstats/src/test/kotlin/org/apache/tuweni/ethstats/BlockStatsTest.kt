// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
