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

  private static class MockMessageSender implements MessageSender {

    Verb verb;
    Peer peer;
    Bytes payload;

    @Override
    public void sendMessage(Verb verb, Peer peer, Bytes payload) {
      this.verb = verb;
      this.peer = peer;
      this.payload = payload;

    }
  }

  @Test
  void testInitialState() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    State state = new State(repo, Hash::keccak256, new MockMessageSender());
    assertTrue(repo.peers().isEmpty());
    assertTrue(repo.lazyPushPeers().isEmpty());
    assertTrue(repo.eagerPushPeers().isEmpty());
  }

  @Test
  void firstRoundWithThreePeers() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    State state = new State(repo, Hash::keccak256, new MockMessageSender());
    state.addPeer(new Peer());
    state.addPeer(new Peer());
    state.addPeer(new Peer());
    assertTrue(repo.lazyPushPeers().isEmpty());
    assertEquals(3, repo.eagerPushPeers().size());
  }

  @Test
  void removePeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    State state = new State(repo, Hash::keccak256, new MockMessageSender());
    Peer peer = new Peer();
    state.addPeer(peer);
    state.removePeer(peer);
    assertTrue(repo.peers().isEmpty());
    assertTrue(repo.lazyPushPeers().isEmpty());
    assertTrue(repo.eagerPushPeers().isEmpty());
  }

  @Test
  void prunePeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    State state = new State(repo, Hash::keccak256, new MockMessageSender());
    Peer peer = new Peer();
    state.addPeer(peer);
    state.receivePruneMessage(peer);
    assertEquals(0, repo.eagerPushPeers().size());
    assertEquals(1, repo.lazyPushPeers().size());
  }

  @Test
  void graftPeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    State state = new State(repo, Hash::keccak256, new MockMessageSender());
    Peer peer = new Peer();
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
    State state = new State(repo, Hash::keccak256, messageSender);
    Peer peer = new Peer();
    state.addPeer(peer);
    Peer otherPeer = new Peer();
    state.addPeer(otherPeer);
    Bytes32 msg = Bytes32.random();
    state.receiveFullMessage(peer, msg);
    assertEquals(msg, messageSender.payload);
    assertEquals(otherPeer, messageSender.peer);
  }

  @Test
  void receiveFullMessageFromEagerPeerWithALazyPeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    MockMessageSender messageSender = new MockMessageSender();
    State state = new State(repo, Hash::keccak256, messageSender);
    Peer peer = new Peer();
    state.addPeer(peer);
    Peer otherPeer = new Peer();
    state.addPeer(otherPeer);
    Bytes32 msg = Bytes32.random();
    Peer lazyPeer = new Peer();
    state.addPeer(lazyPeer);
    repo.moveToLazy(lazyPeer);
    state.receiveFullMessage(peer, msg);
    assertEquals(msg, messageSender.payload);
    assertEquals(otherPeer, messageSender.peer);
    state.processQueue();
    assertEquals(Hash.keccak256(msg), messageSender.payload);
    assertEquals(lazyPeer, messageSender.peer);
    assertEquals(MessageSender.Verb.IHAVE, messageSender.verb);
  }

  @Test
  void receiveFullMessageFromEagerPeerThenPartialMessageFromLazyPeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    MockMessageSender messageSender = new MockMessageSender();
    State state = new State(repo, Hash::keccak256, messageSender);
    Peer peer = new Peer();
    state.addPeer(peer);
    Peer lazyPeer = new Peer();
    state.addPeer(lazyPeer);
    repo.moveToLazy(lazyPeer);
    Bytes message = Bytes32.random();
    state.receiveFullMessage(peer, message);
    state.receiveHeaderMessage(lazyPeer, message);
    assertNull(messageSender.payload);
    assertNull(messageSender.peer);
  }

  @Test
  void receivePartialMessageFromLazyPeerAndNoFullMessage() throws Exception {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    MockMessageSender messageSender = new MockMessageSender();
    State state = new State(repo, Hash::keccak256, messageSender, 100, 4000);
    Peer peer = new Peer();
    state.addPeer(peer);
    Peer lazyPeer = new Peer();
    state.addPeer(lazyPeer);
    repo.moveToLazy(lazyPeer);
    Bytes message = Bytes32.random();
    state.receiveHeaderMessage(lazyPeer, message);
    Thread.sleep(200);
    assertEquals(message, messageSender.payload);
    assertEquals(lazyPeer, messageSender.peer);
    assertEquals(MessageSender.Verb.GRAFT, messageSender.verb);
  }

  @Test
  void receivePartialMessageFromLazyPeerAndThenFullMessage() throws Exception {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    MockMessageSender messageSender = new MockMessageSender();
    State state = new State(repo, Hash::keccak256, messageSender, 500, 4000);
    Peer peer = new Peer();
    state.addPeer(peer);
    Peer lazyPeer = new Peer();
    state.addPeer(lazyPeer);
    repo.moveToLazy(lazyPeer);
    Bytes message = Bytes32.random();
    state.receiveHeaderMessage(lazyPeer, Hash.keccak256(message));
    Thread.sleep(100);
    state.receiveFullMessage(peer, message);
    Thread.sleep(500);
    assertNull(messageSender.verb);
    assertNull(messageSender.payload);
    assertNull(messageSender.peer);
  }

  @Test
  void receiveFullMessageFromUnknownPeer() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    MockMessageSender messageSender = new MockMessageSender();
    State state = new State(repo, Hash::keccak256, messageSender);
    Peer peer = new Peer();
    Bytes message = Bytes32.random();
    state.receiveFullMessage(peer, message);
    assertEquals(1, repo.eagerPushPeers().size());
    assertEquals(0, repo.lazyPushPeers().size());
    assertEquals(peer, repo.eagerPushPeers().iterator().next());
  }

  @Test
  void prunePeerWhenReceivingTwiceTheSameFullMessage() {
    EphemeralPeerRepository repo = new EphemeralPeerRepository();
    MockMessageSender messageSender = new MockMessageSender();
    State state = new State(repo, Hash::keccak256, messageSender);
    Peer peer = new Peer();
    Peer secondPeer = new Peer();
    Bytes message = Bytes32.random();
    state.receiveFullMessage(peer, message);
    state.receiveFullMessage(secondPeer, message);
    assertEquals(1, repo.eagerPushPeers().size());
    assertEquals(1, repo.lazyPushPeers().size());
    assertNull(messageSender.payload);
    assertEquals(secondPeer, messageSender.peer);
    assertEquals(MessageSender.Verb.PRUNE, messageSender.verb);
  }
}
