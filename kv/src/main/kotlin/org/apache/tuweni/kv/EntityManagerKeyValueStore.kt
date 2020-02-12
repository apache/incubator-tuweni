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

import kotlin.jvm.Throws
import kotlinx.coroutines.Dispatchers

import java.io.IOException
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Stream
import javax.persistence.EntityManager
import kotlin.coroutines.CoroutineContext

class EntityManagerKeyValueStore<K, V>
        @Throws(IOException::class)
        constructor(
          private val entityManagerProvider: () -> EntityManager,
          private val entityClass: Class<V>,
          private val idAccessor: (V) -> K,
          override val coroutineContext: CoroutineContext = Dispatchers.IO
        ) : KeyValueStore<K, V> {

  companion object {
    /**
     * Open a relational database backed key-value store using a JPA entity manager.
     *
     * @param entityManagerProvider The supplier of entity manager to operate.
     * @return A key-value store.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun <K, V> open(
      entityManagerProvider: Supplier<EntityManager>,
      entityClass: Class<V>,
      idAccessor: Function<V, K>
    ) = EntityManagerKeyValueStore<K, V>(entityManagerProvider::get, entityClass, idAccessor::apply)
  }

  override suspend fun get(key: K): V? {
    val em = entityManagerProvider()
    em.transaction.begin()
    try {
      return em.find(entityClass, key)
    } finally {
      em.transaction.commit()
      em.close()
    }
  }

  override suspend fun put(key: K, value: V) {
    val em = entityManagerProvider()
    em.transaction.begin()
    try {
      em.merge(value)
    } finally {
      em.transaction.commit()
      em.close()
    }
  }

  override suspend fun keys(): Iterable<K> {
    val em = entityManagerProvider()
    val query = em.createQuery(em.criteriaBuilder.createQuery(entityClass))
    val resultStream: Stream<V> = query.resultStream
    return Iterable { resultStream.map(idAccessor).iterator() }
  }

  override fun close() {
  }
}
