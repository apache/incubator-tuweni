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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.junit.RedisPort;
import org.apache.tuweni.junit.RedisServerExtension;

import java.net.InetAddress;
import java.util.function.Function;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RedisServerExtension.class)
class RedisKeyValueStoreTest {

  @Test
  void testPutAndGet(@RedisPort Integer redisPort) throws Exception {
    KeyValueStore<Bytes, Bytes> store = RedisKeyValueStore
        .open(redisPort, Function.identity(), Function.identity(), Function.identity(), Function.identity());
    AsyncCompletion completion = store.putAsync(Bytes.of(123), Bytes.of(10, 12, 13));
    completion.join();
    Bytes value = store.getAsync(Bytes.of(123)).get();
    assertNotNull(value);
    assertEquals(Bytes.of(10, 12, 13), value);
    RedisClient client =
        RedisClient.create(RedisURI.create(InetAddress.getLoopbackAddress().getHostAddress(), redisPort));
    try (StatefulRedisConnection<Bytes, Bytes> conn = client.connect(new RedisBytesCodec())) {
      assertEquals(Bytes.of(10, 12, 13), conn.sync().get(Bytes.of(123)));
    }
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
