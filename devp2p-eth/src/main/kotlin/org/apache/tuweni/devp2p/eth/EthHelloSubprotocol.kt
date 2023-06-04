// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.eth

import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.SubProtocol
import org.apache.tuweni.rlpx.wire.SubProtocolClient
import org.apache.tuweni.rlpx.wire.SubProtocolHandler
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier
import org.apache.tuweni.rlpx.wire.WireConnection
import kotlin.coroutines.CoroutineContext

class EthHelloSubprotocol(
  private val coroutineContext: CoroutineContext = Dispatchers.Default,
  private val blockchainInfo: BlockchainInformation,
  private val listener: (WireConnection, Status) -> Unit = { _, _ -> },
) : SubProtocol {

  companion object {
    val ETH62 = SubProtocolIdentifier.of("eth", 62, 8)
    val ETH63 = SubProtocolIdentifier.of("eth", 63, 17)
    val ETH64 = SubProtocolIdentifier.of("eth", 64, 17)
    val ETH65 = SubProtocolIdentifier.of("eth", 65, 17)
  }

  override fun id(): SubProtocolIdentifier = ETH65

  override fun supports(subProtocolIdentifier: SubProtocolIdentifier): Boolean {
    return "eth".equals(subProtocolIdentifier.name()) && (
      subProtocolIdentifier.version() == ETH62.version() ||
        subProtocolIdentifier.version() == ETH63.version() || subProtocolIdentifier.version() == ETH64.version() ||
        subProtocolIdentifier.version() == ETH65.version()
      )
  }

  override fun createHandler(service: RLPxService, client: SubProtocolClient): SubProtocolHandler {
    val controller = EthHelloController(listener)
    return EthHelloHandler(coroutineContext, blockchainInfo, service, controller)
  }

  override fun getCapabilities() = mutableListOf(ETH62, ETH63, ETH64, ETH65)

  override fun createClient(service: RLPxService, identifier: SubProtocolIdentifier): SubProtocolClient {
    return EthHelloClient(service)
  }
}
