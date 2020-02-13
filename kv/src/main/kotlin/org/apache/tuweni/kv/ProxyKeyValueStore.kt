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

import kotlin.coroutines.CoroutineContext

/**
 * A store used as a proxy for another store.
 *
 * For example, we may want to store rich objects and transform them to a lower-level form,
 * or reuse the same store across multiple usages.
 */
class ProxyKeyValueStore<K, V, E, R>(
  private val store: KeyValueStore<E, R>,
  private val unproxyKey: (E) -> K,
  private val proxyKey: (K) -> E,
  private val unproxyValue: (R?) -> V?,
  private val proxyValue: (V) -> R,
  override val coroutineContext: CoroutineContext = store.coroutineContext
) : KeyValueStore<K, V> {

  override suspend fun get(key: K): V? = unproxyValue(store.get(proxyKey(key)))

  override suspend fun put(key: K, value: V) = store.put(proxyKey(key), proxyValue(value))

  override suspend fun keys(): Iterable<K> = store.keys().map(unproxyKey)

  override fun close() = store.close()
}
