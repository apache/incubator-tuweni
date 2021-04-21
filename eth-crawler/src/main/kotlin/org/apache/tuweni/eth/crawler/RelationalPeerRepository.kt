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
import org.apache.tuweni.concurrent.coroutines.asyncResult
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.Endpoint
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.Peer
import org.apache.tuweni.devp2p.PeerRepository
import org.apache.tuweni.devp2p.parseEnodeUri
import java.net.URI
import java.sql.Timestamp
import java.util.UUID
import javax.sql.DataSource
import kotlin.coroutines.CoroutineContext

class RelationalPeerRepository(
  private val dataSource: DataSource,
  override val coroutineContext: CoroutineContext = Dispatchers.Default,
) : CoroutineScope, PeerRepository {

  private val listeners = mutableListOf<(Peer) -> Unit>()

  override fun addListener(listener: (Peer) -> Unit) {
    listeners.add(listener)
  }

  override suspend fun get(host: String, port: Int, nodeId: SECP256K1.PublicKey): Peer {
    return get(nodeId, Endpoint(host, port))
  }

  fun get(nodeId: SECP256K1.PublicKey, endpoint: Endpoint): Peer {
    dataSource.connection.use { conn ->
      val stmt = conn.prepareStatement("select id,publickey from identity where publickey=?")
      stmt.setString(1, nodeId.bytes().toUnprefixedHexString())
      val rs = stmt.executeQuery()
      rs.use {
        if (!rs.next()) {
          val id = UUID.randomUUID().toString()
          val insert = conn.prepareStatement("insert into identity(id, publickey) values(?, ?)")
          insert.setString(1, id)
          insert.setBytes(2, nodeId.bytes().toArrayUnsafe())
          insert.execute()
          val newPeer = RepositoryPeer(nodeId, id, endpoint, dataSource)
          listeners.let {
            for (listener in listeners) {
              listener(newPeer)
            }
          }
          return newPeer
        } else {
          val id = rs.getString(1)
          val pubKey = rs.getBytes(2)
          return RepositoryPeer(SECP256K1.PublicKey.fromBytes(Bytes.wrap(pubKey)), id, endpoint, dataSource)
        }
      }
    }
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
}

internal class RepositoryPeer(
  override val nodeId: SECP256K1.PublicKey,
  val id: String,
  knownEndpoint: Endpoint,
  private val dataSource: DataSource,
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
