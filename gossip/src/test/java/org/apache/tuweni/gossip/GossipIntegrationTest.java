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

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.junit.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({VertxExtension.class, TempDirectoryExtension.class, BouncyCastleExtension.class})
class GossipIntegrationTest {

  @Test
  void threeGossipServersStarting(@VertxInstance Vertx vertx, @TempDirectory Path tempDir) throws Exception {
    GossipCommandLineOptions opts1 = new GossipCommandLineOptions(
        new String[] {"tcp://127.0.0.1:9001", "tcp://127.0.0.1:9002"},
        9000,
        "127.0.0.1",
        tempDir.resolve("log1.log").toString(),
        10000,
        null);
    GossipCommandLineOptions opts2 = new GossipCommandLineOptions(
        new String[] {"tcp://127.0.0.1:9000", "tcp://127.0.0.1:9002"},
        9001,
        "127.0.0.1",
        tempDir.resolve("log2.log").toString(),
        10001,
        null);
    GossipCommandLineOptions opts3 = new GossipCommandLineOptions(
        new String[] {"tcp://127.0.0.1:9000", "tcp://127.0.0.1:9001"},
        9002,
        "127.0.0.1",
        tempDir.resolve("log3.log").toString(),
        10002,
        null);
    AtomicBoolean terminationRan = new AtomicBoolean(false);

    ExecutorService service = Executors.newFixedThreadPool(3);

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
    GossipApp app1 = app1Future.get(10, TimeUnit.SECONDS);
    GossipApp app2 = app2Future.get(10, TimeUnit.SECONDS);
    GossipApp app3 = app3Future.get(10, TimeUnit.SECONDS);

    assertFalse(terminationRan.get());

    HttpClient client = vertx.createHttpClient();

    for (int i = 0; i < 20; i++) {
      client.request(HttpMethod.POST, 10000, "127.0.0.1", "/publish").exceptionHandler(thr -> {
        throw new RuntimeException(thr);
      }).handler(resp -> {

      }).end(Buffer.buffer(Bytes32.rightPad(Bytes.ofUnsignedInt(i)).toHexString().getBytes(StandardCharsets.UTF_8)));
    }

    List<String> receiver1 = null;

    int counter = 0;
    do {
      Thread.sleep(1000);
      counter++;
      receiver1 = Files.readAllLines(tempDir.resolve("log2.log"));
    } while (receiver1.size() < 20 && counter < 20);

    client.close();

    service.submit(app1::stop);
    service.submit(app2::stop);
    service.submit(app3::stop);


    assertEquals(20, receiver1.size());
    List<String> receiver2 = Files.readAllLines(tempDir.resolve("log3.log"));
    assertEquals(20, receiver2.size());

    service.shutdown();

  }
}
