// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.plumtree;

/**
 * Interface to decide whether to prune peers when they send messages late.
 *
 * <p>Pruned peers become "lazy peers". They send message attestations (IHAVE).
 */
public interface PeerPruning {

  /**
   * Decides whether to prune a peer
   *
   * @param peer the peer to consider
   * @return true if the peer should be pruned
   */
  boolean prunePeer(Peer peer);
}
