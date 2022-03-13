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
import org.rocksdb.Options
import org.rocksdb.RocksDB
import org.rocksdb.RocksIterator
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function
import kotlin.coroutines.CoroutineContext

/**
 * A key-value store backed by RocksDB.
 *
 * @param dbPath The path to the RocksDB database.
 * @param options Options for the RocksDB database.
 * @param coroutineContext The co-routine context for blocking tasks.
 * @return A key-value store.
 * @throws IOException If an I/O error occurs.
 * @constructor Open a RocksDB-backed key-value store.
 */
class RocksDBKeyValueStore<K, V>
@Throws(IOException::class)
constructor(
  private val dbPath: Path,
  private val keySerializer: (K) -> Bytes,
  private val valueSerializer: (V) -> Bytes,
  private val keyDeserializer: (Bytes) -> K,
  private val valueDeserializer: (Bytes) -> V,
  private val options: Options = Options().setCreateIfMissing(true).setWriteBufferSize(268435456).setMaxOpenFiles(-1),
  override val coroutineContext: CoroutineContext = Dispatchers.IO
) : KeyValueStore<K, V> {

  companion object {
    /**
     * Open a RocksDB-backed key-value store.
     *
     * @param dbPath The path to the RocksDB database.
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
    ) = RocksDBKeyValueStore(
      dbPath,
      keySerializer::apply,
      valueSerializer::apply,
      keyDeserializer::apply,
      valueDeserializer::apply
    )

    /**
     * Open a RocksDB-backed key-value store.
     *
     * @param dbPath The path to the RocksDB database.
     * @param keySerializer the serializer of key objects to bytes
     * @param valueSerializer the serializer of value objects to bytes
     * @param keyDeserializer the deserializer of keys from bytes
     * @param valueDeserializer the deserializer of values from bytes
     * @param options Options for the RocksDB database.
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
      RocksDBKeyValueStore(
        dbPath,
        keySerializer::apply,
        valueSerializer::apply,
        keyDeserializer::apply,
        valueDeserializer::apply,
        options
      )
  }

  private var db: RocksDB
  private val closed = AtomicBoolean(false)

  init {
    RocksDB.loadLibrary()
    db = create()
  }

  private fun create(): RocksDB {
    Files.createDirectories(dbPath)
    return RocksDB.open(options, dbPath.toAbsolutePath().toString())
  }

  override suspend fun containsKey(key: K) = db[keySerializer(key).toArrayUnsafe()] != null

  override suspend fun get(key: K): V? {
    if (closed.get()) {
      throw IllegalStateException("Closed DB")
    }
    val rawValue = db[keySerializer(key).toArrayUnsafe()]
    return if (rawValue == null) {
      null
    } else {
      valueDeserializer(Bytes.wrap(rawValue))
    }
  }

  override suspend fun remove(key: K) {
    if (closed.get()) {
      throw IllegalStateException("Closed DB")
    }
    db.delete(keySerializer(key).toArrayUnsafe())
  }

  override suspend fun put(key: K, value: V) {
    if (closed.get()) {
      throw IllegalStateException("Closed DB")
    }
    db.put(keySerializer(key).toArrayUnsafe(), valueSerializer(value).toArrayUnsafe())
  }

  private class BytesIterator<K>(val rIterator: RocksIterator, val keyDeserializer: (Bytes) -> K) : Iterator<K> {

    override fun hasNext(): Boolean = rIterator.isValid

    override fun next(): K {
      val key = rIterator.key()
      rIterator.next()
      return keyDeserializer(Bytes.wrap(key))
    }
  }

  override suspend fun keys(): Iterable<K> {
    if (closed.get()) {
      throw IllegalStateException("Closed DB")
    }
    val iter = db.newIterator()
    iter.seekToFirst()
    return Iterable { BytesIterator(iter, keyDeserializer) }
  }

  override suspend fun clear() {
    close()
    if (closed.compareAndSet(true, false)) {
      deleteRecursively(dbPath)
      db = create()
    }
  }

  /**
   * Closes the underlying RocksDB instance.
   */
  override fun close() {
    if (closed.compareAndSet(false, true)) {
      db.close()
    }
  }
}
