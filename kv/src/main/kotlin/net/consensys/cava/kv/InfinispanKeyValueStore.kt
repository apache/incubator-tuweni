/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.kv

import kotlinx.coroutines.future.await
import net.consensys.cava.bytes.Bytes
import org.infinispan.Cache

/**
 * A key-value store backed by [Infinispan](https://infinispan.org)
 *
 */
class InfinispanKeyValueStore constructor(private val cache: Cache<Bytes, Bytes>) : KeyValueStore {

  companion object {

    /**
     * Open an Infinispan key-value store.
     *
     * @param cache The backing cache for this store.
     * @return A key-value store.
     */
    @JvmStatic
    fun open(cache: Cache<Bytes, Bytes>) = InfinispanKeyValueStore(cache)
  }

  override suspend fun get(key: Bytes): Bytes? = cache.getAsync(key).await()

  override suspend fun put(key: Bytes, value: Bytes) {
    cache.putAsync(key, value).await()
  }

  /**
   * The cache is managed outside the scope of this key-value store.
   */
  override fun close() {
  }
}
