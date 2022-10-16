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
      "{\"active\":true,\"gasPrice\":5000,\"hashrate\":42,\"mining\":true,\"peers\":23,\"syncing\":true,\"uptime\":1234567}",
      mapper.writeValueAsString(stats)
    )
  }
}
