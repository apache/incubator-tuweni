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
package org.apache.tuweni.plumtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.Hash;
import org.apache.tuweni.junit.BouncyCastleExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class StateTest {

  private static class PeerImpl implements Peer {

  }

  private static class MockMessageSender implements MessageSender {

    Verb verb;
    String attributes;
    Peer peer;
    Bytes hash;
    Bytes payload;

    @Override
    public void sendMessage(Verb verb, String attributes, Peer peer, Bytes hash, Bytes payload) {
      this.verb = verb;
      this.attributes = attributes;
      this.peer = peer;
      this.hash = hash;
      this.payload = payload;
    }
  }

  private static final MessageListener messageListener = new MessageListener() {

    public Bytes message;

    @Override
    public void listen(Bytes messageBody, String attributes, Peer peer) {
      message = messageBody;
    }
  };

  @Test
  void testInitialState() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    assertTrue(repo.peers().isEmpty());
    assertTrue(repo.lazyPushPeers().isEmpty());
    assertTrue(repo.eagerPushPeers().isEmpty());
  }

  @Test
  void firstRoundWithThreePeers() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    State state = new State(
        repo,
        Hash::keccak256,
        new MockMessageSender(),
        messageListener,
        (message, peer) -> true,
        (peer) -> true);
    state.addPeer(new PeerImpl());
    state.addPeer(new PeerImpl());
    state.addPeer(new PeerImpl());
    assertTrue(repo.lazyPushPeers().isEmpty());
    assertEquals(3, repo.eagerPushPeers().size());
  }

  @Test
  void firstRoundWithTwoPeers() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    State state = new State(
        repo,
        Hash::keccak256,
        new MockMessageSender(),
        messageListener,
        (message, peer) -> true,
        (peer) -> true);
    state.addPeer(new PeerImpl());
    state.addPeer(new PeerImpl());
    assertTrue(repo.lazyPushPeers().isEmpty());
    assertEquals(2, repo.eagerPushPeers().size());
  }

  @Test
  void firstRoundWithOnePeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    State state = new State(
        repo,
        Hash::keccak256,
        new MockMessageSender(),
        messageListener,
        (message, peer) -> true,
        (peer) -> true);
    state.addPeer(new PeerImpl());
    assertTrue(repo.lazyPushPeers().isEmpty());
    assertEquals(1, repo.eagerPushPeers().size());
  }

  @Test
  void removePeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    State state = new State(
        repo,
        Hash::keccak256,
        new MockMessageSender(),
        messageListener,
        (message, peer) -> true,
        (peer) -> true);
    Peer peer = new PeerImpl();
    state.addPeer(peer);
    state.removePeer(peer);
    assertTrue(repo.peers().isEmpty());
    assertTrue(repo.lazyPushPeers().isEmpty());
    assertTrue(repo.eagerPushPeers().isEmpty());
  }

  @Test
  void prunePeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    State state = new State(
        repo,
        Hash::keccak256,
        new MockMessageSender(),
        messageListener,
        (message, peer) -> true,
        (peer) -> true);
    Peer peer = new PeerImpl();
    state.addPeer(peer);
    state.receivePruneMessage(peer);
    assertEquals(0, repo.eagerPushPeers().size());
    assertEquals(1, repo.lazyPushPeers().size());
  }

  @Test
  void graftPeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    State state = new State(
        repo,
        Hash::keccak256,
        new MockMessageSender(),
        messageListener,
        (message, peer) -> true,
        (peer) -> true);
    Peer peer = new PeerImpl();
    state.addPeer(peer);
    state.receivePruneMessage(peer);
    assertEquals(0, repo.eagerPushPeers().size());
    assertEquals(1, repo.lazyPushPeers().size());
    state.receiveGraftMessage(peer, Bytes32.random());
    assertEquals(1, repo.eagerPushPeers().size());
    assertEquals(0, repo.lazyPushPeers().size());
  }

  @Test
  void receiveFullMessageFromEagerPeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    MockMessageSender messageSender = new MockMessageSender();
    State state =
        new State(repo, Hash::keccak256, messageSender, messageListener, (message, peer) -> true, (peer) -> true);
    Peer peer = new PeerImpl();
    state.addPeer(peer);
    Peer otherPeer = new PeerImpl();
    state.addPeer(otherPeer);
    Bytes32 msg = Bytes32.random();
    String attributes = "{\"message_type\": \"BLOCK\"}";
    state.receiveGossipMessage(peer, attributes, msg, Hash.keccak256(msg));
    assertEquals(msg, messageSender.payload);
    assertEquals(otherPeer, messageSender.peer);
  }

  @Test
  void receiveFullMessageFromEagerPeerWithALazyPeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    MockMessageSender messageSender = new MockMessageSender();
    State state =
        new State(repo, Hash::keccak256, messageSender, messageListener, (message, peer) -> true, (peer) -> true);
    Peer peer = new PeerImpl();
    state.addPeer(peer);
    Peer otherPeer = new PeerImpl();
    state.addPeer(otherPeer);
    Bytes32 msg = Bytes32.random();
    Peer lazyPeer = new PeerImpl();
    state.addPeer(lazyPeer);
    repo.moveToLazy(lazyPeer);
    String attributes = "{\"message_type\": \"BLOCK\"}";
    state.receiveGossipMessage(peer, attributes, msg, Hash.keccak256(msg));
    assertEquals(msg, messageSender.payload);
    assertEquals(otherPeer, messageSender.peer);
    state.processQueue();
    assertEquals(Hash.keccak256(msg), messageSender.hash);
    assertEquals(lazyPeer, messageSender.peer);
    assertEquals(MessageSender.Verb.IHAVE, messageSender.verb);
    assertTrue(state.lazyQueue.isEmpty());
  }

  @Test
  void receiveFullMessageFromEagerPeerThenPartialMessageFromLazyPeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    MockMessageSender messageSender = new MockMessageSender();
    State state =
        new State(repo, Hash::keccak256, messageSender, messageListener, (message, peer) -> true, (peer) -> true);
    Peer peer = new PeerImpl();
    state.addPeer(peer);
    Peer lazyPeer = new PeerImpl();
    state.addPeer(lazyPeer);
    repo.moveToLazy(lazyPeer);
    Bytes message = Bytes32.random();
    String attributes = "{\"message_type\": \"BLOCK\"}";
    state.receiveGossipMessage(peer, attributes, message, Hash.keccak256(message));
    state.receiveIHaveMessage(lazyPeer, message);
    assertNull(messageSender.payload);
    assertNull(messageSender.peer);
  }

  @Test
  void receivePartialMessageFromLazyPeerAndNoFullMessage() throws Exception {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    MockMessageSender messageSender = new MockMessageSender();
    State state = new State(
        repo,
        Hash::keccak256,
        messageSender,
        messageListener,
        (message, peer) -> true,
        (peer) -> true,
        100,
        4000);
    Peer peer = new PeerImpl();
    state.addPeer(peer);
    Peer lazyPeer = new PeerImpl();
    state.addPeer(lazyPeer);
    repo.moveToLazy(lazyPeer);
    Bytes message = Bytes32.random();
    state.receiveIHaveMessage(lazyPeer, message);
    Thread.sleep(200);
    assertEquals(message, messageSender.hash);
    assertEquals(lazyPeer, messageSender.peer);
    assertEquals(MessageSender.Verb.GRAFT, messageSender.verb);
  }

  @Test
  void receivePartialMessageFromLazyPeerAndThenFullMessage() throws Exception {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    MockMessageSender messageSender = new MockMessageSender();
    State state = new State(
        repo,
        Hash::keccak256,
        messageSender,
        messageListener,
        (message, peer) -> true,
        (peer) -> true,
        500,
        4000);
    Peer peer = new PeerImpl();
    state.addPeer(peer);
    Peer lazyPeer = new PeerImpl();
    state.addPeer(lazyPeer);
    repo.moveToLazy(lazyPeer);
    Bytes message = Bytes32.random();
    state.receiveIHaveMessage(lazyPeer, Hash.keccak256(message));
    Thread.sleep(100);
    String attributes = "{\"message_type\": \"BLOCK\"}";
    state.receiveGossipMessage(peer, attributes, message, Hash.keccak256(message));
    Thread.sleep(500);
    assertNull(messageSender.verb);
    assertNull(messageSender.payload);
    assertNull(messageSender.peer);
  }

  @Test
  void receiveFullMessageFromUnknownPeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    MockMessageSender messageSender = new MockMessageSender();
    State state =
        new State(repo, Hash::keccak256, messageSender, messageListener, (message, peer) -> true, (peer) -> true);
    Peer peer = new PeerImpl();
    Bytes message = Bytes32.random();
    String attributes = "{\"message_type\": \"BLOCK\"}";
    state.receiveGossipMessage(peer, attributes, message, Hash.keccak256(message));
    assertEquals(1, repo.eagerPushPeers().size());
    assertEquals(0, repo.lazyPushPeers().size());
    assertEquals(peer, repo.eagerPushPeers().iterator().next());
  }

  @Test
  void prunePeerWhenReceivingTwiceTheSameFullMessage() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    MockMessageSender messageSender = new MockMessageSender();
    State state =
        new State(repo, Hash::keccak256, messageSender, messageListener, (message, peer) -> true, (peer) -> true);
    Peer peer = new PeerImpl();
    Peer secondPeer = new PeerImpl();
    Bytes message = Bytes32.random();
    String attributes = "{\"message_type\": \"BLOCK\"}";
    state.receiveGossipMessage(peer, attributes, message, Hash.keccak256(message));
    state.receiveGossipMessage(secondPeer, attributes, message, Hash.keccak256(message));
    assertEquals(2, repo.eagerPushPeers().size());
    assertEquals(0, repo.lazyPushPeers().size());
    assertNull(messageSender.payload);
    assertEquals(secondPeer, messageSender.peer);
    assertEquals(MessageSender.Verb.PRUNE, messageSender.verb);
  }
}
