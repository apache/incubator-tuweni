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
package org.apache.tuweni.scuttlebutt.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.crypto.sodium.Sodium;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;
import org.apache.tuweni.scuttlebutt.Identity;

import java.util.concurrent.atomic.AtomicReference;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class ScuttlebuttLocalDiscoveryServiceTest {

  @BeforeAll
  static void checkAvailable() {
    Assumptions.assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void startStop(@VertxInstance Vertx vertx) throws Exception {
    ScuttlebuttLocalDiscoveryService service = new ScuttlebuttLocalDiscoveryService(vertx, 0, "0.0.0.0", "233.0.10.0");
    service.start().join();
    service.stop().join();
  }

  @Test
  void startStart(@VertxInstance Vertx vertx) throws Exception {
    ScuttlebuttLocalDiscoveryService service = new ScuttlebuttLocalDiscoveryService(vertx, 0, "0.0.0.0", "233.0.10.0");
    service.start().join();
    service.start().join();
    service.stop().join();
  }

  @Test
  void invalidMulticastAddress(@VertxInstance Vertx vertx) throws Exception {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new ScuttlebuttLocalDiscoveryService(vertx, 8008, "0.0.0.0", "10.0.0.0"));
  }

  @Test
  void stopFirst(@VertxInstance Vertx vertx) throws Exception {
    ScuttlebuttLocalDiscoveryService service = new ScuttlebuttLocalDiscoveryService(vertx, 0, "0.0.0.0", "233.0.10.0");
    service.stop().join();
    service.start().join();
    service.stop().join();
  }

  @Test
  void broadcastAndListen(@VertxInstance Vertx vertx) throws Exception {
    ScuttlebuttLocalDiscoveryService service =
        new ScuttlebuttLocalDiscoveryService(vertx, 18008, 18009, "127.0.0.1", "127.0.0.1", false);
    ScuttlebuttLocalDiscoveryService service2 =
        new ScuttlebuttLocalDiscoveryService(vertx, 18009, 18008, "127.0.0.1", "127.0.0.1", false);

    try {
      service2.start().join();
      AtomicReference<LocalIdentity> ref = new AtomicReference<>();
      service2.addListener(ref::set);

      LocalIdentity localId = new LocalIdentity("10.0.0.1", 10000, Identity.random());
      service.addIdentityToBroadcastList(localId);
      service.start().join();
      service.broadcast();
      Thread.sleep(1000);
      Assertions.assertNotNull(ref.get());
      Assertions.assertEquals(localId, ref.get());
    } finally {
      AsyncCompletion.allOf(service2.stop(), service.stop()).join();
    }
  }

}
