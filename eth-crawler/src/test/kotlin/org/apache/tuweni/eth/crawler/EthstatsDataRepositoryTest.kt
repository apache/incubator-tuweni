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
package org.apache.tuweni.eth.crawler

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.ethstats.NodeInfo
import org.apache.tuweni.ethstats.NodeStats
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EthstatsDataRepositoryTest {

  private var repository: EthstatsDataRepository? = null

  @BeforeEach
  fun setUpRepository() {
    val ds = HikariDataSource()
    ds.jdbcUrl = "jdbc:h2:mem:testdb"
    val flyway = Flyway.configure()
      .dataSource(ds)
      .load()
    flyway.migrate()
    repository = EthstatsDataRepository(ds)
  }

  @Test
  fun testStoreLatency() {
    runBlocking {
      repository!!.storeLatency("foo", "bar", 42).await()
      val peerData = repository!!.getPeerData("bar").await()
      assertEquals(42, peerData?.latency)
    }
  }

  @Test
  fun testStorePendingTx() {
    runBlocking {
      repository!!.storePendingTx("foo", "bar", 42).await()
      val peerData = repository!!.getPeerData("bar").await()
      assertEquals(42, peerData?.pendingTx)
    }
  }

  @Test
  fun testStoreNodeInfo() {
    runBlocking {
      val nodeInfo = NodeInfo("foo", "node", 123, "eth", "protocol", os = "os", osVer = "123", client = "wow")
      repository!!.storeNodeInfo("foo", "bar", nodeInfo).await()
      val peerData = repository!!.getPeerData("bar").await()
      assertEquals(nodeInfo, peerData?.nodeInfo)
    }
  }

  @Test
  fun testStoreNodeStats() {
    runBlocking {
      val nodeStats = NodeStats(true, false, true, 42, 9, 4000, 100)
      repository!!.storeNodeStats("foo", "bar", nodeStats).await()
      val peerData = repository!!.getPeerData("bar").await()
      assertEquals(nodeStats, peerData?.nodeStats)
    }
  }
}
