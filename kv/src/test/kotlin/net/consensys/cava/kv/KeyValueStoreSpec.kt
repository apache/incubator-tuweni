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
package net.consensys.cava.kv

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import com.winterbe.expekt.should
import kotlinx.coroutines.runBlocking
import net.consensys.cava.bytes.Bytes
import net.consensys.cava.kv.Vars.foo
import net.consensys.cava.kv.Vars.foobar
import org.infinispan.Cache
import org.infinispan.configuration.cache.ConfigurationBuilder
import org.infinispan.manager.DefaultCacheManager
import org.iq80.leveldb.DBException
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.DriverManager
import java.util.concurrent.RejectedExecutionException

object Vars {
  val foo = Bytes.wrap("foo".toByteArray())!!
  val foobar = Bytes.wrap("foobar".toByteArray())!!
}

object KeyValueStoreSpec : Spek({
  val backingMap = mutableMapOf<Bytes, Bytes>()
  val kv = MapKeyValueStore(backingMap)

  describe("a map-backed key value store") {

    it("should allow to store values") {
      runBlocking {
        kv.put(foo, foo)
        backingMap.get(foo).should.equal(foo)
      }
    }

    it("should allow to retrieve values") {
      runBlocking {
        kv.put(foobar, foo)
        kv.get(foobar).should.equal(foo)
      }
    }

    it("should return null when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }
  }
})

object InfinispanKeyValueStoreSpec : Spek({
  val cacheManager = DefaultCacheManager()
  cacheManager.defineConfiguration("local", ConfigurationBuilder().build())
  val backingMap: Cache<Bytes, Bytes> = cacheManager.getCache("local")
  val kv = MapKeyValueStore(backingMap)

  describe("a map-backed key value store") {

    it("should allow to store values") {
      runBlocking {
        kv.put(foo, foo)
        backingMap.get(foo).should.equal(foo)
      }
    }

    it("should allow to retrieve values") {
      runBlocking {
        kv.put(foobar, foo)
        kv.get(foobar).should.equal(foo)
      }
    }

    it("should return null when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }
  }
})

object MapDBKeyValueStoreSpec : Spek({
  val testDir = Files.createTempDirectory("data")
  val kv = MapDBKeyValueStore(testDir.resolve("data.db"))

  describe("a MapDB-backed key value store") {

    it("should allow to retrieve values") {
      runBlocking {
        kv.put(foobar, foo)
        kv.get(foobar).should.equal(foo)
      }
    }

    it("should return null when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }

    it("should not allow usage after the DB is closed") {
      val kv2 = MapDBKeyValueStore(testDir.resolve("data2.db"))
      kv2.close()
      runBlocking {
        var caught = false
        try {
          kv2.put(foobar, foo)
        } catch (e: IllegalAccessError) {
          caught = true
        }
        caught.should.be.`true`
      }
    }

    afterGroup {
      kv.close()
      MoreFiles.deleteRecursively(testDir, RecursiveDeleteOption.ALLOW_INSECURE)
    }
  }
})

object LevelDBKeyValueStoreSpec : Spek({
  val path = Files.createTempDirectory("leveldb")
  val kv = LevelDBKeyValueStore(path)
  afterGroup {
    kv.close()
    MoreFiles.deleteRecursively(path, RecursiveDeleteOption.ALLOW_INSECURE)
  }
  describe("a levelDB-backed key value store") {

    it("should allow to retrieve values") {
      runBlocking {
        kv.put(foobar, foo)
        kv.get(foobar).should.equal(foo)
      }
    }

    it("should return null when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }

    it("should not allow usage after the DB is closed") {
      val kv2 = LevelDBKeyValueStore(path.resolve("subdb"))
      kv2.close()
      runBlocking {
        var caught = false
        try {
          kv2.put(foobar, foo)
        } catch (e: DBException) {
          caught = true
        }
        caught.should.be.`true`
      }
    }
  }
})

object RocksDBKeyValueStoreSpec : Spek({
  val path = Files.createTempDirectory("rocksdb")
  val kv = RocksDBKeyValueStore(path)
  afterGroup {
    kv.close()
    MoreFiles.deleteRecursively(path, RecursiveDeleteOption.ALLOW_INSECURE)
  }
  describe("a RocksDB-backed key value store") {

    it("should allow to retrieve values") {
      runBlocking {
        kv.put(foobar, foo)
        kv.get(foobar).should.equal(foo)
      }
    }

    it("should return null when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }

    it("should not allow usage after the DB is closed") {
      val kv2 = RocksDBKeyValueStore(path.resolve("subdb"))
      kv2.close()
      runBlocking {
        var caught = false
        try {
          kv2.put(foobar, foo)
        } catch (e: IllegalStateException) {
          caught = true
        }
        caught.should.be.`true`
      }
    }
  }
})

object SQLKeyValueStoreSpec : Spek({
  Files.deleteIfExists(Paths.get(System.getProperty("java.io.tmpdir"), "testdb.mv.db"))
  Files.deleteIfExists(Paths.get(System.getProperty("java.io.tmpdir"), "testdb.trace.db"))
  val jdbcUrl = "jdbc:h2:${System.getProperty("java.io.tmpdir")}/testdb"
  DriverManager.getConnection(jdbcUrl).use {
    val st = it.createStatement()
    st.executeUpdate("create table store(key binary, value binary, primary key(key))")
    st.executeUpdate("create table store2(id binary, val binary, primary key(id))")
  }
  val kv = SQLKeyValueStore(jdbcUrl)
  val otherkv = SQLKeyValueStore.open(jdbcUrl, "store2", "id", "val")
  afterGroup {
    kv.close()
    otherkv.close()
  }
  describe("a SQL-backed key value store") {

    it("should allow to retrieve values") {
      runBlocking {
        kv.put(foobar, foo)
        kv.get(foobar).should.equal(foo)
      }
    }

    it("should allow to retrieve values when configured with a different table") {
      runBlocking {
        otherkv.put(foobar, foo)
        otherkv.get(foobar).should.equal(foo)
      }
    }

    it("should return null when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }

    it("should not allow usage after the DB is closed") {
      val kv2 = SQLKeyValueStore("jdbc:h2:mem:testdb")
      kv2.close()
      runBlocking {
        var caught = false
        try {
          kv2.put(foobar, foo)
        } catch (e: RejectedExecutionException) {
          caught = true
        }
        caught.should.be.`true`
      }
    }
  }
})
