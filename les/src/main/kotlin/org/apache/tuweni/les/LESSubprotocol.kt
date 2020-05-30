/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.les

import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.SubProtocol
import org.apache.tuweni.rlpx.wire.SubProtocolHandler
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier
import org.apache.tuweni.units.bigints.UInt256

/**
 * The LES subprotocol entry point class, to be used in conjunction with RLPxService
 *
 *
 * This subprotocol is implemented after the specification presented on the *
 * [Ethereum wiki.](https://github.com/ethereum/wiki/wiki/Light-client-protocol)
 *
 * @see org.apache.tuweni.rlpx.RLPxService
 */
class LESSubprotocol
/**
 * Default constructor.
 *
 * @param networkId the identifier, as an integer of the chain to connect to. 0 for testnet, 1 for mainnet.
 * @param blockStore the key-value store for blocks
 * @param blockHeaderStore the key-value store for block headers
 */(
   private val networkId: Int,
   private val serveHeaders: Boolean,
   private val serveChainSince: UInt256,
   private val serveStateSince: UInt256,
   private val flowControlBufferLimit: UInt256,
   private val flowControlMaximumRequestCostTable: UInt256,
   private val flowControlMinimumRateOfRecharge: UInt256,
   private val repo: BlockchainRepository
 ) : SubProtocol {

  override fun getCapabilities(): MutableList<SubProtocolIdentifier> = mutableListOf(SubProtocolIdentifier.of("les", 2))

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
