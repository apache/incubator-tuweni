// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.crawler

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.ethstats.BlockStats
import org.apache.tuweni.ethstats.NodeInfo
import org.apache.tuweni.ethstats.NodeStats
import org.apache.tuweni.ethstats.TxStats
import org.apache.tuweni.units.bigints.UInt256
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("cannot work in CI")
class EthstatsDataRepositoryTest {

  private var repository: EthstatsDataRepository? = null

  @BeforeEach
  fun before() {
    val provider = EmbeddedPostgres.builder().start()
    val dataSource = provider.postgresDatabase
    val flyway = Flyway.configure().dataSource(dataSource).load()
    flyway.migrate()
    repository = EthstatsDataRepository(dataSource)
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

  @Test
  fun testStoreBlock() {
    runBlocking {
      val blockStats = BlockStats(
        UInt256.valueOf(33),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        42,
        Address.fromBytes(Bytes.random(20)),
        42,
        400,
        UInt256.valueOf(400),
        UInt256.valueOf(1600),
        listOf(TxStats(Hash.fromBytes(Bytes32.random())), TxStats(Hash.fromBytes(Bytes32.random()))),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        listOf()
      )
      repository!!.storeBlock("foo", "bar", blockStats).await()
      val block = repository!!.getLatestBlock("bar").await()
      assertEquals(blockStats, block)
    }
  }
}
