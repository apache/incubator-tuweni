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
package org.apache.tuweni.plumtree.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;
import org.apache.tuweni.plumtree.EphemeralPeerRepository;

import java.util.concurrent.atomic.AtomicReference;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class VertxGossipServerTest {

  @Test
  void gossipDeadBeefToOtherNode(@VertxInstance Vertx vertx) throws Exception {

    AtomicReference<Bytes> messageReceived1 = new AtomicReference<>();
    AtomicReference<Bytes> messageReceived2 = new AtomicReference<>();

    VertxGossipServer server1 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10000,
        bytes -> bytes,
        new EphemeralPeerRepository(),
        messageReceived1::set,
        (message, peer) -> true,
        null,
        200,
        200);
    VertxGossipServer server2 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10001,
        bytes -> bytes,
        new EphemeralPeerRepository(),
        messageReceived2::set,
        (message, peer) -> true,
        null,
        200,
        200);

    server1.start().join();
    server2.start().join();

    server1.connectTo("127.0.0.1", 10001).join();
    String attributes = "{\"message_type\": \"BLOCK\"}";
    server1.gossip(attributes, Bytes.fromHexString("deadbeef"));
    for (int i = 0; i < 10; i++) {
      Thread.sleep(500);
      if (Bytes.fromHexString("deadbeef").equals(messageReceived2.get())) {
        break;
      }
    }
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived2.get());

    server1.stop().join();
    server2.stop().join();
  }

  @Test
  void gossipDeadBeefToTwoOtherNodes(@VertxInstance Vertx vertx) throws Exception {

    AtomicReference<Bytes> messageReceived1 = new AtomicReference<>();
    AtomicReference<Bytes> messageReceived2 = new AtomicReference<>();
    AtomicReference<Bytes> messageReceived3 = new AtomicReference<>();

    VertxGossipServer server1 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10000,
        bytes -> bytes,
        new EphemeralPeerRepository(),
        messageReceived1::set,
        (message, peer) -> true,
        null,
        200,
        200);
    VertxGossipServer server2 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10001,
        bytes -> bytes,
        new EphemeralPeerRepository(),
        messageReceived2::set,
        (message, peer) -> true,
        null,
        200,
        200);
    VertxGossipServer server3 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10002,
        bytes -> bytes,
        new EphemeralPeerRepository(),
        messageReceived3::set,
        (message, peer) -> true,
        null,
        200,
        200);

    server1.start().join();
    server2.start().join();
    server3.start().join();

    server1.connectTo("127.0.0.1", 10001).join();
    server3.connectTo("127.0.0.1", 10001).join();
    String attributes = "{\"message_type\": \"BLOCK\"}";
    server1.gossip(attributes, Bytes.fromHexString("deadbeef"));
    for (int i = 0; i < 10; i++) {
      Thread.sleep(500);
      if (Bytes.fromHexString("deadbeef").equals(messageReceived2.get())
          && Bytes.fromHexString("deadbeef").equals(messageReceived3.get())) {
        break;
      }
    }
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived2.get());
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived3.get());
    assertNull(messageReceived1.get());

    server1.stop().join();
    server2.stop().join();
  }

  @Test
  void gossipCollision(@VertxInstance Vertx vertx) throws Exception {
    AtomicReference<Bytes> messageReceived1 = new AtomicReference<>();
    AtomicReference<Bytes> messageReceived2 = new AtomicReference<>();

    EphemeralPeerRepository peerRepository1 = new EphemeralPeerRepository();
    EphemeralPeerRepository peerRepository3 = new EphemeralPeerRepository();

    VertxGossipServer server1 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10000,
        bytes -> bytes,
        peerRepository1,
        messageReceived1::set,
        (message, peer) -> true,
        null,
        200,
        200);
    VertxGossipServer server2 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10001,
        bytes -> bytes,
        new EphemeralPeerRepository(),
        messageReceived2::set,
        (message, peer) -> true,
        null,
        200,
        200);
    VertxGossipServer server3 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10002,
        bytes -> bytes,
        peerRepository3,
        messageReceived2::set,
        (message, peer) -> true,
        null,
        200,
        200);

    server1.start().join();
    server2.start().join();
    server3.start().join();

    server1.connectTo("127.0.0.1", 10001).join();
    server2.connectTo("127.0.0.1", 10002).join();
    server1.connectTo("127.0.0.1", 10002).join();
    assertEquals(2, peerRepository1.eagerPushPeers().size());
    String attributes = "{\"message_type\": \"BLOCK\"}";
    server1.gossip(attributes, Bytes.fromHexString("deadbeef"));
    Thread.sleep(1000);
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived2.get());
    Thread.sleep(1000);

    assertTrue(peerRepository1.lazyPushPeers().size() > 0 || peerRepository3.lazyPushPeers().size() > 0);

    server1.stop().join();
    server2.stop().join();
    server3.stop().join();
  }
}
