// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.plumtree;

import org.apache.tuweni.bytes.Bytes;

import java.util.Map;

/** Listens to an incoming message, along with its attributes. */
public interface MessageListener {

  /**
   * Consumes a message
   *
   * @param messageBody the body of the message
   * @param attributes the attributes of the message
   * @param peer the peer we received the message from
   */
  public void listen(Bytes messageBody, Map<String, Bytes> attributes, Peer peer);
}
