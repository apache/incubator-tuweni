// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.plumtree;

/**
 * A peer part of the gossip system.
 *
 * <p>Peers must implement #hashcode() and #equals(Peer)
 */
public interface Peer extends Comparable<Peer> {}
