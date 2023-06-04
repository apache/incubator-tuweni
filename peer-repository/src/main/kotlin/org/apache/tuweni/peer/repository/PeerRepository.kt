// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.peer.repository

import org.apache.tuweni.crypto.SECP256K1
import java.time.Instant

/**
 * A repository of peers, organized for Ethereum and other blockchain and peer-to-peer systems.
 */
interface PeerRepository {

  fun storePeer(id: Identity, lastContacted: Instant?, lastDiscovered: Instant?): Peer

  fun randomPeer(): Peer?

  fun storeIdentity(networkInterface: String, port: Int, publicKey: SECP256K1.PublicKey): Identity

  fun addConnection(peer: Peer, identity: Identity)

  fun markConnectionInactive(peer: Peer, identity: Identity)
  fun peerDiscoveredAt(peer: Peer, time: Long)
}

/**
 * A peer in a peer-to-peer system.
 */
interface Peer {
  fun connections(): List<Connection>

  fun id(): Identity

  fun lastContacted(): Instant?

  fun lastDiscovered(): Instant?
}

/**
 * Connections of a peer. Can be a past connection.
 */
interface Connection {
  fun active(): Boolean
  fun peer(): Peer
  fun identity(): Identity
}

interface Identity {
  fun networkInterface(): String
  fun port(): Int
  fun publicKey(): SECP256K1.PublicKey
  fun id(): String
  fun connections(): List<Connection>
  fun activePeers(): List<Peer>
}
