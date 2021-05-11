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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.apache.tuweni.ethstats.NodeStats
import org.apache.tuweni.ethstats.NodeInfo
import org.apache.tuweni.ethstats.BlockStats
import javax.sql.DataSource
import kotlin.coroutines.CoroutineContext

class EthstatsDataRepository(
  val ds: DataSource,
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : CoroutineScope {
  fun storeNodeInfo(remoteAddress: String, id: String, nodeInfo: NodeInfo) = async {
    ds.connection.use { conn ->
      val stmt =
        conn.prepareStatement(
          "insert into ethstats_peer(address, id, name, client, net, api, protocol, os, osVer, node, port) values(?,?,?,?,?,?,?,?,?,?,?)"
        )
      stmt.use {
        it.setString(1, remoteAddress)
        it.setString(2, id)
        it.setString(3, nodeInfo.name)
        it.setString(4, nodeInfo.client)
        it.setString(5, nodeInfo.net)
        it.setString(6, nodeInfo.api)
        it.setString(7, nodeInfo.protocol)
        it.setString(8, nodeInfo.os)
        it.setString(9, nodeInfo.osVersion())
        it.setString(10, nodeInfo.node)
        it.setInt(11, nodeInfo.port)
        it.execute()
      }
    }
  }

  fun storeBlock(remoteAddress: String, id: String, block: BlockStats) = async {
    println(remoteAddress + id + block)
    TODO()
  }

  fun storeLatency(remoteAddress: String, id: String, latency: Long) = async {
    ds.connection.use { conn ->
      val stmt =
        conn.prepareStatement(
          "insert into latency(address, id, value) values(?,?,?)"
        )
      stmt.use {
        it.setString(1, remoteAddress)
        it.setString(2, id)
        it.setLong(3, latency)
        it.execute()
      }
    }
  }

  fun storeNodeStats(remoteAddress: String, id: String, nodeStats: NodeStats) = async {
    ds.connection.use { conn ->
      val stmt =
        conn.prepareStatement(
          "insert into ethstats_nodestats(address, id, gasPrice, hashrate, mining, syncing, active, uptime, peers) values(?,?,?,?,?,?,?,?,?)"
        )
      stmt.use {
        it.setString(1, remoteAddress)
        it.setString(2, id)
        it.setInt(3, nodeStats.gasPrice)
        it.setInt(4, nodeStats.hashrate)
        it.setBoolean(5, nodeStats.mining)
        it.setBoolean(6, nodeStats.syncing)
        it.setBoolean(7, nodeStats.active)
        it.setInt(8, nodeStats.uptime)
        it.setInt(9, nodeStats.peers)
        it.execute()
      }
    }
  }

  fun storePendingTx(remoteAddress: String, id: String, pendingTx: Long) = async {
    ds.connection.use { conn ->
      val stmt =
        conn.prepareStatement(
          "insert into pendingtx(address, id, value) values(?,?,?)"
        )
      stmt.use {
        it.setString(1, remoteAddress)
        it.setString(2, id)
        it.setLong(3, pendingTx)
        it.execute()
      }
    }
  }

  fun updateDisconnect(remoteAddress: String, id: String) = async {
    ds.connection.use { conn ->
      val stmt =
        conn.prepareStatement(
          "update ethstats_peer set disconnect_time=now() where address=? and id=?"
        )
      stmt.use {
        it.setString(1, remoteAddress)
        it.setString(2, id)
        it.execute()
      }
    }
  }

  fun getPeerData(id: String): Deferred<PeerData?> = async {
    return@async ds.connection.use { conn ->
      val stmt =
        conn.prepareStatement(
          "select latency.value from latency where latency.id=? order by latency.createdAt desc limit 1"
        )
      val latency = stmt.use {
        it.setString(1, id)
        val rs = it.executeQuery()
        if (rs.next()) {
          rs.getLong(1)
        } else {
          null
        }
      }
      val pendingtxStmt =
        conn.prepareStatement(
          "select pendingtx.value from pendingtx where pendingtx.id=? order by pendingtx.createdAt desc limit 1"
        )
      val pendingTx = pendingtxStmt.use {
        it.setString(1, id)
        val rs = it.executeQuery()
        if (rs.next()) {
          rs.getLong(1)
        } else {
          null
        }
      }
      val nodeInfoStmt = conn.prepareStatement("select name, node, port, net, protocol, api, os, osVer, client from ethstats_peer where id=? order by createdAt desc limit 1")
      val nodeInfo = nodeInfoStmt.use {
        it.setString(1, id)
        val rs = it.executeQuery()
        if (rs.next()) {
          NodeInfo(
            rs.getString(1),
            rs.getString(2),
            rs.getInt(3),
            rs.getString(4),
            rs.getString(5),
            rs.getString(6),
            rs.getString(7),
            rs.getString(8),
            client = rs.getString(9),
          )
        } else {
          null
        }
      }

      val nodeStatsStmt = conn.prepareStatement(
        "select active, syncing, mining, hashrate, peers, gasPrice, uptime from ethstats_nodestats where id=? order by createdAt desc limit 1"
      )
      val nodeStats = nodeStatsStmt.use {
        it.setString(1, id)
        val rs = it.executeQuery()
        if (rs.next()) {
          NodeStats(
            rs.getBoolean(1),
            rs.getBoolean(2),
            rs.getBoolean(3),
            rs.getInt(4),
            rs.getInt(5),
            rs.getInt(6),
            rs.getInt(7)
          )
        } else {
          null
        }
      }
      PeerData(latency, pendingTx, nodeInfo, nodeStats)
    }
  }
}

data class PeerData(val latency: Long?, val pendingTx: Long?, val nodeInfo: NodeInfo?, val nodeStats: NodeStats?)
