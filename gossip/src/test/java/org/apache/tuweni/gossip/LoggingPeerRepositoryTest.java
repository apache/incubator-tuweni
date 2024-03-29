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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.plumtree.Peer;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;


class LoggingPeerRepositoryTest {

  private static final Peer FOO = new Peer() {

    @Override
    public int compareTo(@NotNull Peer o) {
      return -1;
    }
  };

  @Test
  void testAddEagerPeer() {
    LoggingPeerRepository repo = new LoggingPeerRepository();
    assertTrue(repo.eagerPushPeers().isEmpty());
    repo.addEager(FOO);
    assertEquals(FOO, repo.eagerPushPeers().iterator().next());
  }

  @Test
  void testAddLazyPeer() {
    LoggingPeerRepository repo = new LoggingPeerRepository();
    assertTrue(repo.lazyPushPeers().isEmpty());
    repo.addEager(FOO);
    repo.moveToLazy(FOO);
    assertEquals(FOO, repo.lazyPushPeers().iterator().next());
  }

  @Test
  void testAddEagerLazyEager() {
    LoggingPeerRepository repo = new LoggingPeerRepository();
    assertTrue(repo.lazyPushPeers().isEmpty());
    repo.addEager(FOO);
    repo.moveToLazy(FOO);
    repo.moveToEager(FOO);
    assertEquals(FOO, repo.eagerPushPeers().iterator().next());
  }

  @Test
  void testRemove() {
    LoggingPeerRepository repo = new LoggingPeerRepository();
    assertTrue(repo.lazyPushPeers().isEmpty());
    repo.addEager(FOO);
    repo.removePeer(FOO);
    assertTrue(repo.eagerPushPeers().isEmpty());
    assertTrue(repo.lazyPushPeers().isEmpty());
  }

  @Test
  void testRemoveLazy() {
    LoggingPeerRepository repo = new LoggingPeerRepository();
    assertTrue(repo.lazyPushPeers().isEmpty());
    repo.addEager(FOO);
    repo.moveToLazy(FOO);
    repo.removePeer(FOO);
    assertTrue(repo.eagerPushPeers().isEmpty());
    assertTrue(repo.lazyPushPeers().isEmpty());
  }

  @Test
  void testConsiderPeer() {
    LoggingPeerRepository repo = new LoggingPeerRepository();
    assertTrue(repo.lazyPushPeers().isEmpty());
    repo.considerNewPeer(FOO);
    assertEquals(FOO, repo.eagerPushPeers().iterator().next());
    repo.moveToLazy(FOO);
    repo.considerNewPeer(FOO);
    assertTrue(repo.eagerPushPeers().isEmpty());
    assertEquals(FOO, repo.lazyPushPeers().iterator().next());
  }
}
