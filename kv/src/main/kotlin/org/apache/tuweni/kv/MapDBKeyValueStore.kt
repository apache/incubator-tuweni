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
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.HTreeMap
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext

/**
 * A key-value store backed by a MapDB instance.
 *
 * @param dbPath The path to the MapDB database.
 * @param keySerializer the serializer of key objects to bytes
 * @param valueSerializer the serializer of value objects to bytes
 * @param keyDeserializer the deserializer of keys from bytes
 * @param valueDeserializer the deserializer of values from bytes
 * @param coroutineContext The co-routine context for blocking tasks.
 * @return A key-value store.
 * @throws IOException If an I/O error occurs.
 * @constructor Open a MapDB-backed key-value store.
 */
class MapDBKeyValueStore<K, V>
@Throws(IOException::class)
constructor(
  dbPath: Path,
  private val keySerializer: (K) -> Bytes,
  private val valueSerializer: (V) -> Bytes,
  private val keyDeserializer: (Bytes) -> K,
  private val valueDeserializer: (Bytes?) -> V?,
  override val coroutineContext: CoroutineContext = Dispatchers.IO
) : KeyValueStore<K, V> {

  companion object {
    /**
     * Open a MapDB-backed key-value store.
     *
     * @param dbPath The path to the MapDB database.
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
      keySerializer: (K) -> Bytes,
      valueSerializer: (V) -> Bytes,
      keyDeserializer: (Bytes) -> K,
      valueDeserializer: (Bytes?) -> V?
    ) =
      MapDBKeyValueStore<K, V>(dbPath, keySerializer, valueSerializer, keyDeserializer, valueDeserializer)
  }

  private val db: DB
  private val storageData: HTreeMap<Bytes, Bytes>

  init {
    Files.createDirectories(dbPath.parent)
    db = DBMaker.fileDB(dbPath.toFile()).transactionEnable().closeOnJvmShutdown().make()
    storageData = db.hashMap(
      "storageData",
      BytesSerializer(),
      BytesSerializer()
    ).createOrOpen()
  }

  override suspend fun get(key: K): V? = valueDeserializer(storageData[keySerializer(key)])

  override suspend fun put(key: K, value: V) {
    storageData[keySerializer(key)] = valueSerializer(value)
    db.commit()
  }

  override suspend fun keys(): Iterable<K> = storageData.keys.map(keyDeserializer)

  /**
   * Closes the underlying MapDB instance.
   */
  override fun close() = db.close()
}

private class BytesSerializer : org.mapdb.Serializer<Bytes> {

  override fun serialize(out: DataOutput2, value: Bytes) {
    out.packInt(value.size())
    out.write(value.toArrayUnsafe())
  }

  override fun deserialize(input: DataInput2, available: Int): Bytes {
    val size = input.unpackInt()
    val bytes = ByteArray(size)
    input.readFully(bytes)
    return Bytes.wrap(bytes)
  }
}
