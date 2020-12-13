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
package org.apache.tuweni.kademlia

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import java.util.Collections
import java.util.function.Function

/**
 * Determine the XOR-distance between this and an equal-length byte array.
 *
 * @param other the byte-array to calculate the distance to
 * @return the distance as an integer
 * @throws IllegalArgumentException if [other] is not the same length as this
 */
infix fun ByteArray.xorDist(other: ByteArray): Int {
  require(size == other.size) { "arrays are of different lengths" }
  var distance = size * 8
  for (i in indices) {
    val xor = (this[i].toInt() xor other[i].toInt()) and 0xff
    if (xor == 0) {
      distance -= 8
    } else {
      distance -= (Integer.numberOfLeadingZeros(xor) - 24)
      break
    }
  }
  return distance
}

/**
 * Compare two equal-length byte arrays for their XOR-distance to this.
 *
 * @param a the first byte array
 * @param b the second byte array
 * @return -1 if [a] is closer, +1 if [b] is closer, or 0 if they are the same distance to this
 * @throws IllegalArgumentException if [a] or [b] are not the same length as this
 */
fun ByteArray.xorDistCmp(a: ByteArray, b: ByteArray): Int {
  require(size == a.size && size == b.size) { "arrays are of different lengths" }
  for (i in indices) {
    val distA = (this[i].toInt() xor a[i].toInt()) and 0xff
    val distB = (this[i].toInt() xor b[i].toInt()) and 0xff
    if (distA > distB) {
      return 1
    } else if (distA < distB) {
      return -1
    }
  }
  return 0
}

/**
 * Insert an element into a mutable list, based on a comparison function.
 *
 * @param element the element to insert
 * @param comparison the comparison function
 */
fun <E> MutableList<E>.orderedInsert(element: E, comparison: (E, E) -> Int) {
  var i = this.binarySearch { e -> comparison(e, element) }
  if (i < 0) {
    i = -i - 1
  }
  this.add(i, element)
}

/**
 * A Kademlia Routing Table
 *
 * @constructor Create a new routing table.
 * @param selfId the ID of the local node
 * @param k the size of each bucket (k value)
 * @param maxReplacements the maximum number of replacements to cache in each bucket
 * @param nodeId a function for obtaining the id of a network node
 * @param <K> the network node type
 *
 */
class KademliaRoutingTable<T>(
  private val selfId: ByteArray,
  k: Int,
  maxReplacements: Int = k,
  private val nodeId: (T) -> ByteArray,
  private val distanceToSelf: (T) -> Int = { nodeId(it) xorDist selfId }
) : Set<T> {

  companion object {
    /**
     * Create a new routing table.
     *
     * @param selfId the ID of the local node
     * @param k the size of each bucket (k value)
     * @param nodeId a function for obtaining the id of a network node
     * @param <K> the network node type
     * @return A new routing table
     */
    @JvmStatic
    fun <T> create(
      selfId: ByteArray,
      k: Int,
      nodeId: Function<T, ByteArray>,
      distanceToSelf: Function<T, Int>
    ): KademliaRoutingTable<T> =
      KademliaRoutingTable(selfId, k, nodeId = nodeId::apply, distanceToSelf = distanceToSelf::apply)

    /**
     * Create a new routing table.
     *
     * @param selfId the ID of the local node
     * @param k the size of each bucket (k value)
     * @param maxReplacements the maximum number of replacements to cache in each bucket
     * @param nodeId a function for obtaining the id of a network node
     * @param <K> the network node type
     * @return A new routing table
     */
    @JvmStatic
    fun <T> create(
      selfId: ByteArray,
      k: Int,
      maxReplacements: Int,
      nodeId: Function<T, ByteArray>,
      distanceToSelf: Function<T, Int>
    ): KademliaRoutingTable<T> = KademliaRoutingTable(selfId, k, maxReplacements, nodeId::apply, distanceToSelf::apply)
  }

  init {
    require(selfId.isNotEmpty()) { "self id must not be empty" }
    require(k > 0) { "k value must be positive" }
  }

  private val idBitSize = selfId.size * 8
  private val buckets: Array<Bucket<T>> = Array(idBitSize + 1) { Bucket<T>(k, maxReplacements) }

  private val distanceCache: Cache<T, Int> =
    CacheBuilder.newBuilder().maximumSize((1L + idBitSize) * k).weakKeys().build()

  /**
   * Provides the size of the table as the sum of the size of the buckets
   * @return the size of the table
   */
  override val size: Int
    get() = buckets.fold(0) { acc, bucket -> acc + bucket.size }

  /**
   * @return true if the table, ie all its buckets are empty
   */
  override fun isEmpty(): Boolean = buckets.find { bucket -> !bucket.isEmpty() } == null

  /**
   * @return an iterator to traverse all bucket contents
   */
  override fun iterator(): Iterator<T> = buckets.asSequence().flatMap { bucket -> bucket.asSequence() }.iterator()

  /**
   * @param element the element to test
   * @return true if there is a bucket with the element in it
   */
  override fun contains(element: T): Boolean = bucketFor(element).contains(element)

  /**
   * @param elements the elements to test
   * @return true if there is a bucket with all elements in it
   */
  override fun containsAll(elements: Collection<T>): Boolean {
    val peers = elements.toMutableSet()
    buckets.forEach { bucket ->
      peers.removeAll(peers.filter { peer -> bucket.contains(peer) })
      if (peers.isEmpty()) {
        return true
      }
    }
    return false
  }

  /**
   * Return the nearest nodes to a target id, in order from closest to furthest.
   *
   * The sort order is the log distance from the target id to the node ids.
   *
   * @param targetId the id to find nodes nearest to
   * @param limit the maximum number of nodes to return
   * @return a list of nodes from the routing table
   */
  fun nearest(targetId: ByteArray, limit: Int): List<T> {
    val results = mutableListOf<T>()
    for (bucket in buckets) {
      val bucketView = ArrayList(bucket)
      for (node in bucketView) {
        val nodeId = idForNode(node)
        results.orderedInsert(node) { a, _ -> targetId.xorDistCmp(idForNode(a), nodeId) }
        if (results.size > limit) {
          results.removeAt(results.lastIndex)
        }
      }
    }
    return results
  }

  /**
   * Add a node to the table.
   *
   * @param node the node to add
   * @return `null` if the node was successfully added to the table (or already in the table). Otherwise, a node
   *         will be returned that is a suitable candidate for eviction, and the provided node will be stored in
   *         the replacements list.
   */
  fun add(node: T): T? = bucketFor(node).add(node)

  /**
   * Remove a node from the table, potentially adding an alternative from the replacement cache.
   *
   * @param node the node to evict
   * @param `true` if the node was removed
   */
  fun evict(node: T): Boolean = bucketFor(node).evict(node)

  /**
   * Clear all nodes (and replacements) from the table.
   */
  fun clear() {
    buckets.forEach { bucket -> bucket.clear() }
  }

  /**
   * Returns all peers at a given distance of the original ID.
   */
  fun peersOfDistance(value: Int): List<T> {
    return buckets[value].toList()
  }

  /**
   * Provides a peer at random
   * @return a random peer from a random bucket
   */
  fun getRandom(): T {
    return buckets.filter { !it.isEmpty() }.random().random()
  }

  /**
   * Provides the distance between our identity and the peer
   * @param node the peer to compare to
   * @return the distance between the peer and our identity
   */
  fun logDistToSelf(node: T): Int {
    if (node == null) {
      return 0
    }
    return distanceCache.get(node) { distanceToSelf(node) }
  }

  private fun idForNode(node: T): ByteArray {
    val id = nodeId(node)
    require(id.size == selfId.size) { "id obtained for node is not the correct length" }
    require(!id.contentEquals(selfId)) { "id obtained for node is the same as self" }
    return id
  }

  private fun bucketFor(node: T) = buckets[logDistToSelf(node)]

  private class Bucket<E> private constructor(
    // ordered with most recent first
    private val entries: MutableList<E>,
    private val k: Int,
    private val maxReplacements: Int
  ) : List<E> by entries {

    constructor(k: Int, maxReplacements: Int) : this(Collections.synchronizedList(mutableListOf()), k, maxReplacements)

    // ordered with most recent last
    private val replacementCache = mutableListOf<E>()

    init {
      require(k > 0) { "k value must be positive" }
    }

    @Synchronized
    fun add(node: E): E? {
      // remove from the replacement cache, if present
      replacementCache.remove(node)

      // check if the list contains this node, and move to the front if so
      for (i in entries.indices) {
        if (entries[i] == node) {
          // already in table, so move to front
          entries.removeAt(i)
          entries.add(0, node)
          return null
        }
      }

      assert(entries.size <= k)
      if (entries.size == k) {
        // bucket is full, so add to the replacement cache
        assert(replacementCache.size <= maxReplacements)
        if (replacementCache.size == maxReplacements) {
          replacementCache.removeAt(0)
        }
        replacementCache.add(node)
        return entries.last()
      }

      // add entry to the front of the bucket
      entries.add(0, node)
      return null
    }

    // remove and replace from replacement cache
    @Synchronized
    fun evict(node: E): Boolean {
      if (!entries.remove(node)) {
        return false
      }
      if (!replacementCache.isEmpty()) {
        val replacement = replacementCache.removeAt(replacementCache.lastIndex)
        entries.add(0, replacement)
      }
      return true
    }

    @Synchronized
    fun clear() {
      entries.clear()
      replacementCache.clear()
    }
  }
}
