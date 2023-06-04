// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.rpc.mux

import org.apache.tuweni.scuttlebutt.rpc.RPCResponse

/**
 * Handles incoming items from a result stream
 */
interface ScuttlebuttStreamHandler {
  /**
   * Handles a new message from the result stream.
   *
   * @param message a new message appearing.
   */
  fun onMessage(message: RPCResponse)

  /**
   * Invoked when the stream has been closed.
   */
  fun onStreamEnd()

  /**
   * Invoked when there is an error in the stream.
   *
   * @param ex the underlying error
   */
  fun onStreamError(ex: Exception)
}
