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

import org.apache.tuweni.devp2p.eth.Status
import org.apache.tuweni.peer.repository.Identity
import org.apache.tuweni.rlpx.WireConnectionRepository
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier
import org.apache.tuweni.rlpx.wire.WireConnection
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Class bridging the subprotocols and the connection repository of the RLPx service, updating the Ethereum peer repository with information.
 */
class WireConnectionPeerRepositoryAdapter(val peerRepository: EthereumPeerRepository) : WireConnectionRepository {

  companion object {
    val logger = LoggerFactory.getLogger(WireConnectionPeerRepositoryAdapter::class.java)
  }

  private val wireConnectionToIdentities = ConcurrentHashMap<String, Identity>()
  private val connections = ConcurrentHashMap<String, WireConnection>()
  private val connectionListeners = ArrayList<WireConnectionRepository.Listener>()

  private val disconnectionListeners = ArrayList<WireConnectionRepository.Listener>()

  override fun addConnectionListener(listener: WireConnectionRepository.Listener) {
    connectionListeners.add(listener)
  }

  override fun addDisconnectionListener(listener: WireConnectionRepository.Listener) {
    disconnectionListeners.add(listener)
  }

  override fun add(wireConnection: WireConnection): String {
    val id =
      peerRepository.storeIdentity(wireConnection.peerHost(), wireConnection.peerPort(), wireConnection.peerPublicKey())

    val peer = peerRepository.storePeer(id, Instant.now(), Instant.now())
    peerRepository.addConnection(peer, id)
    connections[id.id()] = wireConnection
    wireConnectionToIdentities[wireConnection.uri()] = peer.id()
    wireConnection.registerListener {
      if (it == WireConnection.Event.CONNECTED) {
        for (listener in connectionListeners) {
          listener.connectionEvent(wireConnection)
        }
      } else if (it == WireConnection.Event.DISCONNECTED) {
        for (listener in disconnectionListeners) {
          listener.connectionEvent(wireConnection)
        }
        peerRepository.markConnectionInactive(peer, id)
      }
    }
    return id.id()
  }

  override fun get(id: String): WireConnection? = connections[id]

  override fun asIterable(): Iterable<WireConnection> = connections.values

  override fun asIterable(identifier: SubProtocolIdentifier): Iterable<WireConnection> =
    connections.values.filter { conn -> conn.supports(identifier) }

  override fun close() {
    connections.clear()
    wireConnectionToIdentities.clear()
  }

  fun get(ethereumConnection: EthereumConnection): WireConnection {
    val conn = connections[ethereumConnection.identity().id()]
    if (conn == null) {
      logger.info("Connection ${ethereumConnection.identity().id()} not found, present are: ${connections.keys.joinToString(",")}}")
      throw NoSuchElementException("No connection available")
    }
    return conn
  }

  fun listenToStatus(conn: WireConnection, status: Status) {
    wireConnectionToIdentities[conn.uri()]?.let { peerRepository.storeStatus(it, status) }
  }
}
