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
package org.apache.tuweni.peer.repository.memory

import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.peer.repository.Connection
import org.apache.tuweni.peer.repository.Identity
import org.apache.tuweni.peer.repository.Peer
import org.apache.tuweni.peer.repository.PeerRepository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Memory-backed peer repository.
 *
 * Useful for proof-of-concept.
 */
class MemoryPeerRepository : PeerRepository {

  private val peerMap = ConcurrentHashMap<String, Peer>()
  private val identities = HashSet<Identity>()
  private val connections = ConcurrentHashMap<String, Connection>()

  override fun storePeer(id: String, lastContacted: Instant?, lastDiscovered: Instant?): Peer {
    val peer = MemoryPeer(id, lastContacted, lastDiscovered)
    peerMap[peer.id()] = peer
    return peer
  }

  override fun randomPeer(): Peer? = peerMap.values.firstOrNull()

  override fun storeIdentity(networkInterface: String, port: Int, keyPair: SECP256K1.KeyPair): Identity {
    val identity = MemoryIdentity(networkInterface, port, keyPair)
    identities.add(identity)
    return identity
  }

  override fun addConnection(peer: Peer, identity: Identity) {
    val now = Instant.now()
    val conn = MemoryConnection(true, peer, identity)
    connections[createConnectionKey(peer, identity)] = conn
    (peer as MemoryPeer).connections.add(conn)
    peer.lastContacted = now
    (identity as MemoryIdentity).connections.add(conn)
  }

  override fun markConnectionInactive(peer: Peer, identity: Identity) {
    (connections[createConnectionKey(peer, identity)] as MemoryConnection).active = false
  }

  private fun createConnectionKey(peer: Peer, identity: Identity): String =
    """${peer.id()}-${identity.keyPair().publicKey().toHexString()}"""
}

internal data class MemoryPeer(
  private val id: String,
  internal var lastContacted: Instant?,
  internal val lastDiscovered: Instant?,
  internal val connections: MutableList<Connection> = mutableListOf()
) : Peer {
  override fun connections(): List<Connection> = connections

  override fun id(): String = id

  override fun lastContacted(): Instant? = lastContacted

  override fun lastDiscovered(): Instant? = lastDiscovered
}

internal data class MemoryConnection(var active: Boolean, val peer: Peer, val identity: Identity) : Connection {
  override fun peer(): Peer = peer

  override fun identity(): Identity = identity

  override fun active(): Boolean = active
}

internal data class MemoryIdentity(
  private val networkInterface: String,
  private val port: Int,
  private val keyPair: SECP256K1.KeyPair,
  internal val connections: MutableList<Connection> = mutableListOf()
) : Identity {
  override fun networkInterface(): String = networkInterface

  override fun port(): Int = port

  override fun keyPair(): SECP256K1.KeyPair = keyPair

  override fun connections(): List<Connection> = connections

  override fun activePeers(): List<Peer> = connections.filter { it.active() }.map { it.peer() }
}
