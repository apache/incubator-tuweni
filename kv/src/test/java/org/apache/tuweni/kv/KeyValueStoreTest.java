/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.kv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.io.Base64;
import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class KeyValueStoreTest {

  private static final Function<Bytes, Bytes> bytesIdentityFn = Function.identity();

  @Test
  void testPutAndGet() throws Exception {
    Map<Bytes, Bytes> map = new HashMap<>();
    KeyValueStore<Bytes, Bytes> store = MapKeyValueStore.open(map);
    AsyncCompletion completion = store.putAsync(Bytes.of(123), Bytes.of(10, 12, 13));
    completion.join();
    assertTrue(store.containsKeyAsync(Bytes.of(123)).get());
    Bytes value = store.getAsync(Bytes.of(123)).get();
    assertNotNull(value);
    assertEquals(Bytes.of(10, 12, 13), value);
    assertEquals(Bytes.of(10, 12, 13), map.get(Bytes.of(123)));
  }

  @Test
  void testPutAndGetMapDB(@TempDirectory Path tmpDir) throws Exception {
    try (KeyValueStore<Bytes, Bytes> store = MapDBKeyValueStore
        .open(tmpDir.resolve("mapdb"), bytesIdentityFn, bytesIdentityFn, bytesIdentityFn, bytesIdentityFn)) {
      AsyncCompletion completion = store.putAsync(Bytes.of(123), Bytes.of(10, 12, 13));
      completion.join();
      assertTrue(store.containsKeyAsync(Bytes.of(123)).get());
      Bytes value = store.getAsync(Bytes.of(123)).get();
      assertNotNull(value);
      assertEquals(Bytes.of(10, 12, 13), value);
    }
  }

  @Test
  void testPutAndGetProxied(@TempDirectory Path tmpDir) throws Exception {
    KeyValueStore<Bytes, Bytes> store = MapDBKeyValueStore
        .open(tmpDir.resolve("mapdbproxy"), bytesIdentityFn, bytesIdentityFn, bytesIdentityFn, bytesIdentityFn);
    try (ProxyKeyValueStore<String, String, Bytes, Bytes> proxy = ProxyKeyValueStore
        .open(store, Base64::encode, Base64::decode, Base64::encode, (key, value) -> Base64.decode(value))) {
      AsyncCompletion completion = proxy.putAsync(Base64.encode(Bytes.of(123)), Base64.encode(Bytes.of(10, 12, 13)));
      completion.join();
      assertTrue(store.containsKeyAsync(Bytes.of(123)).get());
      String value = proxy.getAsync(Base64.encode(Bytes.of(123))).get();
      assertNotNull(value);
      assertEquals(Base64.encode(Bytes.of(10, 12, 13)), value);
      assertEquals(Bytes.of(10, 12, 13), store.getAsync(Bytes.of(123)).get());
    }

  }

  @Test
  void testPutAndGetMapDBDirect(@TempDirectory Path tmpDir) throws Exception {
    try (KeyValueStore<Bytes, Bytes> store = MapDBKeyValueStore.open(tmpDir.resolve("mapdbdirect"))) {
      AsyncCompletion completion = store.putAsync(Bytes.of(123), Bytes.of(10, 12, 13));
      completion.join();
      assertTrue(store.containsKeyAsync(Bytes.of(123)).get());
      Bytes value = store.getAsync(Bytes.of(123)).get();
      assertNotNull(value);
      assertEquals(Bytes.of(10, 12, 13), value);
    }
  }

  @Test
  void testNoValue() throws Exception {
    Map<Bytes, Bytes> map = new HashMap<>();
    KeyValueStore<Bytes, Bytes> store = MapKeyValueStore.open(map);
    assertNull(store.getAsync(Bytes.of(123)).get());
    assertFalse(store.containsKeyAsync(Bytes.of(123)).get());
  }

  @Test
  void testKeys() throws Exception {
    Map<Bytes, Bytes> map = new HashMap<>();
    KeyValueStore<Bytes, Bytes> store = MapKeyValueStore.open(map);
    AsyncCompletion completion = store.putAsync(Bytes.of(123), Bytes.of(10, 12, 13));
    completion.join();
    Set<Bytes> keys = new HashSet<>();
    store.keysAsync().get().forEach(keys::add);
    assertEquals(new HashSet<>(Arrays.asList(Bytes.of(123))), keys);
  }

  // disabled on Windows as CI doesn't have leveldb headers.
  @DisabledOnOs(OS.WINDOWS)
  @Test
  void testLevelDBWithoutOptions(@TempDirectory Path tempDirectory) throws Exception {
    try (LevelDBKeyValueStore<Bytes, Bytes> leveldb = LevelDBKeyValueStore
        .open(
            tempDirectory.resolve("foo").resolve("bar"),
            bytesIdentityFn,
            bytesIdentityFn,
            bytesIdentityFn,
            bytesIdentityFn)) {
      AsyncCompletion completion = leveldb.putAsync(Bytes.of(123), Bytes.of(10, 12, 13));
      completion.join();
      Bytes value = leveldb.getAsync(Bytes.of(123)).get();
      assertNotNull(value);
      assertEquals(Bytes.of(10, 12, 13), value);
    }
  }

  @Test
  void testRocksDBWithoutOptions(@TempDirectory Path tempDirectory) throws Exception {
    try (RocksDBKeyValueStore<Bytes, Bytes> rocksdb = RocksDBKeyValueStore
        .open(
            tempDirectory.resolve("foo").resolve("bar"),
            bytesIdentityFn,
            bytesIdentityFn,
            bytesIdentityFn,
            bytesIdentityFn)) {
      AsyncCompletion completion = rocksdb.putAsync(Bytes.of(123), Bytes.of(10, 12, 13));
      completion.join();
      Bytes value = rocksdb.getAsync(Bytes.of(123)).get();
      assertNotNull(value);
      assertEquals(Bytes.of(10, 12, 13), value);
    }
  }
}
