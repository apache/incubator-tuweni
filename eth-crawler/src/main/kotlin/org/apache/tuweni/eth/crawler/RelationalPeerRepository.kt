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
import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.ExpiringMap
import org.apache.tuweni.concurrent.coroutines.asyncResult
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.Endpoint
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.Peer
import org.apache.tuweni.devp2p.PeerRepository
import org.apache.tuweni.devp2p.eth.Status
import org.apache.tuweni.devp2p.parseEnodeUri
import org.apache.tuweni.rlpx.wire.WireConnection
import org.slf4j.LoggerFactory
import java.net.URI
import java.sql.Timestamp
import java.util.UUID
import javax.sql.DataSource
import kotlin.coroutines.CoroutineContext

open class RelationalPeerRepository(
  private val dataSource: DataSource,
  private val expiration: Long = 5 * 60 * 1000L,
  private val clientIdsInterval: Long = 24 * 60 * 60 * 1000 * 2L,
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : CoroutineScope, PeerRepository {

  companion object {
    internal val logger = LoggerFactory.getLogger(RelationalPeerRepository::class.java)
  }

  private val listeners = mutableListOf<(Peer) -> Unit>()
  private val peerCache = ExpiringMap<SECP256K1.PublicKey, String>()

  override fun addListener(listener: (Peer) -> Unit) {
    listeners.add(listener)
  }

  override suspend fun get(host: String, port: Int, nodeId: SECP256K1.PublicKey): Peer {
    return get(nodeId, Endpoint(host, port))
  }

  fun get(nodeId: SECP256K1.PublicKey, endpoint: Endpoint): Peer {
    val id = peerCache.computeIfAbsent(nodeId, expiration) {
      dataSource.connection.use { conn ->
        logger.trace("Get peer with $nodeId")
        val stmt = conn.prepareStatement("select id,publickey from identity where publickey=?")
        stmt.setBytes(1, nodeId.bytes().toArrayUnsafe())
        val rs = stmt.executeQuery()
        rs.use {
          if (!rs.next()) {
            logger.debug("Creating new peer with public key ${nodeId.toHexString()}")
            val id = UUID.randomUUID().toString()
            val insert = conn.prepareStatement("insert into identity(id, publickey) values(?, ?)")
            insert.setString(1, id)
            insert.setBytes(2, nodeId.bytes().toArrayUnsafe())
            insert.execute()
            id
          } else {
            logger.trace("Found existing peer with public key ${nodeId.toHexString()}")
            val id = rs.getString(1)
            id
          }
        }
      }
    }
    return RepositoryPeer(nodeId, id, endpoint, dataSource)
  }

  override suspend fun get(uri: URI): Peer {
    val (nodeId, endpoint) = parseEnodeUri(uri)
    return get(nodeId, endpoint)
  }

  override fun getAsync(uri: URI): AsyncResult<Peer> {
    return asyncResult { get(uri) }
  }

  override fun getAsync(uri: String): AsyncResult<Peer> {
    return asyncResult { get(uri) }
  }

  fun recordInfo(wireConnection: WireConnection, status: Status?) {
    dataSource.connection.use { conn ->
      val peer = get(
        wireConnection.peerPublicKey(),
        Endpoint(wireConnection.peerHost(), wireConnection.peerPort())
      ) as RepositoryPeer
      val stmt =
        conn.prepareStatement(
          "insert into nodeInfo(id, createdAt, host, port, publickey, p2pVersion, clientId, capabilities, genesisHash, bestHash, totalDifficulty, identity, disconnectReason) values(?,?,?,?,?,?,?,?,?,?,?,?,?)"
        )
      stmt.use {
        val peerHello = wireConnection.peerHello
        it.setString(1, UUID.randomUUID().toString())
        it.setTimestamp(2, Timestamp(System.currentTimeMillis()))
        it.setString(3, wireConnection.peerHost())
        it.setInt(4, wireConnection.peerPort())
        it.setBytes(5, wireConnection.peerPublicKey().bytesArray())
        it.setInt(6, peerHello?.p2pVersion() ?: 0)
        it.setString(7, peerHello?.clientId() ?: "")
        it.setString(8, peerHello?.capabilities()?.joinToString(",") { it.name() + "/" + it.version() } ?: "")
        it.setString(9, status?.genesisHash?.toHexString())
        it.setString(10, status?.bestHash?.toHexString())
        it.setString(11, status?.totalDifficulty?.toHexString())
        it.setString(12, peer.id)
        it.setString(13, wireConnection.disconnectReason.text)

        it.execute()
      }
    }
  }

  internal fun getPeers(infoCollected: Long, from: Int? = null, limit: Int? = null): List<PeerConnectionInfo> {
    dataSource.connection.use { conn ->
      var query = "select distinct nodeinfo.host, nodeinfo.port, nodeinfo.publickey from nodeinfo \n" +
        "  inner join (select id, max(createdAt) as maxCreatedAt from nodeinfo group by id) maxSeen \n" +
        "  on nodeinfo.id = maxSeen.id and nodeinfo.createdAt = maxSeen.maxCreatedAt where createdAt < ?"
      if (from != null && limit != null) {
        query += " limit $limit offset $from"
      }
      val stmt =
        conn.prepareStatement(query)
      stmt.use {
        it.setTimestamp(1, Timestamp(infoCollected))
        // map results.
        val rs = stmt.executeQuery()
        val result = mutableListOf<PeerConnectionInfo>()
        while (rs.next()) {
          val pubkey = SECP256K1.PublicKey.fromBytes(Bytes.wrap(rs.getBytes(3)))
          val port = rs.getInt(2)
          val host = rs.getString(1)
          result.add(PeerConnectionInfo(pubkey, host, port))
        }
        return result
      }
    }
  }

  internal fun getPeersWithInfo(
    infoCollected: Long,
    from: Int? = null,
    limit: Int? = null
  ): List<PeerConnectionInfoDetails> {
    dataSource.connection.use { conn ->
      var query =
        "select distinct nodeinfo.createdAt, nodeinfo.publickey, nodeinfo.p2pversion, nodeinfo.clientId, nodeinfo.capabilities, nodeinfo.genesisHash, nodeinfo.besthash, nodeinfo.totalDifficulty from nodeinfo " +
          "  inner join (select identity, max(createdAt) as maxCreatedAt from nodeinfo group by identity) maxSeen " +
          "  on nodeinfo.identity = maxSeen.identity and nodeinfo.createdAt = maxSeen.maxCreatedAt where createdAt < ? order by nodeInfo.createdAt desc"
      if (from != null && limit != null) {
        query += " limit $limit offset $from"
      }
      val stmt =
        conn.prepareStatement(query)
      stmt.use {
        it.setTimestamp(1, Timestamp(infoCollected))
        // map results.
        val rs = stmt.executeQuery()
        val result = mutableListOf<PeerConnectionInfoDetails>()
        while (rs.next()) {
          val createdAt = rs.getTimestamp(1).toInstant().toEpochMilli()
          val pubkey = SECP256K1.PublicKey.fromBytes(Bytes.wrap(rs.getBytes(2)))
          val p2pVersion = rs.getInt(3)
          val clientId = rs.getString(4) ?: ""
          val capabilities = rs.getString(5) ?: ""
          val genesisHash = rs.getString(6) ?: ""
          val bestHash = rs.getString(7) ?: ""
          val totalDifficulty = rs.getString(8) ?: ""
          result.add(
            PeerConnectionInfoDetails(
              createdAt,
              pubkey,
              p2pVersion,
              clientId,
              capabilities,
              genesisHash,
              bestHash,
              totalDifficulty
            )
          )
        }
        return result
      }
    }
  }

  internal fun getPendingPeers(from: Int = 0, limit: Int = 100): List<PeerConnectionInfo> {
    dataSource.connection.use { conn ->
      val stmt =
        conn.prepareStatement(
          "select endpoint.host, endpoint.port, identity.publickey from endpoint inner " +
            "join identity on (endpoint.identity = identity.id) where endpoint.identity NOT IN (select identity from nodeinfo) order by endpoint.lastSeen desc limit $limit offset $from"
        )
      stmt.use {
        // map results.
        val rs = stmt.executeQuery()
        val result = mutableListOf<PeerConnectionInfo>()
        while (rs.next()) {
          val pubkey = SECP256K1.PublicKey.fromBytes(Bytes.wrap(rs.getBytes(3)))
          val port = rs.getInt(2)
          val host = rs.getString(1)
          result.add(PeerConnectionInfo(pubkey, host, port))
        }
        return result
      }
    }
  }

  internal fun getClientIdsInternal(): List<ClientInfo> {
    dataSource.connection.use { conn ->
      val sql =
        "select clients.clientId, count(clients.clientId) from (select nodeinfo.clientId, nodeInfo.createdAt from nodeinfo inner join (select identity, max(createdAt) as maxCreatedAt from nodeinfo group by identity) maxSeen on nodeinfo.identity = maxSeen.identity and nodeinfo.createdAt = maxSeen.maxCreatedAt) as clients where clients.createdAt > ? group by clients.clientId"
      val stmt =
        conn.prepareStatement(sql)
      stmt.use {
        val time = System.currentTimeMillis() - clientIdsInterval
        logger.info("Logging client ids query: $sql with $time")
        it.setTimestamp(1, Timestamp(time))
        // map results.
        val rs = stmt.executeQuery()
        val result = mutableListOf<ClientInfo>()
        while (rs.next()) {
          val clientId = rs.getString(1)
          val count = rs.getInt(2)
          result.add(ClientInfo(clientId, count))
        }
        return result
      }
    }
  }

  internal fun getPeerWithInfo(infoCollected: Long, publicKey: String): PeerConnectionInfoDetails? {
    dataSource.connection.use { conn ->
      var query =
        "select distinct nodeinfo.createdAt, nodeinfo.publickey, nodeinfo.p2pversion, nodeinfo.clientId, nodeinfo.capabilities, nodeinfo.genesisHash, nodeinfo.besthash, nodeinfo.totalDifficulty from nodeinfo " +
          "  inner join (select identity, max(createdAt) as maxCreatedAt from nodeinfo group by identity) maxSeen " +
          "  on nodeinfo.identity = maxSeen.identity and nodeinfo.createdAt = maxSeen.maxCreatedAt where createdAt < ? and nodeinfo.publickey = ? order by nodeInfo.createdAt desc"
      val stmt =
        conn.prepareStatement(query)
      stmt.use {
        it.setTimestamp(1, Timestamp(infoCollected))
        it.setBytes(2, Bytes.fromHexString(publicKey).toArrayUnsafe())
        // map results.
        val rs = stmt.executeQuery()
        if (rs.next()) {
          val createdAt = rs.getTimestamp(1).toInstant().toEpochMilli()
          val pubkey = SECP256K1.PublicKey.fromBytes(Bytes.wrap(rs.getBytes(2)))
          val p2pVersion = rs.getInt(3)
          val clientId = rs.getString(4) ?: ""
          val capabilities = rs.getString(5) ?: ""
          val genesisHash = rs.getString(6) ?: ""
          val bestHash = rs.getString(7) ?: ""
          val totalDifficulty = rs.getString(8) ?: ""
          return PeerConnectionInfoDetails(
            createdAt,
            pubkey,
            p2pVersion,
            clientId,
            capabilities,
            genesisHash,
            bestHash,
            totalDifficulty
          )
        } else {
          return null
        }
      }
    }
  }
}

internal data class ClientReadyStats(val total: Int, val ready: Int)
internal data class ClientInfo(val clientId: String, val count: Int)
internal data class PeerConnectionInfo(val nodeId: SECP256K1.PublicKey, val host: String, val port: Int)
internal data class PeerConnectionInfoDetails(
  val createdAt: Long,
  val nodeId: SECP256K1.PublicKey,
  val p2pVersion: Int,
  val clientId: String,
  val capabilities: String,
  val genesisHash: String,
  val bestHash: String,
  val totalDifficulty: String
)

internal class RepositoryPeer(
  override val nodeId: SECP256K1.PublicKey,
  val id: String,
  knownEndpoint: Endpoint,
  private val dataSource: DataSource
) : Peer {

  init {
    dataSource.connection.use {
      val stmt = it.prepareStatement("select lastSeen,lastVerified,host,port from endpoint where identity=?")
      stmt.use {
        it.setString(1, id)
        val rs = it.executeQuery()
        if (rs.next()) {
          val lastSeenStored = rs.getTimestamp(1)
          val lastVerifiedStored = rs.getTimestamp(2)
          val host = rs.getString(3)
          val port = rs.getInt(4)
          if (knownEndpoint.address == host && knownEndpoint.udpPort == port) {
            lastSeen = lastSeenStored.time
            lastVerified = lastVerifiedStored.time
          }
        }
      }
    }
  }

  @Volatile
  override var endpoint: Endpoint = knownEndpoint

  override var enr: EthereumNodeRecord? = null

  @Synchronized
  override fun getEndpoint(ifVerifiedOnOrAfter: Long): Endpoint? {
    if ((lastVerified ?: 0) >= ifVerifiedOnOrAfter) {
      return this.endpoint
    }
    return null
  }

  @Volatile
  override var lastVerified: Long? = null

  @Volatile
  override var lastSeen: Long? = null

  @Synchronized
  override fun updateEndpoint(endpoint: Endpoint, time: Long, ifVerifiedBefore: Long?): Endpoint {
    val currentEndpoint = this.endpoint
    if (currentEndpoint == endpoint) {
      this.seenAt(time)
      return currentEndpoint
    }

    if (ifVerifiedBefore == null || (lastVerified ?: 0) < ifVerifiedBefore) {
      if (currentEndpoint.address != endpoint.address || currentEndpoint.udpPort != endpoint.udpPort) {
        lastVerified = null
      }
      this.endpoint = endpoint
      this.seenAt(time)
      return endpoint
    }

    return currentEndpoint
  }

  @Synchronized
  override fun verifyEndpoint(endpoint: Endpoint, time: Long): Boolean {
    if (endpoint != this.endpoint) {
      return false
    }
    if ((lastVerified ?: 0) < time) {
      lastVerified = time
    }
    seenAt(time)
    return true
  }

  @Synchronized
  override fun seenAt(time: Long) {
    if ((lastSeen ?: 0) < time) {
      lastSeen = time
      persist()
    }
  }

  @Synchronized
  override fun updateENR(record: EthereumNodeRecord, time: Long) {
    if (enr == null || enr!!.seq() < record.seq()) {
      enr = record
      updateEndpoint(Endpoint(record.ip().hostAddress, record.udp()!!, record.tcp()), time)
    }
  }

  fun persist() {
    dataSource.connection.use { conn ->
      val stmt =
        conn.prepareStatement(
          "insert into endpoint(id, lastSeen, lastVerified, host, port, identity) values(?,?,?,?,?,?)"
        )
      stmt.use {
        it.setString(1, UUID.randomUUID().toString())
        it.setTimestamp(2, Timestamp(lastSeen ?: 0))
        it.setTimestamp(3, Timestamp(lastVerified ?: 0))
        it.setString(4, endpoint.address)
        it.setInt(5, endpoint.udpPort)
        it.setString(6, id)
        it.execute()
      }
    }
  }
}
