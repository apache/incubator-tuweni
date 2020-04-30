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
package org.apache.tuweni.rlpx.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;
import org.apache.tuweni.rlpx.MemoryWireConnectionsRepository;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({VertxExtension.class, BouncyCastleExtension.class})
class VertxRLPxServiceTest {

  @Test
  void invalidPort(@VertxInstance Vertx vertx) {
    Assertions
        .assertThrows(
            IllegalArgumentException.class,
            () -> new VertxRLPxService(vertx, -1, "localhost", 30, SECP256K1.KeyPair.random(), new ArrayList<>(), "a"));
  }

  @Test
  void invalidAdvertisedPort(@VertxInstance Vertx vertx) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new VertxRLPxService(vertx, 3, "localhost", -1, SECP256K1.KeyPair.random(), new ArrayList<>(), "a"));
  }

  @Test
  void invalidClientId(@VertxInstance Vertx vertx) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new VertxRLPxService(vertx, 34, "localhost", 23, SECP256K1.KeyPair.random(), new ArrayList<>(), null));
  }

  @Test
  void invalidClientIdSpaces(@VertxInstance Vertx vertx) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new VertxRLPxService(vertx, 34, "localhost", 23, SECP256K1.KeyPair.random(), new ArrayList<>(), "   "));
  }

  @Test
  void startAndStopService(@VertxInstance Vertx vertx) throws InterruptedException {
    VertxRLPxService service =
        new VertxRLPxService(vertx, 10000, "localhost", 10000, SECP256K1.KeyPair.random(), new ArrayList<>(), "a");

    service.start().join();
    try {
      assertEquals(10000, service.actualPort());
    } finally {
      service.stop();
    }
  }

  @Test
  void startServiceWithPortZero(@VertxInstance Vertx vertx) throws InterruptedException {
    VertxRLPxService service =
        new VertxRLPxService(vertx, 0, "localhost", 0, SECP256K1.KeyPair.random(), new ArrayList<>(), "a");

    service.start().join();
    try {
      assertTrue(service.actualPort() != 0);
      assertEquals(service.actualPort(), service.advertisedPort());
    } finally {
      service.stop();
    }
  }

  @Test
  void stopServiceWithoutStartingItFirst(@VertxInstance Vertx vertx) {
    VertxRLPxService service =
        new VertxRLPxService(vertx, 0, "localhost", 10000, SECP256K1.KeyPair.random(), new ArrayList<>(), "abc");
    AsyncCompletion completion = service.stop();
    assertTrue(completion.isDone());
  }

  @Test
  void connectToOtherPeer(@VertxInstance Vertx vertx) throws Exception {
    SECP256K1.KeyPair ourPair = SECP256K1.KeyPair.random();
    SECP256K1.KeyPair peerPair = SECP256K1.KeyPair.random();
    VertxRLPxService service = new VertxRLPxService(vertx, 0, "localhost", 10000, ourPair, new ArrayList<>(), "abc");
    service.start().join();

    VertxRLPxService peerService =
        new VertxRLPxService(vertx, 0, "localhost", 10000, peerPair, new ArrayList<>(), "abc");
    peerService.start().join();

    try {
      service.connectTo(peerPair.publicKey(), new InetSocketAddress(peerService.actualPort()));
    } finally {
      service.stop();
      peerService.stop();
    }
  }

  @Test
  void checkWireConnectionCreated(@VertxInstance Vertx vertx) throws Exception {
    SECP256K1.KeyPair ourPair = SECP256K1.KeyPair.random();
    SECP256K1.KeyPair peerPair = SECP256K1.KeyPair.random();

    MemoryWireConnectionsRepository repository = new MemoryWireConnectionsRepository();
    VertxRLPxService service =
        new VertxRLPxService(vertx, 0, "localhost", 10000, ourPair, new ArrayList<>(), "abc", repository);
    service.start().join();

    MemoryWireConnectionsRepository peerRepository = new MemoryWireConnectionsRepository();
    VertxRLPxService peerService =
        new VertxRLPxService(vertx, 0, "localhost", 10000, peerPair, new ArrayList<>(), "abc", peerRepository);
    peerService.start().join();

    try {
      service.connectTo(peerPair.publicKey(), new InetSocketAddress("localhost", peerService.actualPort()));
      Thread.sleep(3000);
      assertEquals(1, repository.asMap().size());

    } finally {
      service.stop();
      peerService.stop();
    }
  }
}
