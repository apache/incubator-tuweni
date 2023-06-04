// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
