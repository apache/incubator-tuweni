// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.kademlia

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OrderedInsertTest {

  @Test
  fun shouldInsertToEmptyList() {
    val list = mutableListOf<Int>()
    list.orderedInsert(1) { a, b -> a.compareTo(b) }
    assertEquals(listOf(1), list)
  }

  @Test
  fun shouldInsertInOrderedPosition() {
    val list = mutableListOf(1, 2, 5, 7)
    list.orderedInsert(4) { a, b -> a.compareTo(b) }
    assertEquals(listOf(1, 2, 4, 5, 7), list)
  }

  @Test
  fun shouldInsertDuplicateInOrderedPosition() {
    val list = mutableListOf(1, 2, 5, 7)
    list.orderedInsert(5) { a, b -> a.compareTo(b) }
    assertEquals(listOf(1, 2, 5, 5, 7), list)
  }

  @Test
  fun shouldInsertAtStart() {
    val list = mutableListOf(2, 5, 7)
    list.orderedInsert(1) { a, b -> a.compareTo(b) }
    assertEquals(listOf(1, 2, 5, 7), list)
  }

  @Test
  fun shouldInsertAtEnd() {
    val list = mutableListOf(2, 5, 7)
    list.orderedInsert(8) { a, b -> a.compareTo(b) }
    assertEquals(listOf(2, 5, 7, 8), list)
  }
}
