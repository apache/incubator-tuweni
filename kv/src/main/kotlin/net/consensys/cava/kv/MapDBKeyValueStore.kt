/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.kv

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.tuweni.bytes.Bytes
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.HTreeMap
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * A key-value store backed by a MapDB instance.
 *
 * @param dbPath The path to the MapDB database.
 * @param dispatcher The co-routine dispatcher for blocking tasks.
 * @return A key-value store.
 * @throws IOException If an I/O error occurs.
 * @constructor Open a MapDB-backed key-value store.
 */
class MapDBKeyValueStore
@Throws(IOException::class)
constructor(
  dbPath: Path,
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : KeyValueStore {

  companion object {
    /**
     * Open a MapDB-backed key-value store.
     *
     * @param dbPath The path to the MapDB database.
     * @return A key-value store.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun open(dbPath: Path) = MapDBKeyValueStore(dbPath)
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

  override suspend fun get(key: Bytes): Bytes? = withContext(dispatcher) {
    storageData[key]
  }

  override suspend fun put(key: Bytes, value: Bytes) = withContext(dispatcher) {
    storageData[key] = value
    db.commit()
  }

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
