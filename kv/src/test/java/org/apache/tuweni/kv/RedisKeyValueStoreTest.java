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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.junit.RedisPort;
import org.apache.tuweni.junit.RedisServerExtension;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@ExtendWith(RedisServerExtension.class)
class RedisKeyValueStoreTest {

  @Test
  void testPutAndGet(@RedisPort Integer redisPort) throws Exception {
    System.out.println(Bytes.wrap("\r\n".getBytes(StandardCharsets.US_ASCII)));
    KeyValueStore<Bytes, Bytes> store = RedisKeyValueStore
        .open(redisPort, Function.identity(), Function.identity(), Function.identity(), Function.identity());
    Bytes32 key = Bytes32.random();
    Bytes32 expectedValue = Bytes32.random();
    AsyncCompletion completion = store.putAsync(key, expectedValue);
    completion.join();
    try (JedisPool client =
        new JedisPool(new JedisPoolConfig(), InetAddress.getLoopbackAddress().getHostAddress(), redisPort)) {
      try (Jedis conn = client.getResource()) {
        assertArrayEquals(expectedValue.toArray(), conn.get(key.toArray()));
      }
    }
    Bytes value = store.getAsync(key).get();
    assertNotNull(value);
    assertEquals(expectedValue, value);
  }

  @Test
  void testNoValue(@RedisPort Integer redisPort) throws Exception {
    KeyValueStore<Bytes, Bytes> store = RedisKeyValueStore
        .open(
            redisPort,
            InetAddress.getLoopbackAddress(),
            Function.identity(),
            Function.identity(),
            Function.identity(),
            Function.identity());
    assertNull(store.getAsync(Bytes.of(124)).get());
  }

  @Test
  void testRedisCloseable(@RedisPort Integer redisPort) throws Exception {
    try (RedisKeyValueStore<Bytes, Bytes> redis = RedisKeyValueStore
        .open(
            "redis://127.0.0.1:" + redisPort,
            Function.identity(),
            Function.identity(),
            Function.identity(),
            Function.identity())) {
      AsyncCompletion completion = redis.putAsync(Bytes.of(125), Bytes.of(10, 12, 13));
      completion.join();
      Bytes value = redis.getAsync(Bytes.of(125)).get();
      assertNotNull(value);
      assertEquals(Bytes.of(10, 12, 13), value);
    }
  }
}
