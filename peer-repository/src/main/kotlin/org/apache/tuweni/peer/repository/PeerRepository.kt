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
