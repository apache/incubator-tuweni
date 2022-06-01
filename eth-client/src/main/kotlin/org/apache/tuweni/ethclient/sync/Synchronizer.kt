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
package org.apache.tuweni.ethclient.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.apache.tuweni.devp2p.eth.EthRequestsManager
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.ethclient.EthereumPeerRepository
import org.apache.tuweni.units.bigints.UInt256
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

val logger = LoggerFactory.getLogger(Synchronizer::class.java)

abstract class Synchronizer(
  val executor: ExecutorService = Executors.newFixedThreadPool(1),
  override val coroutineContext: CoroutineContext = executor.asCoroutineDispatcher(),
  val repository: BlockchainRepository,
  val client: EthRequestsManager,
  val peerRepository: EthereumPeerRepository,
  val from: UInt256?,
  val to: UInt256?
) : CoroutineScope {
  abstract fun start()
  abstract fun stop()

  fun addHeaders(result: List<BlockHeader>) {
    launch {
      logger.info("Receiving ${result.size} headers - first ${result.firstOrNull()?.hash}")
      val bodiesToRequest = mutableListOf<Hash>()
      result.map { header ->
        async {
          repository.storeBlockHeader(header)
          if (!repository.hasBlockBody(header.hash)) {
            bodiesToRequest.add(header.hash)
          }
        }
      }.awaitAll()
      if (!bodiesToRequest.isEmpty()) {
        logger.info("Requesting ${bodiesToRequest.size} block bodies")
        client.requestBlockBodies(bodiesToRequest)
      } else {
        logger.info("No bodies requested")
      }
    }
  }
}
