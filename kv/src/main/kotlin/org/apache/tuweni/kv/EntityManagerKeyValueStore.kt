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
import java.io.IOException
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Stream
import javax.persistence.EntityManager
import kotlin.coroutines.CoroutineContext

/**
 * JPA-backed key value store.
 *
 * @param entityClass the class of entity to store
 * @param entityManagerProvider the provider of entity managers to interact with JPA
 * @param idAccessor the ID accessor
 * @param coroutineContext the kotlin coroutine context.
 */
class EntityManagerKeyValueStore<K, V>
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
     * @param entityClass The class of objects to store in the store
     * @return A key-value store.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    fun <K, V> open(
      entityManagerProvider: Supplier<EntityManager>,
      entityClass: Class<V>,
      idAccessor: Function<V, K>
    ) = EntityManagerKeyValueStore(entityManagerProvider::get, entityClass, idAccessor::apply)
  }

  override suspend fun containsKey(key: K): Boolean = get(key) != null

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

  override suspend fun remove(key: K) {
    val em = entityManagerProvider()
    em.transaction.begin()
    try {
      val entity = em.find(entityClass, key)
      return em.remove(entity)
    } finally {
      em.transaction.commit()
      em.close()
    }
  }

  /**
   * Convenience method to put a record directly
   * @param value the value to store
   */
  suspend fun put(value: V) = put(idAccessor(value), value)

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
    em.transaction.begin()
    try {
      val query = em.criteriaBuilder.createQuery(entityClass)
      val root = query.from(entityClass)
      val all = query.select(root)
      val finalAll = em.createQuery(all)
      val resultStream: Stream<V> = finalAll.resultStream
      return Iterable { resultStream.map(idAccessor).iterator() }
    } finally {
      em.transaction.commit()
      em.close()
    }
  }

  override suspend fun clear() {
    val em = entityManagerProvider()
    em.transaction.begin()
    try {
      val deleteAll = em.createQuery("DELETE FROM ${em.metamodel.entity(entityClass).name}")
      deleteAll.executeUpdate()
    } finally {
      em.transaction.commit()
      em.close()
    }
  }

  /**
   * Close the store
   */
  override fun close() {
  }
}
