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

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.util.concurrent.UncheckedExecutionException
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.kademlia.KademliaRoutingTable
import java.lang.IllegalArgumentException

/**
 * A routing table for ÐΞVp2p peers.
 */
interface PeerRoutingTable : Set<Peer> {

  /**
   * Return the nearest nodes to a target id, in order from closest to furthest.
   *
   * The sort order is the log distance from the target to the node-id's of the Peers.
   *
   * @param targetId the id to find nodes nearest to
   * @param limit the maximum number of nodes to return
   * @return a list of nodes from the routing table
   */
  fun nearest(targetId: SECP256K1.PublicKey, limit: Int): List<Peer>

  /**
   * Add a node to the table.
   *
   * @param node the node to add
   * @return `null` if the node was successfully added to the table (or already in the table). Otherwise, a node
   *         will be returned that is a suitable candidate for eviction, and the provided node will be stored in
   *         the replacements list.
   */
  fun add(node: Peer): Peer?

  /**
   * Remove a node from the table, potentially adding an alternative from the replacement cache.
   *
   * @param node the node to evict
   * @param `true` if the node was removed
   */
  fun evict(node: Peer): Boolean
}

internal const val DEVP2P_BUCKET_SIZE = 16

/**
 * A Peer routing table for the Ethereum ÐΞVp2p network.
 *
 * This is an implementation of a [KademliaRoutingTable] using keccak256 hashed node ids and a k-bucket size of 6.
 *
 * @constructor Create a new ÐΞVp2p routing table.
 * @param selfId the ID of the local node
 */
internal class DevP2PPeerRoutingTable(selfId: SECP256K1.PublicKey) : PeerRoutingTable {

  private val idHashCache: Cache<SECP256K1.PublicKey, ByteArray> =
    CacheBuilder.newBuilder().maximumSize((1L + 256) * 16).weakKeys().build()

  private val table = KademliaRoutingTable<Peer>(
    selfId = hashForId(selfId)!!,
    k = DEVP2P_BUCKET_SIZE,
    nodeId = { p -> hashForId(p.nodeId)!! }
  )

  override val size: Int
    get() = table.size

  override fun contains(element: Peer): Boolean = table.contains(element)

  override fun containsAll(elements: Collection<Peer>): Boolean = table.containsAll(elements)

  override fun isEmpty(): Boolean = table.isEmpty()

  override fun iterator(): Iterator<Peer> = table.iterator()

  override fun nearest(targetId: SECP256K1.PublicKey, limit: Int): List<Peer> =
    hashForId(targetId)?.let { table.nearest(it, limit) } ?: listOf()

  override fun add(node: Peer): Peer? = table.add(node)

  override fun evict(node: Peer): Boolean = table.evict(node)

  private fun hashForId(id: SECP256K1.PublicKey): ByteArray? {
    try {
      return idHashCache.get(id) { EthereumNodeRecord.nodeId(id).toArrayUnsafe() }
    } catch (e: UncheckedExecutionException) {
      if (e.cause is IllegalArgumentException) {
        return null
      }
      throw e
    }
  }
}
