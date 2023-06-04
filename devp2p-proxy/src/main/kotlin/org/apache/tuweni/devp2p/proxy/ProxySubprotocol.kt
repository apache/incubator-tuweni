// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.proxy

import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.SubProtocol
import org.apache.tuweni.rlpx.wire.SubProtocolClient
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier

class ProxySubprotocol : SubProtocol {

  companion object {
    val ID = SubProtocolIdentifier.of("pxy", 1, 3)
  }

  override fun id() = ID

  override fun supports(subProtocolIdentifier: SubProtocolIdentifier): Boolean = subProtocolIdentifier == ID

  override fun createHandler(service: RLPxService, client: SubProtocolClient) =
    ProxyHandler(service = service, client = client as ProxyClient)

  override fun createClient(service: RLPxService, subProtocolIdentifier: SubProtocolIdentifier) = ProxyClient(service)
}
