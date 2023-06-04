// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.junit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import redis.embedded.RedisServer;

/**
 * A junit5 extension, that sets up an ephemeral Redis server for tests.
 *
 * The ephemeral Redis server is created with a random free port for the test suite and injected into any tests with
 * parameters of type {@link Integer} annotated with {@link RedisPort}
 *
 * NOTE: Redis does not support picking a random port on its own. This extension tries its best to test free ports and
 * avoid collisions.
 */
public final class RedisServerExtension implements ParameterResolver, AfterAllCallback {

  private static Set<Integer> rangesIssued = ConcurrentHashMap.newKeySet();
  private static SecureRandom random = new SecureRandom();

  private static int findFreeRange() {
    int range = random.nextInt(326);
    if (!rangesIssued.add(range)) {
      return findFreeRange();
    }
    return range;
  }

  private static int findFreePort() {
    int range = findFreeRange() * 100 + 32768;
    int port = range;
    while (port < range + 100) {
      try {
        ServerSocket socket = new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
        socket.setReuseAddress(false);
        socket.close();
        return port;
      } catch (IOException e) {
        port++;
      }
    }
    throw new IllegalStateException("Could not reserve a port in range " + range + " and " + range + 100);
  }

  private RedisServer redisServer;

  private Thread shutdownThread = new Thread(() -> {
    if (redisServer != null) {
      try {
        redisServer.stop();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  });

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return Integer.class.equals(parameterContext.getParameter().getType())
        && parameterContext.isAnnotated(RedisPort.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    if (redisServer == null) {
      String localhost = InetAddress.getLoopbackAddress().getHostAddress();

      try {

        redisServer = RedisServer
            .newRedisServer()
            .setting("bind " + localhost)
            .setting("maxmemory 128mb")
            .setting("maxmemory-policy allkeys-lru")
            .setting("appendonly no")
            .setting("save \"\"")
            .port(findFreePort())
            .build();
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        redisServer.start();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return redisServer.ports().get(0);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (redisServer != null) {
      try {
        redisServer.stop();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      Runtime.getRuntime().removeShutdownHook(shutdownThread);
    }
  }
}
