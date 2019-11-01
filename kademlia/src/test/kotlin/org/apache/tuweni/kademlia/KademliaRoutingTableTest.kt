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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class KademliaRoutingTableTest {

  private val shortId = byteArrayOf(0x00)

  data class Node(val nodeId: ByteArray) {

    constructor(id: Byte) : this(byteArrayOf(id))

    override fun equals(other: Any?): Boolean {
      if (this === other) {
        return true
      }
      if (other !is Node) {
        return false
      }
      return nodeId.contentEquals(other.nodeId)
    }

    override fun hashCode(): Int {
      return nodeId.contentHashCode()
    }
  }

  @Test
  fun shouldBeEmptyOnConstruction() {
    val table = KademliaRoutingTable<Node>(shortId, 16, nodeId = { n -> n.nodeId })
    assertTrue(table.isEmpty())
    assertEquals(0, table.size)
  }

  @Test
  fun shouldAddToEmptyTable() {
    val table = KademliaRoutingTable<Node>(shortId, 16, nodeId = { n -> n.nodeId })
    val node = Node(0x01)
    assertNull(table.add(node))
    assertTrue(table.contains(node))
    assertEquals(1, table.size)
  }

  @Test
  fun shouldProposeEvictionIfBucketIsFull() {
    val table = KademliaRoutingTable<Node>(shortId, 2, nodeId = { n -> n.nodeId })
    val node1 = Node(0x04)
    assertNull(table.add(node1))
    val node2 = Node(0x05)
    assertNull(table.add(node2))
    assertTrue(table.contains(node1))
    assertTrue(table.contains(node2))
    assertEquals(2, table.size)

    val node3 = Node(0x06)
    assertEquals(node1, table.add(node3))
    assertTrue(table.contains(node1))
    assertTrue(table.contains(node2))
    assertFalse(table.contains(node3))
    assertEquals(2, table.size)
  }

  @Test
  fun shouldReplaceEvictedNode() {
    val table = KademliaRoutingTable<Node>(shortId, 2, nodeId = { n -> n.nodeId })
    val node1 = Node(0x04)
    val node2 = Node(0x05)
    table.add(node1)
    table.add(node2)

    val node3 = Node(0x06)
    assertEquals(node1, table.add(node3))

    assertTrue(table.evict(node1))
    assertFalse(table.contains(node1))
    assertTrue(table.contains(node2))
    assertTrue(table.contains(node3))
    assertEquals(2, table.size)
  }

  @Test
  fun shouldReturnNearestOrderedNodesUpToLimit() {
    val table = KademliaRoutingTable<Node>(shortId, 16, nodeId = { n -> n.nodeId })
    table.add(Node(0x05))
    table.add(Node(0x04))
    val node3 = Node(0x03)
    table.add(node3)
    val node2 = Node(0x02)
    table.add(node2)
    val node1 = Node(0x01)
    table.add(node1)
    assertEquals(5, table.size)

    val nearest = table.nearest(shortId, 3)
    assertEquals(3, nearest.size)
    assertTrue(nearest.containsAll(listOf(node1, node2, node3)))
    assertOrderedByLogDist(shortId, nearest)
  }

  @Test
  fun shouldReturnAllNodesWhenTableIsSmallerThanLimit() {
    val table = KademliaRoutingTable<Node>(shortId, 16, nodeId = { n -> n.nodeId })
    table.add(Node(0x05))
    table.add(Node(0x04))
    table.add(Node(0x03))
    table.add(Node(0x02))
    table.add(Node(0x01))
    assertEquals(5, table.size)

    val nearest = table.nearest(shortId, 10)
    assertEquals(5, nearest.size)
    assertOrderedByLogDist(shortId, nearest)
  }

  @Test
  fun shouldClearAllNodes() {
    val table = KademliaRoutingTable<Node>(shortId, 16, nodeId = { n -> n.nodeId })
    table.add(Node(0x05))
    table.add(Node(0x04))
    table.add(Node(0x03))
    table.add(Node(0x02))
    table.add(Node(0x01))
    assertEquals(5, table.size)
    table.clear()
    assertTrue(table.isEmpty())
    assertEquals(0, table.size)
  }

  private fun assertOrderedByLogDist(target: ByteArray, nodes: List<Node>) {
    var dist = 0
    for (n in nodes) {
      val nDist = target xorDist n.nodeId
      assertTrue(nDist >= dist) { "list is not ordered by distance" }
      dist = nDist
    }
  }
}
