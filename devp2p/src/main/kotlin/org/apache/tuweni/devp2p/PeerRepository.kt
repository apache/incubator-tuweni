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
package org.apache.tuweni.devp2p

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.asyncResult
import org.apache.tuweni.crypto.SECP256K1
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * A repository of peers in an Ethereum network.
 *
 * Conceptually, this repository stores information about <i>all</i> peers in an Ethereum network, hence the
 * retrieval methods always return a valid [Peer]. However, the [Peer] objects are only generated on demand and
 * may be purged from underlying storage if they can be recreated easily.
 */
interface PeerRepository {

  /**
   * Adds a listener to the repository, which will consume peer entries whenever they are added to the repository.
   */
  fun addListener(listener: (Peer) -> Unit)

  /**
   *  Get a Peer based on a URI components.
   *
   * The returned peer will use the endpoint from the URI, unless the peer is already active, in
   * which case its endpoint will be unchanged.
   *
   * @param host the peer host
   * @param port the peer port
   * @param nodeId the public key associated with the peer
   */
  suspend fun get(host: String, port: Int, nodeId: SECP256K1.PublicKey): Peer

  /**
   * Get a Peer based on a URI.
   *
   * The returned peer will use the endpoint from the URI, unless the peer is already active, in
   * which case its endpoint will be unchanged.
   *
   * @param uri the enode URI
   * @return the peer
   * @throws IllegalArgumentException if the URI is not a valid enode URI
   */
  suspend fun get(uri: URI): Peer

  /**
   * Get a Peer based on a URI.
   *
   * The returned peer will use the endpoint from the URI, unless the peer is already active, in
   * which case its endpoint will be unchanged.
   *
   * @param uri the enode URI
   * @return the peer
   * @throws IllegalArgumentException if the URI is not a valid enode URI
   */
  fun getAsync(uri: URI): AsyncResult<Peer>

  /**
   * Get a Peer based on a URI string.
   *
   * The returned peer will use the endpoint from the URI, unless the peer is already active, in
   * which case its endpoint will be unchanged.
   *
   * @param uri the enode URI
   * @return the peer
   * @throws IllegalArgumentException if the URI is not a valid enode URI
   */
  suspend fun get(uri: String) = get(URI.create(uri))

  /**
   * Get a Peer based on a URI string.
   *
   * The returned peer will use the endpoint from the URI, unless the peer is already active, in
   * which case its endpoint will be unchanged.
   *
   * @param uri the enode URI
   * @return the peer
   * @throws IllegalArgumentException if the URI is not a valid enode URI
   */
  fun getAsync(uri: String): AsyncResult<Peer>
}

/**
 * An in-memory peer repository.
 *
 * Note: as the storage is in-memory, no retrieval methods in this implementation will suspend.
 */
class EphemeralPeerRepository(
  private val peers: MutableMap<SECP256K1.PublicKey, Peer> = ConcurrentHashMap(),
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) :
  PeerRepository, CoroutineScope {

  private val listeners = mutableListOf<(Peer) -> Unit>()

  override fun addListener(listener: (Peer) -> Unit) {
    listeners.add(listener)
  }

  /**
   * Get a peer from node ID and endpoint information
   * @param nodeId the peer public key
   * @param endpoint the peer endpoint
   * @return the peer
   */
  fun get(nodeId: SECP256K1.PublicKey, endpoint: Endpoint) =
    peers.compute(nodeId) { _, peer ->
      if (peer == null) {
        val newPeer = EphemeralPeer(nodeId, endpoint)
        listeners.let {
          for (listener in listeners) {
            listener(newPeer)
          }
        }
        newPeer
      } else {
        peer
      }
    } as Peer

  override suspend fun get(host: String, port: Int, nodeId: SECP256K1.PublicKey): Peer {
    return get(nodeId, Endpoint(host, port))
  }

  override suspend fun get(uri: URI): Peer {
    val (nodeId, endpoint) = parseEnodeUri(uri)
    return get(nodeId, endpoint)
  }

  override fun getAsync(uri: URI): AsyncResult<Peer> = asyncResult { get(uri) }

  override fun getAsync(uri: String): AsyncResult<Peer> = asyncResult { get(uri) }

  private inner class EphemeralPeer(
    override val nodeId: SECP256K1.PublicKey,
    knownEndpoint: Endpoint
  ) : Peer {
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
      seenAt(time)
      if ((lastVerified ?: 0) < time) {
        lastVerified = time
      }
      return true
    }

    @Synchronized
    override fun seenAt(time: Long) {
      if ((lastSeen ?: 0) < time) {
        lastSeen = time
      }
    }

    @Synchronized
    override fun updateENR(record: EthereumNodeRecord, time: Long) {
      if (enr == null || enr!!.seq() < record.seq()) {
        enr = record
        updateEndpoint(Endpoint(record.ip().hostAddress, record.udp()!!, record.tcp()), time)
      }
    }
  }
}
