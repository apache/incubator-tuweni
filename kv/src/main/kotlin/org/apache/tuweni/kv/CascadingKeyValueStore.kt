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
package org.apache.tuweni.kv

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

internal enum class OperationType {
  PUT,
  REMOVE,
  CLEAR
}

internal data class Operation<K, V>(val operationType: OperationType, val key: K?, val value: V?)

/**
 * A key-value store that cascade reads until one store has a value.
 *
 * Writes to the first store.
 */
class CascadingKeyValueStore<K, V>(
  val frontKv: KeyValueStore<K, V>,
  val backingKv: KeyValueStore<K, V>,
  override val coroutineContext: CoroutineContext = Dispatchers.IO
) : KeyValueStore<K, V> {

  private val changes = mutableListOf<Operation<K, V>>()

  override suspend fun containsKey(key: K): Boolean {
    if (frontKv.containsKey(key)) {
      return true
    }
    return backingKv.containsKey(key)
  }

  override suspend fun get(key: K): V? {
    if (frontKv.containsKey(key)) {
      return frontKv.get(key)
    }
    return backingKv.get(key)
  }

  override suspend fun put(key: K, value: V) {
    frontKv.put(key, value)
    changes.add(Operation(OperationType.PUT, key, value))
  }

  override suspend fun remove(key: K) {
    frontKv.remove(key)
    changes.add(Operation(OperationType.REMOVE, key, null))
  }

  override suspend fun keys(): Iterable<K> {
    return listOf(frontKv.keys(), backingKv.keys()).flatten().distinct()
  }

  override suspend fun clear() {
    frontKv.clear()
    changes.add(Operation(OperationType.CLEAR, null, null))
  }

  override fun close() {
    frontKv.close()
    backingKv.close()
  }

  /**
   * Applies changes from the front key/value store to the backing store, in the order they were received.
   */
  suspend fun applyChanges() {
    for (change in changes) {
      when (change.operationType) {
        OperationType.CLEAR -> backingKv.clear()
        OperationType.PUT -> backingKv.put(change.key!!, change.value!!)
        OperationType.REMOVE -> backingKv.remove(change.key!!)
      }
    }
    changes.clear()
  }
}
