/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.les

import kotlinx.coroutines.Dispatchers
import net.consensys.cava.eth.repository.BlockchainRepository
import net.consensys.cava.rlpx.RLPxService
import net.consensys.cava.rlpx.wire.SubProtocol
import net.consensys.cava.rlpx.wire.SubProtocolHandler
import net.consensys.cava.rlpx.wire.SubProtocolIdentifier
import net.consensys.cava.units.bigints.UInt256

/**
 * The LES subprotocol entry point class, to be used in conjunction with RLPxService
 *
 *
 * This subprotocol is implemented after the specification presented on the *
 * [Ethereum wiki.](https://github.com/ethereum/wiki/wiki/Light-client-protocol)
 *
 * @see net.consensys.cava.rlpx.RLPxService
 */
class LESSubprotocol
/**
 * Default constructor.
 *
 * @param networkId the identifier, as an integer of the chain to connect to. 0 for testnet, 1 for mainnet.
 * @param blockStore the key-value store for blocks
 * @param blockHeaderStore the key-value store for block headers
 */
  (
    private val networkId: Int,
    private val serveHeaders: Boolean,
    private val serveChainSince: UInt256,
    private val serveStateSince: UInt256,
    private val flowControlBufferLimit: UInt256,
    private val flowControlMaximumRequestCostTable: UInt256,
    private val flowControlMinimumRateOfRecharge: UInt256,
    private val repo: BlockchainRepository
  ) : SubProtocol {

  override fun id(): SubProtocolIdentifier {
    return LES_ID
  }

  override fun supports(subProtocolIdentifier: SubProtocolIdentifier): Boolean {
    return "les" == subProtocolIdentifier.name() && subProtocolIdentifier.version() == 2
  }

  override fun versionRange(version: Int): Int {
    return 21
  }

  override fun createHandler(service: RLPxService): SubProtocolHandler {
    return LESSubProtocolHandler(
      service,
      LES_ID,
      networkId,
      serveHeaders,
      serveChainSince,
      serveStateSince,
      flowControlBufferLimit,
      flowControlMaximumRequestCostTable,
      flowControlMinimumRateOfRecharge,
      repo,
      Dispatchers.Default
    )
  }

  companion object {

    internal val LES_ID = SubProtocolIdentifier.of("les", 2)
  }
}
