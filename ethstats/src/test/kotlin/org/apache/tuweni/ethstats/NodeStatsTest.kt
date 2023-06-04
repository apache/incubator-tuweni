// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethstats

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.eth.EthJsonModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NodeStatsTest {

  @Test
  fun toJson() {
    val mapper = ObjectMapper()
    mapper.registerModule(EthJsonModule())
    val stats = NodeStats(true, true, true, 42, 23, 5000, 1234567)
    assertEquals(
      "{\"active\":true,\"gasPrice\":5000,\"hashrate\":42," +
        "\"mining\":true,\"peers\":23,\"syncing\":true,\"uptime\":1234567}",
      mapper.writeValueAsString(stats)
    )
  }
}
