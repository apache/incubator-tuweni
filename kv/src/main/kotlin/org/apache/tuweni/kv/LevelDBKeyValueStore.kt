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
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.io.file.Files.deleteRecursively
import org.fusesource.leveldbjni.JniDBFactory
import org.iq80.leveldb.DB
import org.iq80.leveldb.DBIterator
import org.iq80.leveldb.Options
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Function
import kotlin.coroutines.CoroutineContext

/**
 * A key-value store backed by LevelDB.
 *
 * @param dbPath The path to the levelDB database.
 * @param keySerializer the serializer of key objects to bytes
 * @param valueSerializer the serializer of value objects to bytes
 * @param keyDeserializer the deserializer of keys from bytes
 * @param valueDeserializer the deserializer of values from bytes
 * @param options Options for the levelDB database.
 * @param coroutineContext The co-routine context for blocking tasks.
 * @return A key-value store.
 * @throws IOException If an I/O error occurs.
 * @constructor Open a LevelDB-backed key-value store.
 */
class LevelDBKeyValueStore<K, V>
@Throws(IOException::class)
constructor(
  private val dbPath: Path,
  private val keySerializer: (K) -> Bytes,
  private val valueSerializer: (V) -> Bytes,
  private val keyDeserializer: (Bytes) -> K,
  private val valueDeserializer: (Bytes) -> V,
  private val options: Options = Options().createIfMissing(true).cacheSize((100 * 1048576).toLong()),
  override val coroutineContext: CoroutineContext = Dispatchers.IO
) : KeyValueStore<K, V> {

  companion object {

    /**
     * Open a LevelDB-backed key-value store.
     *
     * @param dbPath The path to the levelDB database.
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
      dbPath: Path,
      keySerializer: Function<K, Bytes>,
      valueSerializer: Function<V, Bytes>,
      keyDeserializer: Function<Bytes, K>,
      valueDeserializer: Function<Bytes, V>
    ): LevelDBKeyValueStore<K, V> =
      LevelDBKeyValueStore(
        dbPath,
        keySerializer::apply,
        valueSerializer::apply,
        keyDeserializer::apply,
        valueDeserializer::apply
      )

    /**
     * Open a LevelDB-backed key-value store.
     *
     * @param dbPath The path to the levelDB database.
     * @param keySerializer the serializer of key objects to bytes
     * @param valueSerializer the serializer of value objects to bytes
     * @param keyDeserializer the deserializer of keys from bytes
     * @param valueDeserializer the deserializer of values from bytes
     * @param options Options for the levelDB database.
     * @return A key-value store.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun <K, V> open(
      dbPath: Path,
      keySerializer: Function<K, Bytes>,
      valueSerializer: Function<V, Bytes>,
      keyDeserializer: Function<Bytes, K>,
      valueDeserializer: Function<Bytes, V>,
      options: Options
    ) =
      LevelDBKeyValueStore(
        dbPath,
        keySerializer::apply,
        valueSerializer::apply,
        keyDeserializer::apply,
        valueDeserializer::apply,
        options
      )

    /**
     * Open a LevelDB-backed key-value store using Bytes keys and values.
     *
     * @param dbPath The path to the levelDB database.
     * @return A key-value store dealing with bytes.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun open(
      dbPath: Path
    ) =
      LevelDBKeyValueStore<Bytes, Bytes>(
        dbPath,
        Function.identity<Bytes>()::apply,
        Function.identity<Bytes>()::apply,
        Function.identity<Bytes>()::apply,
        Function.identity<Bytes>()::apply
      )
  }

  private var db: DB

  init {
    db = create()
  }

  private fun create(): DB {
    Files.createDirectories(dbPath)
    return JniDBFactory.factory.open(dbPath.toFile(), options)
  }

  override suspend fun containsKey(key: K): Boolean = db[keySerializer(key).toArrayUnsafe()] != null

  override suspend fun get(key: K): V? {
    val rawValue = db[keySerializer(key).toArrayUnsafe()]
    return if (rawValue == null) {
      null
    } else {
      valueDeserializer(Bytes.wrap(rawValue))
    }
  }

  override suspend fun remove(key: K) {
    db.delete(keySerializer(key).toArrayUnsafe())
  }

  override suspend fun put(key: K, value: V) = db.put(
    keySerializer(key).toArrayUnsafe(),
    valueSerializer(value).toArrayUnsafe()
  )

  private class KIterator<K>(val iter: DBIterator, val keyDeserializer: (Bytes) -> K) : Iterator<K> {
    override fun hasNext(): Boolean = iter.hasNext()

    override fun next(): K = keyDeserializer(Bytes.wrap(iter.next().key))
  }

  override suspend fun keys(): Iterable<K> {
    val iter = db.iterator()
    iter.seekToFirst()
    return Iterable { KIterator(iter, keyDeserializer) }
  }

  override suspend fun clear() {
    close()
    deleteRecursively(dbPath)
    db = create()
  }

  /**
   * Closes the underlying LevelDB instance.
   */
  override fun close() = db.close()
}
