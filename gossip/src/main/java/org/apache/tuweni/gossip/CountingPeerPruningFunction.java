// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.gossip;

import org.apache.tuweni.plumtree.Peer;
import org.apache.tuweni.plumtree.PeerPruning;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Function counting the number of times a peer showed. */
final class CountingPeerPruningFunction implements PeerPruning {

  private final int numberOfOccurrences;
  private final Map<Peer, Integer> countingOccurrences =
      Collections.synchronizedMap(new WeakHashMap<>());

  public CountingPeerPruningFunction(int numberOfOccurrences) {
    this.numberOfOccurrences = numberOfOccurrences;
  }

  @Override
  public boolean prunePeer(Peer peer) {
    Integer currentValue = countingOccurrences.putIfAbsent(peer, 1);
    if (currentValue != null) {
      if (currentValue + 1 >= numberOfOccurrences) {
        countingOccurrences.remove(peer);
        return true;
      }
      countingOccurrences.put(peer, currentValue + 1);
    }

    return false;
  }
}
