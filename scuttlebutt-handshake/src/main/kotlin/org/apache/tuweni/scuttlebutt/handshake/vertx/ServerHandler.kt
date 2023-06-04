// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.handshake.vertx

import org.apache.tuweni.bytes.Bytes

/**
 * Handler managing a stream over SecureScuttlebutt originating from the Vert.x server
 */
interface ServerHandler {
  /**
   * This method is called by the server when a new message arrives from the client.
   *
   * @param message the new message after box decoding.
   */
  fun receivedMessage(message: Bytes?)

  /**
   * Notifies the handler the stream is no longer active.
   */
  fun streamClosed()
}
