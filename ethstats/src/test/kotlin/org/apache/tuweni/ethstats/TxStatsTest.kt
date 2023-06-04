// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethstats

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.Hash
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TxStatsTest {

  @Test
  fun toJson() {
    val stats = TxStats(Hash.fromBytes(Bytes32.random()))
    val mapper = ObjectMapper()
    mapper.registerModule(EthJsonModule())
    assertEquals("{\"hash\":\"" + stats.hash.toHexString() + "\"}", mapper.writeValueAsString(stats))
  }
}
