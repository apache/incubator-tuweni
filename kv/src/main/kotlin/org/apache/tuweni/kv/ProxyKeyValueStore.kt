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

import java.util.function.BiFunction
import java.util.function.Function
import kotlin.coroutines.CoroutineContext

/**
 * A store used as a proxy for another store.
 *
 * For example, we may want to store rich objects and transform them to a lower-level form,
 * or reuse the same store across multiple usages.
 * @param store the key/value store
 * @param unproxyKey function to transform the key from the proxied store to the outer store
 * @param proxyKey function to transform the key from this proxy to the proxied store
 * @param unproxyValue function to transform the value from the proxied store to the outer store
 * @param proxyValue function to transform the value from this proxy to the proxied store
 * @param coroutineContext the kotlin coroutine context
 */
class ProxyKeyValueStore<K, V, E, R>(
  private val store: KeyValueStore<E, R>,
  private val unproxyKey: (E) -> K,
  private val proxyKey: (K) -> E,
  private val unproxyValue: (R) -> V,
  private val proxyValue: (K, V) -> R,
  override val coroutineContext: CoroutineContext = store.coroutineContext
) : KeyValueStore<K, V> {

  companion object {

    /**
     * Opens a proxy store.
     * @param store the store to proxy
     * @param unproxyKey the function to convert a key from the underlying store into a key of this store
     * @param proxyKey the function to convert a key from this store to the underlying store
     * @param unproxyValue the function to convert a value from the underlying store into a value of this store
     * @param proxyValue the function to convert a value from this store to the underlying store
     * @return A key-value store proxying the store passed in.
     */
    @JvmStatic
    fun <K, V, E, R> open(
      store: KeyValueStore<E, R>,
      unproxyKey: Function<E, K>,
      proxyKey: Function<K, E>,
      unproxyValue: Function<R, V>,
      proxyValue: BiFunction<K, V, R>
    ) =
      ProxyKeyValueStore(store, unproxyKey::apply, proxyKey::apply, unproxyValue::apply, proxyValue::apply)
  }

  override suspend fun containsKey(key: K) = store.containsKey(proxyKey(key))

  override suspend fun get(key: K): V? {
    val value = store.get(proxyKey(key))
    return if (value == null) {
      null
    } else {
      unproxyValue(value)
    }
  }

  override suspend fun remove(key: K) {
    store.remove(proxyKey(key))
  }

  override suspend fun put(key: K, value: V) = store.put(proxyKey(key), proxyValue(key, value))

  override suspend fun keys(): Iterable<K> = store.keys().map(unproxyKey)

  override suspend fun clear() = store.clear()

  /**
   * Close the store
   */
  override fun close() = store.close()
}
