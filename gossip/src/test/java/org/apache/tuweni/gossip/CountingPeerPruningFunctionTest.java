// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.gossip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.plumtree.Peer;

import org.junit.jupiter.api.Test;

class CountingPeerPruningFunctionTest {

  @Test
  void testPruning() {
    CountingPeerPruningFunction fn = new CountingPeerPruningFunction(3);
    Peer peer = o -> -1;
    assertFalse(fn.prunePeer(peer));
    assertFalse(fn.prunePeer(peer));
    assertTrue(fn.prunePeer(peer));
  }
}
