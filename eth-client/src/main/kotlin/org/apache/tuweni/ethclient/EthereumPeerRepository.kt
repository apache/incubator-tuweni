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
package org.apache.tuweni.ethclient

import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.eth.Status
import org.apache.tuweni.peer.repository.Connection
import org.apache.tuweni.peer.repository.Identity
import org.apache.tuweni.peer.repository.Peer
import org.apache.tuweni.peer.repository.PeerRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream

/**
 * A peer repository of peers implementing the eth subprotocol.
 */
interface EthereumPeerRepository : PeerRepository {
  /**
   * Stores the status message sent for a connection
   * @param peerIdentity the peer identity
   * @param status the status message
   */
  fun storeStatus(peerIdentity: Identity, status: Status)

  /**
   * Provides a stream of active connections.
   *
   * @return a stream of active connections
   */
  fun activeConnections(): Stream<EthereumConnection>

  /**
   * Adds a listener to be called when a status message is received
   * @param statusListener the listener
   * @return a listener ID
   */
  fun addStatusListener(statusListener: (EthereumConnection) -> Unit): String

  /**
   * Removes a status listener
   * @param id the listener identifier
   */
  fun removeStatusListener(id: String)

  /**
   * Adds a listener to be called when a new peer connects
   * @param identityListener the listener
   * @return a listener ID
   */
  fun addIdentityListener(identityListener: (Identity) -> Unit): String

  /**
   * Removes an identity listener
   * @param id the listener identifier
   */
  fun removeIdentityListener(id: String)
}

interface EthereumConnection : Connection {
  fun status(): Status?
}

/**
 * Memory-backed Ethereum peer repository.
 *
 */
class MemoryEthereumPeerRepository : EthereumPeerRepository {

  val peerMap = ConcurrentHashMap<Identity, Peer>()
  val identities = HashSet<Identity>()
  val connections = ConcurrentHashMap<String, EthereumConnection>()
  val statusListeners = HashMap<String, (EthereumConnection) -> Unit>()
  val identityListeners = HashMap<String, (Identity) -> Unit>()

  override fun storeStatus(peerIdentity: Identity, status: Status) {
    val connKey = peerMap[peerIdentity]?.let { createConnectionKey(it, peerIdentity) }
    connections[connKey]?.let { conn ->
      (conn as MemoryEthereumConnection).status = status
      statusListeners.values.forEach {
        it(conn)
      }
    }
  }

  override fun addStatusListener(statusListener: (EthereumConnection) -> Unit): String {
    val id = UUID.randomUUID().toString()
    statusListeners.put(id, statusListener)
    return id
  }

  override fun removeStatusListener(id: String) {
    statusListeners.remove(id)
  }

  override fun addIdentityListener(identityListener: (Identity) -> Unit): String {
    val id = UUID.randomUUID().toString()
    identityListeners.put(id, identityListener)
    return id
  }

  override fun removeIdentityListener(id: String) {
    identityListeners.remove(id)
  }

  override fun activeConnections(): Stream<EthereumConnection> {
    return connections.values.stream().filter { it.active() }
  }

  override fun storePeer(id: Identity, lastContacted: Instant?, lastDiscovered: Instant?): Peer {
    val peer = MemoryEthereumPeer(id, lastContacted, lastDiscovered)
    peerMap[peer.id()] = peer
    return peer
  }

  override fun randomPeer(): Peer? = peerMap.values.firstOrNull()

  override fun storeIdentity(networkInterface: String, port: Int, publicKey: SECP256K1.PublicKey): Identity {
    val identity = MemoryEthereumIdentity(networkInterface, port, publicKey)
    if (identities.add(identity)) {
      identityListeners.values.forEach {
        it(identity)
      }
    }
    return identity
  }

  override fun addConnection(peer: Peer, identity: Identity) {
    val now = Instant.now()
    val conn = MemoryEthereumConnection(true, peer, identity)
    connections[createConnectionKey(peer, identity)] = conn
    (peer as MemoryEthereumPeer).connections.add(conn)
    peer.lastContacted = now
    (identity as MemoryEthereumIdentity).connections.add(conn)
  }

  override fun markConnectionInactive(peer: Peer, identity: Identity) {
    (connections[createConnectionKey(peer, identity)] as MemoryEthereumConnection).active = false
  }

  override fun peerDiscoveredAt(peer: Peer, time: Long) {
    val timestamp = Instant.ofEpochMilli(time)
    val lastDiscovered = peer.lastDiscovered()
    if (lastDiscovered == null || lastDiscovered.isBefore(timestamp)) {
      (peer as MemoryEthereumPeer).lastDiscovered = timestamp
    }
  }

  private fun createConnectionKey(peer: Peer, identity: Identity): String =
    """${peer.id()}-${identity.publicKey().toHexString()}"""
}

internal data class MemoryEthereumPeer(
  private val id: Identity,
  internal var lastContacted: Instant?,
  internal var lastDiscovered: Instant?
) : Peer {
  val connections: MutableList<Connection> = mutableListOf()

  override fun connections(): List<Connection> = connections

  override fun id(): Identity = id

  override fun lastContacted(): Instant? = lastContacted

  override fun lastDiscovered(): Instant? = lastDiscovered
}

internal data class MemoryEthereumConnection(
  var active: Boolean,
  val peer: Peer,
  val identity: Identity,
  var status: Status? = null
) : EthereumConnection {
  override fun peer(): Peer = peer

  override fun identity(): Identity = identity

  override fun active(): Boolean = active

  override fun status(): Status? = status
}

internal data class MemoryEthereumIdentity(
  private val networkInterface: String,
  private val port: Int,
  private val publicKey: SECP256K1.PublicKey
) : Identity {

  internal val connections: MutableList<Connection> = mutableListOf()

  override fun networkInterface(): String = networkInterface

  override fun port(): Int = port

  override fun publicKey(): SECP256K1.PublicKey = publicKey

  override fun id(): String = publicKey.toHexString() + "@" + networkInterface + ":" + port

  override fun connections(): List<Connection> = connections

  override fun activePeers(): List<Peer> = connections.filter { it.active() }.map { it.peer() }
}
