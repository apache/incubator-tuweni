// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.eth

import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.SubProtocolClient

/**
 * Client of the ETH subprotocol, allowing to request block and node data
 */
class EthHelloClient(
  private val service: RLPxService,
) : SubProtocolClient
