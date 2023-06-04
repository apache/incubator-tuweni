// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.les

import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.devp2p.eth.BlockchainInformation
import org.apache.tuweni.devp2p.eth.ConnectionSelectionStrategy
import org.apache.tuweni.devp2p.eth.EthClient
import org.apache.tuweni.devp2p.eth.EthController
import org.apache.tuweni.devp2p.eth.EthRequestsManager
import org.apache.tuweni.devp2p.eth.Status
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.TransactionPool
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.SubProtocol
import org.apache.tuweni.rlpx.wire.SubProtocolClient
import org.apache.tuweni.rlpx.wire.SubProtocolHandler
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier
import org.apache.tuweni.rlpx.wire.WireConnection
import org.apache.tuweni.units.bigints.UInt256
import kotlin.coroutines.CoroutineContext

/**
 * The LES subprotocol entry point class, to be used in conjunction with RLPxService
 *
 *
 * This subprotocol is implemented after the specification presented on the *
 * [Ethereum wiki.](https://github.com/ethereum/wiki/wiki/Light-client-protocol)
 *
 * @see org.apache.tuweni.rlpx.RLPxService
 * @param coroutineContext the Kotlin coroutine context
 * @param blockchainInfo blockchain information to send to peers
 * @param serveHeaders whether to serve headers
 * @param serveChainSince block number at which to start serving blocks
 * @param serveStateSince block number at which to start serving state
 * @param flowControlBufferLimit limit of bytes to send
 * @param flowControlMaximumRequestCostTable cost table for control flow
 * @param flowControlMinimumRateOfRecharge rate of recharge for cost
 * @param repo the blockchain repository this subprotocol will serve data from
 * @param listener a listener for new connections when a status message is provided
 */
class LESSubprotocol(
  private val coroutineContext: CoroutineContext = Dispatchers.Default,
  private val blockchainInfo: BlockchainInformation,
  private val serveHeaders: Boolean,
  private val serveChainSince: UInt256,
  private val serveStateSince: UInt256,
  private val flowControlBufferLimit: UInt256,
  private val flowControlMaximumRequestCostTable: UInt256,
  private val flowControlMinimumRateOfRecharge: UInt256,
  private val repo: BlockchainRepository,
  private val pendingTransactionsPool: TransactionPool,
  private val connectionSelectionStrategy: ConnectionSelectionStrategy,
  private val listener: (WireConnection, Status) -> Unit = { _, _ -> }
) : SubProtocol {

  override fun createClient(service: RLPxService, subProtocolIdentifier: SubProtocolIdentifier): SubProtocolClient {
    return EthClient(service, pendingTransactionsPool, connectionSelectionStrategy)
  }

  override fun id(): SubProtocolIdentifier {
    return LES_ID
  }

  override fun supports(subProtocolIdentifier: SubProtocolIdentifier): Boolean {
    return "les" == subProtocolIdentifier.name() && subProtocolIdentifier.version() == 2
  }

  override fun createHandler(service: RLPxService, client: SubProtocolClient): SubProtocolHandler {
    val controller = EthController(repo, pendingTransactionsPool, client as EthRequestsManager, listener)
    return LESSubProtocolHandler(
      service,
      LES_ID,
      blockchainInfo,
      serveHeaders,
      serveChainSince,
      serveStateSince,
      flowControlBufferLimit,
      flowControlMaximumRequestCostTable,
      flowControlMinimumRateOfRecharge,
      controller,
      coroutineContext
    )
  }

  companion object {
    internal val LES_ID = SubProtocolIdentifier.of("les", 2, 21)
  }
}
