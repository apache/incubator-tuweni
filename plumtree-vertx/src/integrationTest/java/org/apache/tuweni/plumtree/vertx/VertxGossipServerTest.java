// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.plumtree.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;
import org.apache.tuweni.plumtree.EphemeralPeerRepository;
import org.apache.tuweni.plumtree.MessageListener;
import org.apache.tuweni.plumtree.Peer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class VertxGossipServerTest {

  private static class MessageListenerImpl implements MessageListener {

    public Bytes message;

    @Override
    public void listen(Bytes messageBody, Map<String, Bytes> attributes, Peer peer) {
      message = messageBody;
    }
  }

  @Test
  void gossipDeadBeefToOtherNode(@VertxInstance Vertx vertx) throws Exception {

    MessageListenerImpl messageReceived1 = new MessageListenerImpl();
    MessageListenerImpl messageReceived2 = new MessageListenerImpl();

    VertxGossipServer server1 =
        new VertxGossipServer(
            vertx,
            "127.0.0.1",
            10000,
            bytes -> bytes,
            new EphemeralPeerRepository(),
            messageReceived1,
            (message, peer) -> true,
            null,
            200,
            200);
    VertxGossipServer server2 =
        new VertxGossipServer(
            vertx,
            "127.0.0.1",
            10001,
            bytes -> bytes,
            new EphemeralPeerRepository(),
            messageReceived2,
            (message, peer) -> true,
            null,
            200,
            200);

    server1.start().join();
    server2.start().join();

    server1.connectTo("127.0.0.1", 10001).join();
    Map<String, Bytes> attributes =
        Collections.singletonMap(
            "message_type", Bytes.wrap("BLOCK".getBytes(StandardCharsets.UTF_8)));
    server1.gossip(attributes, Bytes.fromHexString("deadbeef"));
    for (int i = 0; i < 10; i++) {
      Thread.sleep(500);
      if (Bytes.fromHexString("deadbeef").equals(messageReceived2.message)) {
        break;
      }
    }
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived2.message);

    server1.stop().join();
    server2.stop().join();
  }

  @Test
  void gossipDeadBeefToTwoOtherNodes(@VertxInstance Vertx vertx) throws Exception {

    MessageListenerImpl messageReceived1 = new MessageListenerImpl();
    MessageListenerImpl messageReceived2 = new MessageListenerImpl();
    MessageListenerImpl messageReceived3 = new MessageListenerImpl();

    VertxGossipServer server1 =
        new VertxGossipServer(
            vertx,
            "127.0.0.1",
            10000,
            bytes -> bytes,
            new EphemeralPeerRepository(),
            messageReceived1,
            (message, peer) -> true,
            null,
            200,
            200);
    VertxGossipServer server2 =
        new VertxGossipServer(
            vertx,
            "127.0.0.1",
            10001,
            bytes -> bytes,
            new EphemeralPeerRepository(),
            messageReceived2,
            (message, peer) -> true,
            null,
            200,
            200);
    VertxGossipServer server3 =
        new VertxGossipServer(
            vertx,
            "127.0.0.1",
            10002,
            bytes -> bytes,
            new EphemeralPeerRepository(),
            messageReceived3,
            (message, peer) -> true,
            null,
            200,
            200);

    server1.start().join();
    server2.start().join();
    server3.start().join();

    server1.connectTo("127.0.0.1", 10001).join();
    server3.connectTo("127.0.0.1", 10001).join();
    Map<String, Bytes> attributes =
        Collections.singletonMap(
            "message_type", Bytes.wrap("BLOCK".getBytes(StandardCharsets.UTF_8)));
    server1.gossip(attributes, Bytes.fromHexString("deadbeef"));
    for (int i = 0; i < 10; i++) {
      Thread.sleep(500);
      if (Bytes.fromHexString("deadbeef").equals(messageReceived2.message)
          && Bytes.fromHexString("deadbeef").equals(messageReceived3.message)) {
        break;
      }
    }
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived2.message);
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived3.message);
    assertNull(messageReceived1.message);

    server1.stop().join();
    server2.stop().join();
    server3.stop().join();
  }

  @Test
  void gossipCollision(@VertxInstance Vertx vertx) throws Exception {
    MessageListenerImpl messageReceived1 = new MessageListenerImpl();
    MessageListenerImpl messageReceived2 = new MessageListenerImpl();

    EphemeralPeerRepository peerRepository1 = new EphemeralPeerRepository();
    EphemeralPeerRepository peerRepository3 = new EphemeralPeerRepository();

    VertxGossipServer server1 =
        new VertxGossipServer(
            vertx,
            "127.0.0.1",
            10000,
            bytes -> bytes,
            peerRepository1,
            messageReceived1,
            (message, peer) -> true,
            null,
            200,
            200);
    VertxGossipServer server2 =
        new VertxGossipServer(
            vertx,
            "127.0.0.1",
            10001,
            bytes -> bytes,
            new EphemeralPeerRepository(),
            messageReceived2,
            (message, peer) -> true,
            null,
            200,
            200);
    VertxGossipServer server3 =
        new VertxGossipServer(
            vertx,
            "127.0.0.1",
            10002,
            bytes -> bytes,
            peerRepository3,
            messageReceived2,
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
    Map<String, Bytes> attributes =
        Collections.singletonMap(
            "message_type", Bytes.wrap("BLOCK".getBytes(StandardCharsets.UTF_8)));
    server1.gossip(attributes, Bytes.fromHexString("deadbeef"));
    Thread.sleep(1000);
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived2.message);
    Thread.sleep(1000);

    assertTrue(
        peerRepository1.lazyPushPeers().size() > 0 || peerRepository3.lazyPushPeers().size() > 0);

    server1.stop().join();
    server2.stop().join();
    server3.stop().join();
  }

  @Test
  void sendMessages(@VertxInstance Vertx vertx) throws Exception {
    MessageListenerImpl messageReceived1 = new MessageListenerImpl();
    MessageListenerImpl messageReceived2 = new MessageListenerImpl();

    EphemeralPeerRepository peerRepository1 = new EphemeralPeerRepository();

    VertxGossipServer server1 =
        new VertxGossipServer(
            vertx,
            "127.0.0.1",
            10000,
            bytes -> bytes,
            peerRepository1,
            messageReceived1,
            (message, peer) -> true,
            null,
            200,
            200);
    VertxGossipServer server2 =
        new VertxGossipServer(
            vertx,
            "127.0.0.1",
            10001,
            bytes -> bytes,
            new EphemeralPeerRepository(),
            messageReceived2,
            (message, peer) -> true,
            null,
            200,
            200);

    server1.start().join();
    server2.start().join();

    server1.connectTo("127.0.0.1", 10001).join();
    assertEquals(1, peerRepository1.eagerPushPeers().size());
    Map<String, Bytes> attributes =
        Collections.singletonMap(
            "message_type", Bytes.wrap("BLOCK".getBytes(StandardCharsets.UTF_8)));
    server1.send(
        peerRepository1.peers().iterator().next(), attributes, Bytes.fromHexString("deadbeef"));
    Thread.sleep(1000);
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived2.message);
    Thread.sleep(1000);

    server1.stop().join();
    server2.stop().join();
  }
}
