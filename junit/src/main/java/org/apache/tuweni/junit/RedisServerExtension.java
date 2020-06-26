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
      redisServer.stop();
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

      redisServer = RedisServer
          .builder()
          .setting("bind " + localhost)
          .setting("maxmemory 128mb")
          .setting("maxmemory-policy allkeys-lru")
          .setting("appendonly no")
          .setting("save \"\"")
          .port(findFreePort())
          .build();
      Runtime.getRuntime().addShutdownHook(shutdownThread);
      redisServer.start();
    }
    return redisServer.ports().get(0);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (redisServer != null) {
      redisServer.stop();
      Runtime.getRuntime().removeShutdownHook(shutdownThread);
    }
  }
}
