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
import org.apache.tuweni.kv.Vars.bar
import org.apache.tuweni.kv.Vars.foo
import org.apache.tuweni.kv.Vars.foobar
import org.infinispan.Cache
import org.infinispan.configuration.cache.ConfigurationBuilder
import org.infinispan.manager.DefaultCacheManager
import org.iq80.leveldb.DBException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.fail
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.DriverManager
import java.util.concurrent.RejectedExecutionException
import javax.persistence.Persistence

object Vars {
  val foo = Bytes.wrap("foo".toByteArray())!!
  val bar = Bytes.wrap("bar".toByteArray())!!
  val foobar = Bytes.wrap("foobar".toByteArray())!!
}

class MapKeyValueStoreTest {
  val backingMap = mutableMapOf<Bytes, Bytes>()
  val kv = MapKeyValueStore(backingMap)

  @Test
  fun testStoreValues(): Unit = runBlocking {
    kv.put(foo, foo)
    backingMap.get(foo).should.equal(foo)
  }

  @Test
  fun testRetrieveValues(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.get(foobar).should.equal(foo)
  }

  @Test
  fun testRemoveValues(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.remove(foobar)
    kv.get(foobar).should.equal(null)
  }

  @Test
  fun testKeyPresent(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.containsKey(foobar).should.be.`true`
    kv.containsKey(bar).should.be.`false`
  }

  @Test
  fun testValueMissing(): Unit = runBlocking {
    kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
  }

  @Test
  fun testKeys(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.put(foo, bar)
    kv.keys().should.equal(setOf(foobar, foo))
  }

  @Test
  fun testClear(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.clear()
    kv.get(foobar).should.be.`null`
  }
}

class InfinispanKeyValueStoreTest {
  val cacheManager = DefaultCacheManager()

  init {
    cacheManager.defineConfiguration("local", ConfigurationBuilder().build())
  }

  val backingMap: Cache<Bytes, Bytes> = cacheManager.getCache("local")
  val kv = InfinispanKeyValueStore(backingMap)

  @Test
  fun testStoreValues(): Unit = runBlocking {
    kv.put(foo, foo)
    backingMap.get(foo).should.equal(foo)
  }

  @Test
  fun testRemoveValues(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.remove(foobar)
    kv.get(foobar).should.equal(null)
  }

  @Test
  fun testRetrieveValues() {
    runBlocking {
      kv.put(foobar, foo)
      kv.get(foobar).should.equal(foo)
    }
  }

  @Test
  fun testKeyPresent(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.containsKey(foobar).should.be.`true`
    kv.containsKey(bar).should.be.`false`
  }

  @Test
  fun testValueMissing() {
    runBlocking {
      kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
    }
  }

  @Test
  fun testKeys(): Unit = runBlocking {
    kv.put(bar, foo)
    kv.put(foo, bar)
    val keys = (kv.keys().map { it })
    keys.should.contain(bar)
    keys.should.contain(foo)
  }

  @Test
  fun testClear(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.clear()
    kv.get(foobar).should.be.`null`
  }
}

class MapDBKeyValueStoreTest {

  companion object {
    val testDir = Files.createTempDirectory("data")
    val kv = MapDBKeyValueStore(
      testDir.resolve("data.db"),
      keySerializer = { it },
      valueSerializer = { it },
      keyDeserializer = { it },
      valueDeserializer = { it }
    )

    @JvmStatic
    @AfterAll
    fun tearDown() {
      kv.close()
      org.apache.tuweni.io.file.Files.deleteRecursively(testDir)
    }
  }

  @Test
  fun testRetrieveValues(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.get(foobar).should.equal(foo)
  }

  @Test
  fun testRemoveValues(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.remove(foobar)
    kv.get(foobar).should.equal(null)
  }

  @Test
  fun testKeyPresent(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.containsKey(foobar).should.be.`true`
    kv.containsKey(bar).should.be.`false`
  }

  @Test
  fun testValueMissing(): Unit = runBlocking {
    kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
  }

  @Test
  fun testKeys(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.put(foo, bar)
    val keys = kv.keys().map { it }
    keys.should.contain(foo)
    keys.should.contain(foobar)
  }

  @Test
  fun testClear(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.clear()
    kv.get(foobar).should.be.`null`
  }

  @Test
  fun testUseAfterClose() {
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
}

// disabled on Windows as CI doesn't have leveldb headers.
@DisabledOnOs(OS.WINDOWS)
class LevelDBKeyValueStoreSpec {
  companion object {

    var path: Path? = null
    var kv: LevelDBKeyValueStore<Bytes, Bytes>? = null

    @JvmStatic
    @BeforeAll
    fun setUp() {
      path = Files.createTempDirectory("leveldb")
      kv = LevelDBKeyValueStore(
        path!!,
        keySerializer = { it },
        valueSerializer = { it },
        keyDeserializer = { it },
        valueDeserializer = { it }
      )
    }

    @JvmStatic
    @AfterAll
    fun tearDown() {
      kv!!.close()
      MoreFiles.deleteRecursively(path!!, RecursiveDeleteOption.ALLOW_INSECURE)
    }
  }

  @Test
  fun testRetrieveValues(): Unit = runBlocking {
    kv!!.put(foobar, foo)
    kv!!.get(foobar).should.equal(foo)
  }

  @Test
  fun testRemoveValues(): Unit = runBlocking {
    kv!!.put(foobar, foo)
    kv!!.remove(foobar)
    kv!!.get(foobar).should.equal(null)
  }

  @Test
  fun testKeyPresent(): Unit = runBlocking {
    kv!!.put(foobar, foo)
    kv!!.containsKey(foobar).should.be.`true`
    kv!!.containsKey(bar).should.be.`false`
  }

  @Test
  fun testValueMissing(): Unit = runBlocking {
    kv!!.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
  }

  @Test
  fun testKeys(): Unit = runBlocking {
    kv!!.put(foobar, foo)
    kv!!.put(foo, bar)
    setOf(foobar, foo).should.equal(kv!!.keys().map { it }.toSet())
  }

  @Test
  fun testClear(): Unit = runBlocking {
    kv!!.put(foobar, foo)
    kv!!.clear()
    kv!!.get(foobar).should.be.`null`
  }

  @Test
  fun testAfterClose() {
    val kv2 = LevelDBKeyValueStore(
      path!!.resolve("subdb"),
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

class RocksDBKeyValueStoreTest {
  companion object {
    val path = Files.createTempDirectory("rocksdb")
    val kv = RocksDBKeyValueStore(
      path,
      keySerializer = { it },
      valueSerializer = { it },
      keyDeserializer = { it },
      valueDeserializer = { it }
    )

    @JvmStatic
    @AfterAll
    fun tearDown() {
      kv.close()
      MoreFiles.deleteRecursively(path, RecursiveDeleteOption.ALLOW_INSECURE)
    }
  }

  @Test
  fun testRetrieveValues(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.get(foobar).should.equal(foo)
  }

  @Test
  fun testRemoveValues(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.remove(foobar)
    kv.get(foobar).should.equal(null)
  }

  @Test
  fun testKeyPresent(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.containsKey(foobar).should.be.`true`
    kv.containsKey(Bytes.fromHexString("0xdeadbeef")).should.be.`false`
  }

  @Test
  fun testValueMissing(): Unit = runBlocking {
    kv.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
  }

  @Test
  fun testKeys(): Unit = runBlocking {
    kv.put(bar, foo)
    kv.put(foo, bar)
    val keys = kv.keys().map { it }
    keys.should.contain(bar)
    keys.should.contain(foo)
  }

  @Test
  fun testClear(): Unit = runBlocking {
    kv.put(foobar, foo)
    kv.clear()
    kv.get(foobar).should.be.`null`
  }

  @Test
  fun testAfterClose() {
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

class SQLKeyValueStoreTest {
  companion object {

    var kv: SQLKeyValueStore<Bytes, Bytes>? = null
    var otherkv: SQLKeyValueStore<Bytes, Bytes>? = null

    @BeforeAll
    @JvmStatic
    fun setUp() {
      Files.deleteIfExists(Paths.get(System.getProperty("java.io.tmpdir"), "testdb.mv.db"))
      Files.deleteIfExists(Paths.get(System.getProperty("java.io.tmpdir"), "testdb.trace.db"))
      val jdbcUrl = "jdbc:h2:${System.getProperty("java.io.tmpdir")}/testdb"
      DriverManager.getConnection(jdbcUrl).use {
        val st = it.createStatement()
        st.executeUpdate("create table store(key binary, value binary, primary key(key))")
        st.executeUpdate("create table store2(id binary, val binary, primary key(id))")
      }

      kv = SQLKeyValueStore(
        jdbcUrl,
        keySerializer = { it },
        valueSerializer = { it },
        keyDeserializer = { it },
        valueDeserializer = { it }
      )
      otherkv = SQLKeyValueStore.open(
        jdbcUrl,
        "store2",
        "id",
        "val",
        keySerializer = { it },
        valueSerializer = { it },
        keyDeserializer = { it },
        valueDeserializer = { it }
      )
    }

    @JvmStatic
    @AfterAll
    fun tearDown() {
      kv!!.close()
      otherkv!!.close()
    }
  }

  @Test
  fun testRetrieveValues(): Unit = runBlocking {
    kv?.put(foobar, foo)
    kv?.get(foobar).should.equal(foo)
  }

  @Test
  fun testRemoveValues(): Unit = runBlocking {
    kv?.put(foobar, foo)
    kv?.remove(foobar)
    kv?.get(foobar).should.equal(null)
  }

  @Test
  fun testKeyPresent(): Unit = runBlocking {
    kv!!.put(foobar, foo)
    kv!!.containsKey(foobar).should.be.`true`
    kv!!.containsKey(Bytes.fromHexString("0xdeadbeef")).should.be.`false`
  }

  @Test
  fun testUpdateValues(): Unit = runBlocking {
    kv?.put(foobar, foo)
    kv?.put(foobar, bar)
    kv?.get(foobar).should.equal(bar)
  }

  @Test
  fun testOtherTable(): Unit = runBlocking {
    otherkv?.put(foobar, foo)
    otherkv?.get(foobar).should.equal(foo)
  }

  @Test
  fun testValueMissing(): Unit = runBlocking {
    kv?.get(Bytes.wrap("foofoobar".toByteArray())).should.be.`null`
  }

  @Test
  fun testKeys(): Unit = runBlocking {
    kv!!.put(bar, foo)
    kv!!.put(foo, bar)
    val keys = kv!!.keys().map { it }
    keys.should.contain(bar)
    keys.should.contain(foo)
  }

  @Test
  fun testClear() {
    runBlocking {
      kv?.put(foobar, foo)
      kv?.clear()
      kv?.get(foobar).should.be.`null`
    }
  }

  @Test
  fun testAfterClose() {
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

class EntityManagerKeyValueStoreSpec {

  companion object {
    val entityManagerFactory = Persistence.createEntityManagerFactory("h2")
    val kv =
      EntityManagerKeyValueStore(entityManagerFactory::createEntityManager, Store::class.java, { it.key })

    @JvmStatic
    @AfterAll
    fun tearDown() {
      kv.close()
    }
  }

  @Test
  fun testRetrieveValues(): Unit = runBlocking {
    kv.put("foo", Store("foo", "bar"))
    kv.get("foo").should.equal(Store("foo", "bar"))
  }

  @Test
  fun testRemoveValues(): Unit = runBlocking {
    kv.put("foobar", Store("foo", "bar"))
    kv.remove("foobar")
    kv.get("foobar").should.equal(null)
  }

  @Test
  fun testKeyPresent(): Unit = runBlocking {
    kv.put("foo", Store("foo", "bar"))
    kv.containsKey("foo").should.be.`true`
    kv.containsKey("foobar").should.be.`false`
  }

  @Test
  fun testUpdateValues(): Unit = runBlocking {
    kv.put("foo", Store("foo", "bar"))
    kv.put("foo", Store("foo", "foobar"))
    kv.get("foo").should.equal(Store("foo", "foobar"))
  }

  @Test
  fun testUpdateValuesWithoutKeys(): Unit = runBlocking {
    kv.put(Store("foo2", "bar2"))
    kv.put(Store("foo2", "foobar2"))
    kv.get("foo2").should.equal(Store("foo2", "foobar2"))
  }

  @Test
  fun testValueMissing() {
    runBlocking {
      kv.get("foobar").should.be.`null`
    }
  }

  @Test
  fun testKeys() {
    runBlocking {
      kv.put("foo", Store("foo", "bar"))
      kv.put("bar", Store("bar", "bar"))
      val keys = kv.keys().map { it }
      keys.should.contain("bar")
      keys.should.contain("foo")
    }
  }

  @Test
  fun testClear() {
    runBlocking {
      kv.put(Store("foo", "bar"))
      kv.clear()
      kv.get("foo").should.be.`null`
    }
  }
}

class ProxyKeyValueStoreSpec {
  companion object {
    val kv = MapKeyValueStore<String, String>()
    val proxy = ProxyKeyValueStore(
      kv,
      Base64::decode,
      Base64::encode,
      Base64::decode,
      { _: Bytes, value: Bytes -> Base64.encode(value) }
    )

    @JvmStatic
    @AfterAll
    fun tearDown() {
      proxy.close()
    }
  }

  @Test
  fun testRetrieveValues() {
    runBlocking {
      proxy.put(foo, bar)
      proxy.get(foo).should.equal(bar)
      kv.get(Base64.encode(foo)).should.equal(Base64.encode(bar))
    }
  }

  @Test
  fun testRemoveValues() {
    runBlocking {
      proxy.put(foo, bar)
      proxy.remove(foo)
      proxy.get(foo).should.equal(null)
    }
  }

  @Test
  fun testKeyPresent() {
    runBlocking {
      proxy.put(foo, bar)
      proxy.containsKey(foo).should.be.`true`
      proxy.containsKey(foobar).should.be.`false`
    }
  }

  @Test
  fun testUpdateValues(): Unit = runBlocking {
    proxy.put(foo, bar)
    proxy.put(foo, foobar)
    proxy.get(foo).should.equal(foobar)
  }

  @Test
  fun testValueMissing() {
    runBlocking {
      proxy.get(foobar).should.be.`null`
    }
  }

  @Test
  fun testKeys() {
    runBlocking {
      proxy.put(foo, bar)
      proxy.put(bar, foo)
      val keys = proxy.keys().map { it }
      keys.should.contain(bar)
      keys.should.contain(foo)
    }
  }

  @Test
  fun testClear() {
    runBlocking {
      proxy.put(foo, bar)
      proxy.clear()
      proxy.get(foo).should.be.`null`
    }
  }
}

class CascadingKeyValueStoreTest {
  companion object {
    val kv = MapKeyValueStore<Bytes, Bytes>()
    val backingKv = MapKeyValueStore<Bytes, Bytes>()
    val cascade = CascadingKeyValueStore(
      kv,
      backingKv
    )

    @JvmStatic
    @AfterAll
    fun tearDown() {
      cascade.close()
    }
  }

  @Test
  fun testRetrieveValues() {
    runBlocking {
      cascade.put(foo, bar)
      cascade.get(foo).should.equal(bar)
      kv.get(foo).should.equal(bar)
    }
  }

  @Test
  fun testRemoveValues() {
    runBlocking {
      cascade.put(foo, bar)
      cascade.remove(foo)
      cascade.get(foo).should.equal(null)
    }
  }

  @Test
  fun testKeyPresent() {
    runBlocking {
      cascade.put(foo, bar)
      cascade.containsKey(foo).should.be.`true`
      cascade.containsKey(foobar).should.be.`false`
    }
  }

  @Test
  fun testUpdateValues() {
    runBlocking {
      cascade.put(foo, bar)
      cascade.put(foo, foobar)
      cascade.get(foo).should.equal(foobar)
    }
  }

  @Test
  fun testValueMissing() {
    runBlocking {
      cascade.get(foobar).should.be.`null`
    }
  }

  @Test
  fun testKeys() {
    runBlocking {
      cascade.put(foo, bar)
      cascade.put(bar, foo)
      val keys = cascade.keys().map { it }
      keys.should.contain(bar)
      keys.should.contain(foo)
    }
  }

  @Test
  fun testClear() {
    runBlocking {
      cascade.put(foo, bar)
      cascade.clear()
      cascade.get(foo).should.be.`null`
    }
  }

  @Test
  fun testCascadeReadsToBackingStore() {
    runBlocking {
      backingKv.put(foo, bar)
      cascade.get(foo).should.equal(bar)
    }
  }

  @Test
  fun testCascadeKeys() {
    runBlocking {
      backingKv.put(foo, bar)
      cascade.keys().should.equal(listOf(foo))
    }
  }

  @Test
  fun testOverrideValuesWithoutTouchingBackingStore() {
    runBlocking {
      backingKv.put(foo, bar)
      cascade.put(foo, foobar)
      cascade.get(foo).should.equal(foobar)
      backingKv.get(foo).should.equal(bar)
    }
  }

  @Test
  fun testApplyChanges() {
    runBlocking {
      backingKv.put(bar, foo)
      cascade.put(foo, bar)
      cascade.remove(bar)
      cascade.applyChanges()
      backingKv.get(bar).should.be.`null`
      backingKv.get(foo).should.equal(bar)
      cascade.clear()
      cascade.applyChanges()
      for (key in backingKv.keys()) {
        fail("No key should be present, found $key")
      }
    }
  }
}
