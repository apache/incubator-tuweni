// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.plumtree.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.plumtree.EphemeralPeerRepository;
import org.apache.tuweni.plumtree.MessageListener;
import org.apache.tuweni.plumtree.Peer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.Test;

public class GossipServletTest {

  private static class MessageListenerImpl implements MessageListener {

    public Bytes message;

    @Override
    public void listen(Bytes messageBody, Map<String, Bytes> attributes, Peer peer) {
      message = messageBody;
    }
  }

  private static Server createHttpServer(GossipServlet gossipServlet, int port) {

    Server server = new Server(port);

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");

    server.setHandler(context);
    context.addServlet(new ServletHolder(gossipServlet), "");

    return server;
  }

  @Test
  void gossipDeadBeefToOtherNode() throws Exception {
    MessageListenerImpl messageReceived1 = new MessageListenerImpl();
    MessageListenerImpl messageReceived2 = new MessageListenerImpl();
    GossipServlet gossipServlet = new GossipServlet(
        200,
        200,
        bytes -> bytes,
        "http://127.0.0.1:10000",
        messageReceived1,
        (message, peer) -> true,
        null,
        new EphemeralPeerRepository());
    GossipServlet gossipServlet2 = new GossipServlet(
        200,
        200,
        bytes -> bytes,
        "http://127.0.0.1:10001",
        messageReceived2,
        (message, peer) -> true,
        null,
        new EphemeralPeerRepository());


    Server server1 = createHttpServer(gossipServlet, 10000);
    Server server2 = createHttpServer(gossipServlet2, 10001);

    server1.start();
    server2.start();

    gossipServlet.connectTo("http://127.0.0.1:10001").join();
    Map<String, Bytes> attributes =
        Collections.singletonMap("message_type", Bytes.wrap("BLOCK".getBytes(StandardCharsets.UTF_8)));
    gossipServlet.gossip(attributes, Bytes.fromHexString("deadbeef"));
    for (int i = 0; i < 10; i++) {
      Thread.sleep(500);
      if (Bytes.fromHexString("deadbeef").equals(messageReceived2.message)) {
        break;
      }
    }
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived2.message);

    server1.stop();
    server2.stop();
  }

  @Test
  void gossipDeadBeefToTwoOtherNodes() throws Exception {
    MessageListenerImpl messageReceived1 = new MessageListenerImpl();
    MessageListenerImpl messageReceived2 = new MessageListenerImpl();
    MessageListenerImpl messageReceived3 = new MessageListenerImpl();
    GossipServlet gossipServlet = new GossipServlet(
        200,
        200,
        bytes -> bytes,
        "http://127.0.0.1:10000",
        messageReceived1,
        (message, peer) -> true,
        null,
        new EphemeralPeerRepository());
    GossipServlet gossipServlet2 = new GossipServlet(
        200,
        200,
        bytes -> bytes,
        "http://127.0.0.1:10001",
        messageReceived2,
        (message, peer) -> true,
        null,
        new EphemeralPeerRepository());
    GossipServlet gossipServlet3 = new GossipServlet(
        200,
        200,
        bytes -> bytes,
        "http://127.0.0.1:10002",
        messageReceived3,
        (message, peer) -> true,
        null,
        new EphemeralPeerRepository());


    Server server1 = createHttpServer(gossipServlet, 10000);
    Server server2 = createHttpServer(gossipServlet2, 10001);
    Server server3 = createHttpServer(gossipServlet3, 10002);

    server1.start();
    server2.start();
    server3.start();

    gossipServlet.connectTo("http://127.0.0.1:10001").join();
    gossipServlet3.connectTo("http://127.0.0.1:10001").join();
    Map<String, Bytes> attributes =
        Collections.singletonMap("message_type", Bytes.wrap("BLOCK".getBytes(StandardCharsets.UTF_8)));
    gossipServlet.gossip(attributes, Bytes.fromHexString("deadbeef"));
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

    server1.stop();
    server2.stop();
    server3.stop();
  }

  @Test
  void gossipCollision() throws Exception {
    MessageListenerImpl messageReceived1 = new MessageListenerImpl();
    MessageListenerImpl messageReceived2 = new MessageListenerImpl();

    EphemeralPeerRepository peerRepository1 = new EphemeralPeerRepository();
    EphemeralPeerRepository peerRepository3 = new EphemeralPeerRepository();

    GossipServlet gossipServlet = new GossipServlet(
        200,
        200,
        bytes -> bytes,
        "http://127.0.0.1:10000",
        messageReceived1,
        (message, peer) -> true,
        null,
        peerRepository1);
    GossipServlet gossipServlet2 = new GossipServlet(
        200,
        200,
        bytes -> bytes,
        "http://127.0.0.1:10001",
        messageReceived2,
        (message, peer) -> true,
        null,
        new EphemeralPeerRepository());
    GossipServlet gossipServlet3 = new GossipServlet(
        200,
        200,
        bytes -> bytes,
        "http://127.0.0.1:10002",
        messageReceived2,
        (message, peer) -> true,
        null,
        peerRepository3);

    Server server1 = createHttpServer(gossipServlet, 10000);
    Server server2 = createHttpServer(gossipServlet2, 10001);
    Server server3 = createHttpServer(gossipServlet3, 10002);

    server1.start();
    server2.start();
    server3.start();

    try {

      gossipServlet.connectTo("http://127.0.0.1:10001").join();
      gossipServlet2.connectTo("http://127.0.0.1:10002").join();
      gossipServlet.connectTo("http://127.0.0.1:10002").join();

      assertEquals(2, peerRepository1.eagerPushPeers().size());
      Map<String, Bytes> attributes =
          Collections.singletonMap("message_type", Bytes.wrap("BLOCK".getBytes(StandardCharsets.UTF_8)));
      gossipServlet.gossip(attributes, Bytes.fromHexString("deadbeef"));
      Thread.sleep(1000);
      assertEquals(Bytes.fromHexString("deadbeef"), messageReceived2.message);
      Thread.sleep(1000);

      assertTrue(peerRepository1.lazyPushPeers().size() > 0 || peerRepository3.lazyPushPeers().size() > 0);

    } finally {
      server1.stop();
      server2.stop();
      server3.stop();
    }
  }

  @Test
  void sendMessages() throws Exception {
    MessageListenerImpl messageReceived1 = new MessageListenerImpl();
    MessageListenerImpl messageReceived2 = new MessageListenerImpl();

    EphemeralPeerRepository peerRepository1 = new EphemeralPeerRepository();

    GossipServlet gossipServlet = new GossipServlet(
        200,
        200,
        bytes -> bytes,
        "http://127.0.0.1:10000",
        messageReceived1,
        (message, peer) -> true,
        null,
        peerRepository1);
    GossipServlet gossipServlet2 = new GossipServlet(
        200,
        200,
        bytes -> bytes,
        "http://127.0.0.1:10001",
        messageReceived2,
        (message, peer) -> true,
        null,
        new EphemeralPeerRepository());

    Server server1 = createHttpServer(gossipServlet, 10000);
    Server server2 = createHttpServer(gossipServlet2, 10001);

    server1.start();
    server2.start();

    gossipServlet.connectTo("http://127.0.0.1:10001").join();
    assertEquals(1, peerRepository1.eagerPushPeers().size());
    Map<String, Bytes> attributes =
        Collections.singletonMap("message_type", Bytes.wrap("BLOCK".getBytes(StandardCharsets.UTF_8)));
    gossipServlet.send(peerRepository1.peers().iterator().next(), attributes, Bytes.fromHexString("deadbeef"));
    Thread.sleep(1000);
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived2.message);
    Thread.sleep(1000);

    server1.stop();
    server2.stop();
  }
}
