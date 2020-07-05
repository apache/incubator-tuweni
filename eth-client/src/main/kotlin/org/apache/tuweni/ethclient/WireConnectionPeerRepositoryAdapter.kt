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

import org.apache.tuweni.peer.repository.PeerRepository
import org.apache.tuweni.rlpx.WireConnectionRepository
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier
import org.apache.tuweni.rlpx.wire.WireConnection
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class WireConnectionPeerRepositoryAdapter(val peerRepository: PeerRepository) : WireConnectionRepository {

  private val connections = ConcurrentHashMap<String, WireConnection>()

  override fun add(wireConnection: WireConnection): String {
    val id =
      peerRepository.storeIdentity(wireConnection.peerHost(), wireConnection.peerPort(), wireConnection.peerPublicKey())
    val peer = peerRepository.storePeer(id, Instant.now(), Instant.now())
    peerRepository.addConnection(peer, id)
    connections[id.id()] = wireConnection
    return id.id()
  }

  override fun get(id: String): WireConnection? = connections[id]

  override fun asIterable(): Iterable<WireConnection> = connections.values

  override fun asIterable(identifier: SubProtocolIdentifier): Iterable<WireConnection> =
    connections.values.filter { conn -> conn.supports(identifier) }

  override fun close() {
    connections.clear()
  }
}
