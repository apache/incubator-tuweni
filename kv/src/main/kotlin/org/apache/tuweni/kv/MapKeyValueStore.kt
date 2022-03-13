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
