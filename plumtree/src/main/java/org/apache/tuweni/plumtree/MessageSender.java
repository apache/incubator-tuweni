// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.plumtree;

import org.apache.tuweni.bytes.Bytes;

import java.util.Map;
import javax.annotation.Nullable;

/**
 * Interface to sending messages to other peers.
 */
public interface MessageSender {

  /**
   * Types of verbs supported by the dialect
   */
  enum Verb {
    /**
     * Indicates we have a message
     */
    IHAVE,
    /**
     * Ask to be added back to eager peers
     */
    GRAFT,
    /**
     * Ask to be removed from eager peers
     */
    PRUNE,
    /**
     * Gossip a message
     */
    GOSSIP,
    /**
     * Send a direct message
     */
    SEND
  }

  /**
   * Sends bytes to a peer.
   * 
   * @param verb the type of message
   * @param attributes the attributes of message
   * @param peer the target of the message
   * @param hash the hash of the message
   * @param payload the bytes to send
   */
  void sendMessage(Verb verb, Map<String, Bytes> attributes, Peer peer, Bytes hash, @Nullable Bytes payload);

}
