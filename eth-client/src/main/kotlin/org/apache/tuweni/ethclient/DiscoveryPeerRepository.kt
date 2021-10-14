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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.asyncResult
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.Endpoint
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.Peer
import org.apache.tuweni.devp2p.PeerRepository
import org.apache.tuweni.devp2p.parseEnodeUri
import java.net.URI
import java.time.Instant
import java.util.Objects
import kotlin.coroutines.CoroutineContext

class DiscoveryPeerRepository(private val repository: org.apache.tuweni.peer.repository.PeerRepository) :
  PeerRepository, CoroutineScope {
  override val coroutineContext: CoroutineContext = Dispatchers.Default

  override fun addListener(listener: (Peer) -> Unit) {
    TODO("Unsupported")
  }

  override suspend fun get(host: String, port: Int, nodeId: SECP256K1.PublicKey): Peer {
    val identity = repository.storeIdentity(host, port, nodeId)
    val peer = repository.storePeer(identity, null, Instant.now())
    return DelegatePeer(repository, peer)
  }

  override suspend fun get(uri: URI): Peer {
    val (nodeId, endpoint) = parseEnodeUri(uri)
    return get(endpoint.address, endpoint.udpPort, nodeId)
  }

  override fun getAsync(uri: URI): AsyncResult<Peer> = asyncResult { get(uri) }

  override fun getAsync(uri: String): AsyncResult<Peer> = asyncResult { get(uri) }
}

internal class DelegatePeer(
  val repository: org.apache.tuweni.peer.repository.PeerRepository,
  val peer: org.apache.tuweni.peer.repository.Peer
) : Peer {
  override val nodeId: SECP256K1.PublicKey
    get() = peer.id().publicKey()
  override val endpoint: Endpoint
    get() = Endpoint(peer.id().networkInterface(), peer.id().port())
  override val enr: EthereumNodeRecord?
    get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.
  override val lastVerified: Long?
    get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.
  override val lastSeen: Long?
    get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.

  override fun getEndpoint(ifVerifiedOnOrAfter: Long): Endpoint? {
    TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
  }

  override fun updateEndpoint(endpoint: Endpoint, time: Long, ifVerifiedBefore: Long?): Endpoint {
    TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
  }

  override fun verifyEndpoint(endpoint: Endpoint, time: Long): Boolean {
    TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
  }

  override fun seenAt(time: Long) {
    repository.peerDiscoveredAt(peer, time)
  }

  override fun updateENR(record: EthereumNodeRecord, time: Long) {
    TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
  }

  override fun hashCode(): Int = Objects.hashCode(peer)

  override fun equals(other: Any?): Boolean {
    return other is Peer && Objects.equals(other.nodeId, nodeId) && Objects.equals(other.endpoint, endpoint)
  }
}
