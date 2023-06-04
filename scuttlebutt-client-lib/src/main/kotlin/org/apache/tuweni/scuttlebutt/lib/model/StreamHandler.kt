// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib.model

/**
 * A handler consuming a stream.
 *
 * @param <T> the type of stream
</T> */
interface StreamHandler<T> {
  /**
   * Handles a new item from the result stream.
   *
   * @param item the item appearing in the stream
   */
  fun onMessage(item: T)

  /**
   * Invoked when the stream has been closed.
   */
  fun onStreamEnd()

  /**
   * Invoked when there is an error in the stream.
   *
   * @param ex the underlying error
   */
  fun onStreamError(ex: Exception?)
}
