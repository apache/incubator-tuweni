// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.les

import org.apache.tuweni.concurrent.AsyncCompletion

internal class LESPeerState {

  val ready = AsyncCompletion.incomplete()
  var ourStatusMessage: StatusMessage? = null
  var peerStatusMessage: StatusMessage? = null

  fun handshakeComplete(): Boolean {
    ourStatusMessage?.let {
      peerStatusMessage?.let {
        return true
      }
    }
    return false
  }
}
