// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.plumtree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory peer repository.
 */
public final class EphemeralPeerRepository implements PeerRepository {

  private final Set<Peer> eagerPushPeers = ConcurrentHashMap.newKeySet();
  private final Set<Peer> lazyPushPeers = ConcurrentHashMap.newKeySet();

  @Override
  public void addEager(Peer peer) {
    eagerPushPeers.add(peer);
  }

  @Override
  public List<Peer> peers() {
    List<Peer> peers = new ArrayList<>(eagerPushPeers);
    peers.addAll(lazyPushPeers);
    return peers;
  }

  @Override
  public Collection<Peer> lazyPushPeers() {
    return lazyPushPeers;
  }

  @Override
  public Collection<Peer> eagerPushPeers() {
    return eagerPushPeers;
  }

  @Override
  public void removePeer(Peer peer) {
    lazyPushPeers.remove(peer);
    eagerPushPeers.remove(peer);
  }

  @Override
  public boolean moveToLazy(Peer peer) {
    eagerPushPeers.remove(peer);
    lazyPushPeers.add(peer);
    return true;
  }

  @Override
  public void moveToEager(Peer peer) {
    lazyPushPeers.remove(peer);
    eagerPushPeers.add(peer);
  }

  @Override
  public void considerNewPeer(Peer peer) {
    if (!lazyPushPeers.contains(peer)) {
      eagerPushPeers.add(peer);
    }

  }
}
