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
import kotlinx.coroutines.future.await
import org.checkerframework.checker.units.qual.K
import org.infinispan.Cache
import kotlin.coroutines.CoroutineContext

/**
 * A key-value store backed by [Infinispan](https://infinispan.org)
 *
 * @param cache the cache to use for this key-value store
 * @param coroutineContext the Kotlin coroutine context
 */
class InfinispanKeyValueStore<K, V> constructor(
  private val cache: Cache<K, V>,
  override val coroutineContext: CoroutineContext = Dispatchers.IO
) : KeyValueStore<K, V> {

  companion object {

    /**
     * Open an Infinispan key-value store.
     *
     * @param cache The backing cache for this store.
     * @return A key-value store.
     */
    @JvmStatic
    fun <K, V> open(cache: Cache<K, V>) = InfinispanKeyValueStore(cache)
  }

  override suspend fun containsKey(key: K): Boolean = cache.containsKeyAsync(key).await()

  override suspend fun get(key: K): V? = cache.getAsync(key).await()

  override suspend fun put(key: K, value: V) {
    cache.putAsync(key, value).await()
  }

  override suspend fun remove(key: K) {
    cache.removeAsync(key).await()
  }

  override suspend fun keys(): Iterable<K> = cache.keys

  override suspend fun clear() {
    cache.clearAsync().await()
  }

  /**
   * The cache is managed outside the scope of this key-value store.
   */
  override fun close() {
  }
}
