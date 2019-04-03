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

import org.apache.tuweni.bytes.Bytes

/**
 * A key-value store backed by an in-memory Map.
 *
 * @param map The backing map for this store.
 * @return A key-value store.
 * @constructor Open an in-memory key-value store.
 */
class MapKeyValueStore
constructor(private val map: MutableMap<Bytes, Bytes> = HashMap()) : KeyValueStore {

  companion object {
    /**
     * Open an in-memory key-value store.
     *
     * This store will use a [java.util.HashMap] as a backing store.
     *
     * @return A key-value store.
     */
    @JvmStatic
    fun open(): MapKeyValueStore = MapKeyValueStore()

    /**
     * Open an in-memory key-value store.
     *
     * @param map The backing map for this store.
     * @return A key-value store.
     */
    @JvmStatic
    fun open(map: MutableMap<Bytes, Bytes>) = MapKeyValueStore(map)
  }

  override suspend fun get(key: Bytes): Bytes? = map[key]

  override suspend fun put(key: Bytes, value: Bytes) {
    map[key] = value
  }

  /**
   * Has no effect in this KeyValueStore implementation.
   */
  override fun close() {}
}
