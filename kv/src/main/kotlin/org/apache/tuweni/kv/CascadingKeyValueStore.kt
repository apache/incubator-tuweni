// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.kv

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

internal enum class OperationType {
  PUT,
  REMOVE,
  CLEAR,
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
  override val coroutineContext: CoroutineContext = Dispatchers.IO,
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
