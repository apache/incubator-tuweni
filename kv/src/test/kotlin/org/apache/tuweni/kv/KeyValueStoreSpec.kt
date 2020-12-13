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

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import com.winterbe.expekt.should
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.io.Base64
import org.apache.tuweni.io.file.Files as TwFiles
import org.apache.tuweni.kv.Vars.bar
import org.apache.tuweni.kv.Vars.foo
import org.apache.tuweni.kv.Vars.foobar
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
import javax.persistence.Persistence

object Vars {
  val foo = Bytes.wrap("foo".toByteArray())!!
  val bar = Bytes.wrap("bar".toByteArray())!!
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

    it("should allow to see if a key is present") {
      runBlocking {
        kv.put(foobar, foo)
        kv.containsKey(foobar).should.be.`true`
        kv.containsKey(bar).should.be.`false`
      }
    }

    it("should return null when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }

    it("should iterate over keys") {
      runBlocking {
        kv.put(foobar, foo)
        kv.put(foo, bar)
        kv.keys().should.equal(setOf(foobar, foo))
      }
    }

    it("can clear its contents") {
      runBlocking {
        kv.put(foobar, foo)
        kv.clear()
        kv.get(foobar).should.be.`null`
      }
    }
  }
})

object InfinispanKeyValueStoreSpec : Spek({
  val cacheManager = DefaultCacheManager()
  cacheManager.defineConfiguration("local", ConfigurationBuilder().build())
  val backingMap: Cache<Bytes, Bytes> = cacheManager.getCache("local")
  val kv = InfinispanKeyValueStore(backingMap)

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

    it("should allow to see if a key is present") {
      runBlocking {
        kv.put(foobar, foo)
        kv.containsKey(foobar).should.be.`true`
        kv.containsKey(bar).should.be.`false`
      }
    }

    it("should return null when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }

    it("should iterate over keys") {
      runBlocking {
        kv.put(bar, foo)
        kv.put(foo, bar)
        val keys = (kv.keys().map { it })
        keys.should.contain(bar)
        keys.should.contain(foo)
      }
    }

    it("can clear its contents") {
      runBlocking {
        kv.put(foobar, foo)
        kv.clear()
        kv.get(foobar).should.be.`null`
      }
    }
  }
})

object MapDBKeyValueStoreSpec : Spek({
  val testDir = Files.createTempDirectory("data")
  val kv = MapDBKeyValueStore(
    testDir.resolve("data.db"),
    keySerializer = { it },
    valueSerializer = { it },
    keyDeserializer = { it },
    valueDeserializer = { it }
  )

  describe("a MapDB-backed key value store") {

    it("should allow to retrieve values") {
      runBlocking {
        kv.put(foobar, foo)
        kv.get(foobar).should.equal(foo)
      }
    }

    it("should allow to see if a key is present") {
      runBlocking {
        kv.put(foobar, foo)
        kv.containsKey(foobar).should.be.`true`
        kv.containsKey(bar).should.be.`false`
      }
    }

    it("should return null when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }

    it("should iterate over keys") {
      runBlocking {
        kv.put(foobar, foo)
        kv.put(foo, bar)
        val keys = kv.keys().map { it }
        keys.should.contain(foo)
        keys.should.contain(foobar)
      }
    }

    it("can clear its contents") {
      runBlocking {
        kv.put(foobar, foo)
        kv.clear()
        kv.get(foobar).should.be.`null`
      }
    }

    it("should not allow usage after the DB is closed") {
      val kv2 = MapDBKeyValueStore(
        testDir.resolve("data2.db"),
        keySerializer = { it },
        valueSerializer = { it },
        keyDeserializer = { it },
        valueDeserializer = { it }
      )
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
      TwFiles.deleteRecursively(testDir)
    }
  }
})

object LevelDBKeyValueStoreSpec : Spek({
  val path = Files.createTempDirectory("leveldb")
  val kv = LevelDBKeyValueStore(
    path,
    keySerializer = { it },
    valueSerializer = { it },
    keyDeserializer = { it },
    valueDeserializer = { it }
  )
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

    it("should allow to see if a key is present") {
      runBlocking {
        kv.put(foobar, foo)
        kv.containsKey(foobar).should.be.`true`
        kv.containsKey(bar).should.be.`false`
      }
    }

    it("should return null when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }

    it("should iterate over keys") {
      runBlocking {
        kv.put(foobar, foo)
        kv.put(foo, bar)
        setOf(foobar, foo).should.equal(kv.keys().map { it }.toSet())
      }
    }

    it("can clear its contents") {
      runBlocking {
        kv.put(foobar, foo)
        kv.clear()
        kv.get(foobar).should.be.`null`
      }
    }

    it("should not allow usage after the DB is closed") {
      val kv2 = LevelDBKeyValueStore(
        path.resolve("subdb"),
        keySerializer = { it },
        valueSerializer = { it },
        keyDeserializer = { it },
        valueDeserializer = { it }
      )
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
  val kv = RocksDBKeyValueStore(
    path,
    keySerializer = { it },
    valueSerializer = { it },
    keyDeserializer = { it },
    valueDeserializer = { it }
  )
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

    it("should allow to see if a key is present") {
      runBlocking {
        kv.put(foobar, foo)
        kv.containsKey(foobar).should.be.`true`
        kv.containsKey(bar).should.be.`false`
      }
    }

    it("should return null when no value is present") {
      runBlocking {
        kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
      }
    }

    it("should iterate over keys") {
      runBlocking {
        kv.put(bar, foo)
        kv.put(foo, bar)
        val keys = kv.keys().map { it }
        keys.should.contain(bar)
        keys.should.contain(foo)
      }
    }

    it("can clear its contents") {
      runBlocking {
        kv.put(foobar, foo)
        kv.clear()
        kv.get(foobar).should.be.`null`
      }
    }

    it("should not allow usage after the DB is closed") {
      val kv2 = RocksDBKeyValueStore(
        path.resolve("subdb"),
        keySerializer = { it },
        valueSerializer = { it },
        keyDeserializer = { it },
        valueDeserializer = { it }
      )
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
  val kv = SQLKeyValueStore(
    jdbcUrl,
    keySerializer = { it },
    valueSerializer = { it },
    keyDeserializer = { it },
    valueDeserializer = { it }
  )
  val otherkv = SQLKeyValueStore.open(
    jdbcUrl,
    "store2",
    "id",
    "val",
    keySerializer = { it },
    valueSerializer = { it },
    keyDeserializer = { it },
    valueDeserializer = { it }
  )
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

    it("should allow to see if a key is present") {
      runBlocking {
        kv.put(foobar, foo)
        kv.containsKey(foobar).should.be.`true`
        kv.containsKey(bar).should.be.`false`
      }
    }

    it("should allow to update values") {
      runBlocking {
        kv.put(foobar, foo)
        kv.put(foobar, bar)
        kv.get(foobar).should.equal(bar)
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

    it("should iterate over keys") {
      runBlocking {
        kv.put(bar, foo)
        kv.put(foo, bar)
        val keys = kv.keys().map { it }
        keys.should.contain(bar)
        keys.should.contain(foo)
      }
    }

    it("can clear its contents") {
      runBlocking {
        kv.put(foobar, foo)
        kv.clear()
        kv.get(foobar).should.be.`null`
      }
    }

    it("should not allow usage after the DB is closed") {
      val kv2 = SQLKeyValueStore(
        "jdbc:h2:mem:testdb",
        keySerializer = { it },
        valueSerializer = { it },
        keyDeserializer = { it },
        valueDeserializer = { it }
      )
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

object EntityManagerKeyValueStoreSpec : Spek({
  val entityManagerFactory = Persistence.createEntityManagerFactory("h2")
  val kv = EntityManagerKeyValueStore(entityManagerFactory::createEntityManager, Store::class.java, { it.key })
  afterGroup {
    kv.close()
  }
  describe("a JPA entity manager-backed key value store") {

    it("should allow to retrieve values") {
      runBlocking {
        kv.put("foo", Store("foo", "bar"))
        kv.get("foo").should.equal(Store("foo", "bar"))
      }
    }

    it("should allow to see if a key is present") {
      runBlocking {
        kv.put("foo", Store("foo", "bar"))
        kv.containsKey("foo").should.be.`true`
        kv.containsKey("foobar").should.be.`false`
      }
    }

    it("should allow to update values") {
      runBlocking {
        kv.put("foo", Store("foo", "bar"))
        kv.put("foo", Store("foo", "foobar"))
        kv.get("foo").should.equal(Store("foo", "foobar"))
      }
    }

    it("should allow to update values without keys") {
      runBlocking {
        kv.put(Store("foo2", "bar2"))
        kv.put(Store("foo2", "foobar2"))
        kv.get("foo2").should.equal(Store("foo2", "foobar2"))
      }
    }

    it("should return null when no value is present") {
      runBlocking {
        kv.get("foobar").should.be.`null`
      }
    }

    it("should iterate over keys") {
      runBlocking {
        kv.put("foo", Store("foo", "bar"))
        kv.put("bar", Store("bar", "bar"))
        val keys = kv.keys().map { it }
        keys.should.contain("bar")
        keys.should.contain("foo")
      }
    }

    it("can clear its contents") {
      runBlocking {
        kv.put(Store("foo", "bar"))
        kv.clear()
        kv.get("foo").should.be.`null`
      }
    }
  }
})

object ProxyKeyValueStoreSpec : Spek({
  val kv = MapKeyValueStore<String, String>()
  val proxy = ProxyKeyValueStore(
    kv,
    Base64::decode,
    Base64::encode,
    Base64::decode,
    { _: Bytes, value: Bytes -> Base64.encode(value) }
  )
  afterGroup {
    proxy.close()
  }
  describe("a proxy key value store") {

    it("should allow to retrieve values") {
      runBlocking {
        proxy.put(foo, bar)
        proxy.get(foo).should.equal(bar)
        kv.get(Base64.encode(foo)).should.equal(Base64.encode(bar))
      }
    }

    it("should allow to see if a key is present") {
      runBlocking {
        proxy.put(foo, bar)
        proxy.containsKey(foo).should.be.`true`
        proxy.containsKey(foobar).should.be.`false`
      }
    }

    it("should allow to update values") {
      runBlocking {
        proxy.put(foo, bar)
        proxy.put(foo, foobar)
        proxy.get(foo).should.equal(foobar)
      }
    }

    it("should return null when no value is present") {
      runBlocking {
        proxy.get(foobar).should.be.`null`
      }
    }

    it("should iterate over keys") {
      runBlocking {
        proxy.put(foo, bar)
        proxy.put(bar, foo)
        val keys = proxy.keys().map { it }
        keys.should.contain(bar)
        keys.should.contain(foo)
      }
    }

    it("can clear its contents") {
      runBlocking {
        proxy.put(foo, bar)
        proxy.clear()
        proxy.get(foo).should.be.`null`
      }
    }
  }
})
