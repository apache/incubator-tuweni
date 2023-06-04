// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
 */
class MemoryPeerRepository : PeerRepository {

  val peerMap = ConcurrentHashMap<Identity, Peer>()
  val identities = HashSet<Identity>()
  val connections = ConcurrentHashMap<String, Connection>()

  override fun storePeer(id: Identity, lastContacted: Instant?, lastDiscovered: Instant?): Peer {
    val peer = MemoryPeer(id, lastContacted, lastDiscovered)
    peerMap[peer.id()] = peer
    return peer
  }

  override fun randomPeer(): Peer? = peerMap.values.firstOrNull()

  override fun storeIdentity(networkInterface: String, port: Int, publicKey: SECP256K1.PublicKey): Identity {
    val identity = MemoryIdentity(networkInterface, port, publicKey)
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

  override fun peerDiscoveredAt(peer: Peer, time: Long) {
    val timestamp = Instant.ofEpochMilli(time)
    val lastDiscovered = peer.lastDiscovered()
    if (lastDiscovered == null || lastDiscovered.isBefore(timestamp)) {
      (peer as MemoryPeer).lastDiscovered = timestamp
    }
  }

  private fun createConnectionKey(peer: Peer, identity: Identity): String =
    """${peer.id()}-${identity.publicKey().toHexString()}"""
}

internal data class MemoryPeer(
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

internal data class MemoryConnection(var active: Boolean, val peer: Peer, val identity: Identity) : Connection {
  override fun peer(): Peer = peer

  override fun identity(): Identity = identity

  override fun active(): Boolean = active
}

internal data class MemoryIdentity(
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
