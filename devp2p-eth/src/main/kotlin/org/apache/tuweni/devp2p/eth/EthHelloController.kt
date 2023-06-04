// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.eth

import org.apache.tuweni.rlpx.wire.WireConnection

/**
 * Controller managing the state of the ETH or LES subprotocol handlers.
 */
class EthHelloController(
  val connectionsListener: (WireConnection, Status) -> Unit = { _, _ -> },
) {

  fun receiveStatus(connection: WireConnection, status: Status) {
    connectionsListener(connection, status)
  }
}
