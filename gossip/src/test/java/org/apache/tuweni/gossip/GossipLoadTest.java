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
package org.apache.tuweni.gossip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Very basic load test scenario in one JVM.
 */
@ExtendWith({VertxExtension.class, TempDirectoryExtension.class, BouncyCastleExtension.class})
class GossipLoadTest {

  @Disabled
  @Test
  void fourGossipServersWithOneSender(@VertxInstance Vertx vertx, @TempDirectory Path tempDir) throws Exception {
    int numberOfMessages = 10000;
    int sendingInterval = 20;
    GossipCommandLineOptions opts1 = new GossipCommandLineOptions(
        new String[] {"tcp://127.0.0.1:9001", "tcp://127.0.0.1:9002", "tcp://127.0.0.1:9003"},
        9000,
        "127.0.0.1",
        tempDir.resolve("log1.log").toString(),
        10000,
        500,
        20,
        true,
        numberOfMessages,
        null);
    GossipCommandLineOptions opts2 = new GossipCommandLineOptions(
        new String[] {},
        9001,
        "127.0.0.1",
        tempDir.resolve("log2.log").toString(),
        10001,
        0,
        0,
        false,
        0,
        null);
    GossipCommandLineOptions opts3 = new GossipCommandLineOptions(
        new String[] {"tcp://127.0.0.1:9003"},
        9002,
        "127.0.0.1",
        tempDir.resolve("log3.log").toString(),
        10002,
        0,
        0,
        false,
        0,
        null);
    GossipCommandLineOptions opts4 = new GossipCommandLineOptions(
        new String[] {},
        9003,
        "127.0.0.1",
        tempDir.resolve("log4.log").toString(),
        10003,
        0,
        0,
        false,
        0,
        null);
    AtomicBoolean terminationRan = new AtomicBoolean(false);

    ExecutorService service = Executors.newFixedThreadPool(4);

    Future<GossipApp> app1Future = service.submit(() -> {
      GossipApp app = new GossipApp(vertx, opts1, System.err, System.out, () -> {
        terminationRan.set(true);
      });
      app.start();
      return app;
    });
    Future<GossipApp> app2Future = service.submit(() -> {
      GossipApp app = new GossipApp(vertx, opts2, System.err, System.out, () -> {
        terminationRan.set(true);
      });
      app.start();
      return app;
    });
    Future<GossipApp> app3Future = service.submit(() -> {
      GossipApp app = new GossipApp(vertx, opts3, System.err, System.out, () -> {
        terminationRan.set(true);
      });
      app.start();
      return app;
    });
    Future<GossipApp> app4Future = service.submit(() -> {
      GossipApp app = new GossipApp(vertx, opts4, System.err, System.out, () -> {
        terminationRan.set(true);
      });
      app.start();
      return app;
    });
    GossipApp app1 = app1Future.get(10, TimeUnit.SECONDS);
    GossipApp app2 = app2Future.get(10, TimeUnit.SECONDS);
    GossipApp app3 = app3Future.get(10, TimeUnit.SECONDS);
    GossipApp app4 = app4Future.get(10, TimeUnit.SECONDS);

    assertFalse(terminationRan.get());

    Thread.sleep((long) (numberOfMessages * sendingInterval * 1.33));


    service.submit(app1::stop);
    service.submit(app2::stop);
    service.submit(app3::stop);
    service.submit(app4::stop);

    List<String> receiver2 = Files.readAllLines(tempDir.resolve("log2.log"));
    List<String> receiver3 = Files.readAllLines(tempDir.resolve("log3.log"));
    List<String> receiver4 = Files.readAllLines(tempDir.resolve("log4.log"));
    System.out.println("n2: " + receiver2.size() + "\n3: " + receiver3.size() + "\n4: " + receiver4.size());
    assertEquals(numberOfMessages, receiver2.size());
    assertEquals(numberOfMessages, receiver3.size());
    assertEquals(numberOfMessages, receiver4.size());

    assertFalse(tempDir.resolve("log1.log").toFile().exists());

    service.shutdown();

  }
}
