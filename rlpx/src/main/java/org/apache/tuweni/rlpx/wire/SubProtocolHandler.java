// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx.wire;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.concurrent.AsyncCompletion;

/**
 * Handler managing messages and new connections of peers related for a given subprotocol.
 */
public interface SubProtocolHandler {

  /**
   * Handle an incoming wire protocol message
   *
   * @param connection the peer connection
   * @param messageType the type of the message
   * @param message the message to be handled
   * @return a handle tracking the completion of the handling of the message.
   */
  AsyncCompletion handle(WireConnection connection, int messageType, Bytes message);

  /**
   * Handle a new peer connection
   * 
   * @param connection the new peer connection
   * @return a handle to the completion of the addition of the new peer.
   */
  AsyncCompletion handleNewPeerConnection(WireConnection connection);

  /**
   * Stops a subprotocol operations.
   *
   * @return a handle to track when the subprotocol operations have stopped
   */
  AsyncCompletion stop();
}
