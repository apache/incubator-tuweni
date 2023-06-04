// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
