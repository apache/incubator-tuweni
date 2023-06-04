// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.plumtree;

import java.util.Collection;
import java.util.List;

/** Repository of active peers associating with a gossip tree. */
public interface PeerRepository {

  void addEager(Peer peer);

  /**
   * Provides the list of all the peers connected.
   *
   * @return the list of peers
   */
  List<Peer> peers();

  /**
   * Provides the list of all lazy peers connected.
   *
   * @return the list of peers to push to lazily
   */
  Collection<Peer> lazyPushPeers();

  /**
   * Provides the list of all eager peers connected.
   *
   * @return the list of peers to push to eagerly
   */
  Collection<Peer> eagerPushPeers();

  /**
   * Removes a peer from the repository
   *
   * @param peer the peer to remove
   */
  void removePeer(Peer peer);

  /**
   * Moves a peer to the list of lazy peers
   *
   * @param peer the peer to move
   * @return true if the move was effective
   */
  boolean moveToLazy(Peer peer);

  /**
   * Moves a peer to the list of eager peers.
   *
   * @param peer the peer to move
   */
  void moveToEager(Peer peer);

  /**
   * Proposes a peer as a new peer.
   *
   * @param peer a peer to be considered for addition
   */
  void considerNewPeer(Peer peer);
}
