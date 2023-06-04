// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.kv

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * A key-value store backed by an in-memory Map.
 *
 * @param map The backing map for this store.
 * @return A key-value store.
 * @constructor Open an in-memory key-value store.
 * @param coroutineContext the kotlin coroutine context
 */
class MapKeyValueStore<K, V>
constructor(
  private val map: MutableMap<K, V> = HashMap(),
  override val coroutineContext: CoroutineContext = Dispatchers.IO
) : KeyValueStore<K, V> {

  companion object {
    /**
     * Open an in-memory key-value store.
     *
     * This store will use a [java.util.HashMap] as a backing store.
     *
     * @return A key-value store.
     */
    @JvmStatic
    fun <K, V> open() = MapKeyValueStore<K, V>()

    /**
     * Open an in-memory key-value store.
     *
     * @param map The backing map for this store.
     * @return A key-value store.
     */
    @JvmStatic
    fun <K, V> open(map: MutableMap<K, V>) = MapKeyValueStore(map)
  }

  override suspend fun containsKey(key: K) = map.containsKey(key)

  override suspend fun get(key: K): V? = map[key]

  override suspend fun put(key: K, value: V) {
    map[key] = value
  }

  override suspend fun remove(key: K) {
    map.remove(key)
  }

  override suspend fun keys(): Iterable<K> = map.keys

  override suspend fun clear() {
    map.clear()
  }

  /**
   * Has no effect in this KeyValueStore implementation.
   */
  override fun close() {}
}
