// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.junit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@ExtendWith(RedisServerExtension.class)
class RedisServerExtensionTest {

  @Test
  void shouldHaveAccessToARedisServer(@RedisPort Integer port) {
    assertNotNull(port);
    assertTrue(port >= 32768, "Port must be more than 32768, was:" + port);
    try (JedisPool client =
        new JedisPool(new JedisPoolConfig(), InetAddress.getLoopbackAddress().getHostAddress(), port)) {
      try (Jedis conn = client.getResource()) {
        assertTrue(conn.isConnected());
      }
    }
  }
}
