// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.plumtree;

import org.apache.tuweni.bytes.Bytes;

/**
 * Validator for a message and a peer.
 *
 * This validator is called prior to gossiping the message from that peer to other peers.
 */
public interface MessageValidator {

  /**
   * Validates that the message from the peer is valid.
   *
   * @param message the payload sent over the network
   * @param peer the peer that sent the message
   * @return true if the message is valid
   */
  boolean validate(Bytes message, Peer peer);
}
