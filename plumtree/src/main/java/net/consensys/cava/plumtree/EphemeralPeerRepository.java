/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.plumtree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EphemeralPeerRepository implements PeerRepository {

  private final Set<Peer> eagerPushPeers = Collections.synchronizedSet(new HashSet<>());
  private final Set<Peer> lazyPushPeers = Collections.synchronizedSet(new HashSet<>());

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
  public void moveToLazy(Peer peer) {
    eagerPushPeers.remove(peer);
    lazyPushPeers.add(peer);
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
