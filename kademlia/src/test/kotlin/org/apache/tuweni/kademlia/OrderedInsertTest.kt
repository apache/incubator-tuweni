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
