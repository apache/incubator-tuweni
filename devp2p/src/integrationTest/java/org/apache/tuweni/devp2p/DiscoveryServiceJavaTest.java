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
package org.apache.tuweni.devp2p;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

@Timeout(10)
@ExtendWith({BouncyCastleExtension.class, VertxExtension.class})
class DiscoveryServiceJavaTest {

  @Test
  void setUpAndShutDownAsync(@VertxInstance Vertx vertx) throws Exception {
    DiscoveryService service = DiscoveryService.Companion.open(vertx, SECP256K1.KeyPair.random(), 0, "127.0.0.1");
    service.awaitBootstrapAsync().join();
    AsyncCompletion completion = service.shutdownAsync();
    completion.join();
    assertTrue(completion.isDone());
  }

  @Test
  void lookupAsync(@VertxInstance Vertx vertx) throws Exception {
    DiscoveryService service = DiscoveryService.Companion.open(vertx, SECP256K1.KeyPair.random(), 0, "127.0.0.1");
    service.awaitBootstrapAsync().join();
    AsyncResult<List<Peer>> result = service.lookupAsync(SECP256K1.KeyPair.random().publicKey());
    List<Peer> peers = result.get();
    service.shutdownAsync().join();
    assertTrue(peers != null && peers.isEmpty());
  }

  @Test
  void managePeerRepository(@VertxInstance Vertx vertx) throws Exception {
    SECP256K1.KeyPair peerKeyPair = SECP256K1.KeyPair.random();
    EphemeralPeerRepository repository = new EphemeralPeerRepository();
    DiscoveryService service = DiscoveryService.Companion
        .open(
            vertx,
            SECP256K1.KeyPair.random(),
            32456,
            "127.0.0.1",
            1,
            emptyMap(),
            Collections
                .singletonList(URI.create("enode://" + peerKeyPair.publicKey().toHexString() + "@127.0.0.1:10000")),
            repository);
    AsyncResult<Peer> result =
        repository.getAsync(URI.create("enode://" + peerKeyPair.publicKey().toHexString() + "@127.0.0.1:10000"));
    assertEquals(peerKeyPair.publicKey(), result.get().getNodeId());
    AsyncResult<Peer> byURIString =
        repository.getAsync("enode://" + peerKeyPair.publicKey().toHexString() + "@127.0.0.1:10000");
    assertEquals(peerKeyPair.publicKey(), byURIString.get().getNodeId());
    service.shutdownAsync().join();
  }
}
