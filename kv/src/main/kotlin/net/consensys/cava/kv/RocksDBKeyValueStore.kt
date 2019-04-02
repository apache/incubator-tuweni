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
import org.rocksdb.Options
import org.rocksdb.RocksDB
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A key-value store backed by RocksDB.
 *
 * @param dbPath The path to the RocksDB database.
 * @param options Options for the RocksDB database.
 * @param dispatcher The co-routine context for blocking tasks.
 * @return A key-value store.
 * @throws IOException If an I/O error occurs.
 * @constructor Open a RocksDB-backed key-value store.
 */
class RocksDBKeyValueStore
@Throws(IOException::class)
constructor(
  dbPath: Path,
  options: Options = Options().setCreateIfMissing(true).setWriteBufferSize(268435456).setMaxOpenFiles(-1),
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : KeyValueStore {

  companion object {
    /**
     * Open a RocksDB-backed key-value store.
     *
     * @param dbPath The path to the RocksDB database.
     * @return A key-value store.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun open(dbPath: Path) = RocksDBKeyValueStore(dbPath)

    /**
     * Open a RocksDB-backed key-value store.
     *
     * @param dbPath The path to the RocksDB database.
     * @param options Options for the RocksDB database.
     * @return A key-value store.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun open(dbPath: Path, options: Options) = RocksDBKeyValueStore(dbPath, options)
  }

  private val db: RocksDB
  private val closed = AtomicBoolean(false)

  init {
    RocksDB.loadLibrary()
    Files.createDirectories(dbPath)
    db = RocksDB.open(options, dbPath.toAbsolutePath().toString())
  }

  override suspend fun get(key: Bytes): Bytes? = withContext(dispatcher) {
    if (closed.get()) {
      throw IllegalStateException("Closed DB")
    }
    val rawValue = db[key.toArrayUnsafe()]
    if (rawValue == null) {
      null
    } else {
      Bytes.wrap(rawValue)
    }
  }

  override suspend fun put(key: Bytes, value: Bytes) = withContext(dispatcher) {
    if (closed.get()) {
      throw IllegalStateException("Closed DB")
    }
    db.put(key.toArrayUnsafe(), value.toArrayUnsafe())
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
