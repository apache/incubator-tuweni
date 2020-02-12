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
import org.apache.tuweni.bytes.Bytes
import org.infinispan.Cache
import kotlin.coroutines.CoroutineContext

/**
 * A key-value store backed by [Infinispan](https://infinispan.org)
 *
 */
class InfinispanKeyValueStore constructor(
  private val cache: Cache<Bytes, Bytes>,
  override val coroutineContext: CoroutineContext = Dispatchers.IO
) : KeyValueStore {

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

  override suspend fun keys(): Iterable<Bytes> = cache.keys

  /**
   * The cache is managed outside the scope of this key-value store.
   */
  override fun close() {
  }
}
