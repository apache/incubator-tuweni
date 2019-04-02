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

import net.consensys.cava.bytes.Bytes32
import net.consensys.cava.eth.BlockBody
import net.consensys.cava.eth.BlockHeader
import net.consensys.cava.eth.Hash
import net.consensys.cava.eth.TransactionReceipt

/**
 * Calls to LES functions from the point of view of the consumer of the subprotocol.
 *
 * When executing those calls, the client will store all data transferred in the blockchain repository.
 *
 *
 */
interface LightClient {

  /**
   * Get block headers from remote peers.
   *
   * @param blockNumberOrHash the block number or the hash to start to look for headers from
   * @param maxHeaders maximum number of headers to return
   * @param skip the number of block apart to skip when returning headers
   * @param reverse if true, walk the chain in descending order
   */
  fun getBlockHeaders(
    blockNumberOrHash: Bytes32,
    maxHeaders: Int = 10,
    skip: Int = 0,
    reverse: Boolean = false
  ): List<BlockHeader>

  /**
   * Get block bodies from remote peers.
   *
   * @param blockHashes hashes identifying block bodies
   */
  fun getBlockBodies(vararg blockHashes: Hash): List<BlockBody>

  /**
   * Get transaction receipts from remote peers for blocks.
   *
   * @param blockHashes hashes identifying blocks
   */
  fun getReceipts(vararg blockHashes: Hash): List<List<TransactionReceipt>>
}
