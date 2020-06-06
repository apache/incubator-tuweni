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
package org.apache.tuweni.devp2p.eth

import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.CompletableAsyncCompletion
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash

interface EthRequestsManager {
  fun requestBlockHeader(blockHash: Hash): AsyncCompletion
  fun requestBlockHeaders(blockHashes: List<Hash>): AsyncCompletion
  fun requestBlockHeaders(blockHash: Hash, maxHeaders: Long, skip: Long, reverse: Boolean): AsyncCompletion
  fun requestBlockHeaders(blockNumber: Long, maxHeaders: Long, skip: Long, reverse: Boolean): AsyncCompletion

  fun requestBlockBodies(blockHashes: List<Hash>)

  fun requestBlock(blockHash: Hash)

  fun wasRequested(connectionId: String, header: BlockHeader): CompletableAsyncCompletion?
  fun wasRequested(connectionId: String, bodies: List<BlockBody>): List<Hash>?
}
