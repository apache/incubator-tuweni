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

import com.jolbox.bonecp.BoneCP
import com.jolbox.bonecp.BoneCPConfig
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.apache.tuweni.bytes.Bytes
import java.io.IOException
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * A key-value store backed by a relational database.
 *
 * @param jdbcurl The JDBC url to connect to the database.
 * @param tableName the name of the table to use for storage.
 * @param keyColumn the key column of the store.
 * @param valueColumn the value column of the store.
 * @param coroutineContext The co-routine context for blocking tasks.
 * @return A key-value store.
 * @throws IOException If an I/O error occurs.
 * @constructor Open a relational database backed key-value store.
 */
class SQLKeyValueStore<K, V>
@Throws(IOException::class)
constructor(
  jdbcurl: String,
  val tableName: String = "store",
  val keyColumn: String = "key",
  val valueColumn: String = "value",
  private val keySerializer: (K) -> Bytes,
  private val valueSerializer: (V) -> Bytes,
  private val keyDeserializer: (Bytes) -> K,
  private val valueDeserializer: (Bytes?) -> V?,
  override val coroutineContext: CoroutineContext = Dispatchers.IO
) : KeyValueStore<K, V> {

  companion object {
    /**
     * Open a relational database backed key-value store.
     *
     * @param jdbcUrl The JDBC url to connect to the database.
     * @param keySerializer the serializer of key objects to bytes
     * @param valueSerializer the serializer of value objects to bytes
     * @param keyDeserializer the deserializer of keys from bytes
     * @param valueDeserializer the deserializer of values from bytes
     * @return A key-value store.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun <K, V> open(
      jdbcUrl: String,
      keySerializer: (K) -> Bytes,
      valueSerializer: (V) -> Bytes,
      keyDeserializer: (Bytes) -> K,
      valueDeserializer: (Bytes?) -> V?
    ) = SQLKeyValueStore<K, V>(
      jdbcurl = jdbcUrl,
      keySerializer = keySerializer,
      valueSerializer = valueSerializer,
      keyDeserializer = keyDeserializer,
      valueDeserializer = valueDeserializer
    )

    /**
     * Open a relational database backed key-value store.
     *
     * @param jdbcUrl The JDBC url to connect to the database.
     * @param tableName the name of the table to use for storage.
     * @param keyColumn the key column of the store.
     * @param valueColumn the value column of the store.
     * @param keySerializer the serializer of key objects to bytes
     * @param valueSerializer the serializer of value objects to bytes
     * @param keyDeserializer the deserializer of keys from bytes
     * @param valueDeserializer the deserializer of values from bytes
     * @return A key-value store.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun <K, V> open(
      jdbcUrl: String,
      tableName: String,
      keyColumn: String,
      valueColumn: String,
      keySerializer: (K) -> Bytes,
      valueSerializer: (V) -> Bytes,
      keyDeserializer: (Bytes) -> K,
      valueDeserializer: (Bytes?) -> V?
    ) =
      SQLKeyValueStore<K, V>(
        jdbcUrl,
        tableName,
        keyColumn,
        valueColumn,
        keySerializer,
        valueSerializer,
        keyDeserializer,
        valueDeserializer
      )
  }

  private val connectionPool: BoneCP

  init {
    val config = BoneCPConfig()
    config.jdbcUrl = jdbcurl

    connectionPool = BoneCP(config)
  }

  protected suspend fun obtainConnection(): Deferred<Connection> = async {
    connectionPool.asyncConnection.get(5, TimeUnit.SECONDS)
  }

  override suspend fun containsKey(key: K): Boolean {
    obtainConnection().await().use {
      val stmt = it.prepareStatement("SELECT $valueColumn FROM $tableName WHERE $keyColumn = ?")
      stmt.setBytes(1, keySerializer(key).toArrayUnsafe())
      stmt.execute()

      val rs = stmt.resultSet
      return rs.next()
    }
  }

  override suspend fun get(key: K): V? {
    obtainConnection().await().use {
      val stmt = it.prepareStatement("SELECT $valueColumn FROM $tableName WHERE $keyColumn = ?")
      stmt.setBytes(1, keySerializer(key).toArrayUnsafe())
      stmt.execute()

      val rs = stmt.resultSet

      return if (rs.next()) {
        valueDeserializer(Bytes.wrap(rs.getBytes(1)))
      } else {
        null
      }
    }
  }

  override suspend fun remove(key: K) {
    obtainConnection().await().use {
      val stmt = it.prepareStatement("DELETE FROM $tableName WHERE $keyColumn = ?")
      stmt.setBytes(1, keySerializer(key).toArrayUnsafe())
      stmt.execute()
    }
  }

  override suspend fun put(key: K, value: V) {
    obtainConnection().await().use {
      val stmt = it.prepareStatement("INSERT INTO $tableName($keyColumn, $valueColumn) VALUES(?,?)")
      stmt.setBytes(1, keySerializer(key).toArrayUnsafe())
      stmt.setBytes(2, valueSerializer(value).toArrayUnsafe())
      it.autoCommit = false
      try {
        stmt.execute()
      } catch (e: SQLException) {
        val updateStmt = it.prepareStatement("UPDATE $tableName SET $valueColumn=? WHERE $keyColumn=?")
        updateStmt.setBytes(1, valueSerializer(value).toArrayUnsafe())
        updateStmt.setBytes(2, keySerializer(key).toArrayUnsafe())
        updateStmt.execute()
      }
      it.commit()
      Unit
    }
  }

  private class SQLIterator<K>(val resultSet: ResultSet, val keyDeserializer: (Bytes) -> K) : Iterator<K> {

    private var next = resultSet.next()

    override fun hasNext(): Boolean = next

    override fun next(): K {
      val key = keyDeserializer(Bytes.wrap(resultSet.getBytes(1)))
      next = resultSet.next()
      return key
    }
  }

  override suspend fun keys(): Iterable<K> {
    obtainConnection().await().use {
      val stmt = it.prepareStatement("SELECT $keyColumn FROM $tableName")
      stmt.execute()
      return Iterable { SQLIterator(stmt.resultSet, keyDeserializer) }
    }
  }

  override suspend fun clear() {
    obtainConnection().await().use {
      val stmt = it.prepareStatement("DELETE FROM $tableName")
      stmt.execute()
    }
  }

  /**
   * Closes the underlying connection pool.
   */
  override fun close() = connectionPool.shutdown()
}
