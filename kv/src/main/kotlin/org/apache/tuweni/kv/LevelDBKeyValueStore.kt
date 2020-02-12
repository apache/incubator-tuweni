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
import org.fusesource.leveldbjni.JniDBFactory
import org.iq80.leveldb.DB
import org.iq80.leveldb.DBIterator
import org.iq80.leveldb.Options
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext

/**
 * A key-value store backed by LevelDB.
 *
 * @param dbPath The path to the levelDB database.
 * @param options Options for the levelDB database.
 * @param coroutineContext The co-routine context for blocking tasks.
 * @return A key-value store.
 * @throws IOException If an I/O error occurs.
 * @constructor Open a LevelDB-backed key-value store.
 */
class LevelDBKeyValueStore
@Throws(IOException::class)
constructor(
  dbPath: Path,
  options: Options = Options().createIfMissing(true).cacheSize((100 * 1048576).toLong()),
  override val coroutineContext: CoroutineContext = Dispatchers.IO
) : KeyValueStore {

  companion object {
    /**
     * Open a LevelDB-backed key-value store.
     *
     * @param dbPath The path to the levelDB database.
     * @return A key-value store.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun open(dbPath: Path) = LevelDBKeyValueStore(dbPath)

    /**
     * Open a LevelDB-backed key-value store.
     *
     * @param dbPath The path to the levelDB database.
     * @param options Options for the levelDB database.
     * @return A key-value store.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun open(dbPath: Path, options: Options) = LevelDBKeyValueStore(dbPath, options)
  }

  private val db: DB

  init {
    Files.createDirectories(dbPath)
    db = JniDBFactory.factory.open(dbPath.toFile(), options)
  }

  override suspend fun get(key: Bytes): Bytes? {
    val rawValue = db[key.toArrayUnsafe()]
    return if (rawValue == null) {
      null
    } else {
      Bytes.wrap(rawValue)
    }
  }

  override suspend fun put(key: Bytes, value: Bytes) = db.put(key.toArrayUnsafe(), value.toArrayUnsafe())

  private class BytesIterator(val iter: DBIterator) : Iterator<Bytes> {
    override fun hasNext(): Boolean = iter.hasNext()

    override fun next(): Bytes = Bytes.wrap(iter.next().key)
  }

  override suspend fun keys(): Iterable<Bytes> {
    val iter = db.iterator()
    iter.seekToFirst()
    return Iterable { BytesIterator(iter) }
  }

  /**
   * Closes the underlying LevelDB instance.
   */
  override fun close() = db.close()
}
