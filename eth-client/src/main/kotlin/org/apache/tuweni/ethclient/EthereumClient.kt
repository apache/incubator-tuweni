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
package org.apache.tuweni.ethclient

import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.NIOFSDirectory
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.eth.EthSubprotocol
import org.apache.tuweni.devp2p.eth.SimpleBlockchainInformation
import org.apache.tuweni.eth.genesis.GenesisFile
import org.apache.tuweni.eth.repository.BlockchainIndex
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.kv.LevelDBKeyValueStore
import org.apache.tuweni.peer.repository.PeerRepository
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.vertx.VertxRLPxService
import org.apache.tuweni.units.bigints.UInt256
import kotlin.coroutines.CoroutineContext

/**
 * Top-level class to run an Ethereum client.
 */
@UseExperimental(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EthereumClient(
  val vertx: Vertx,
  val config: EthereumClientConfig,
  override val coroutineContext: CoroutineContext = Dispatchers.Unconfined
) : CoroutineScope {

  private val genesisFiles = HashMap<String, GenesisFile>()
  private val services = HashMap<String, RLPxService>()
  private val repositories = HashMap<String, BlockchainRepository>()
  private val peerRepositories = HashMap<String, PeerRepository>()

  suspend fun start() {
    config.peerRepositories().forEach {
      peerRepositories[it.getName()] = it.peerRepository()
    }

    config.genesisFiles().forEach {
      genesisFiles[it.getName()] = it.genesisFile()
    }

    val repoToGenesisFile = HashMap<BlockchainRepository, GenesisFile>()

    config.dataStores().forEach() { dataStore ->
      val genesisFile = genesisFiles[dataStore.getGenesisFile()]
      val genesisBlock = genesisFile!!.toBlock()
      val dataStorage = dataStore.getStoragePath()
      val index = NIOFSDirectory(dataStorage.resolve("index"))
      val analyzer = StandardAnalyzer()
      val config = IndexWriterConfig(analyzer)
      val writer = IndexWriter(index, config)
      val repository = BlockchainRepository.init(
        LevelDBKeyValueStore.open(dataStorage.resolve("bodies")),
        LevelDBKeyValueStore.open(dataStorage.resolve("headers")),
        LevelDBKeyValueStore.open(dataStorage.resolve("metadata")),
        LevelDBKeyValueStore.open(dataStorage.resolve("transactionReceipts")),
        LevelDBKeyValueStore.open(dataStorage.resolve("transactions")),
        LevelDBKeyValueStore.open(dataStorage.resolve("state")),
        BlockchainIndex(writer),
        genesisBlock
      )
      repositories[dataStore.getName()] = repository
      repoToGenesisFile[repository] = genesisFile
    }

    AsyncCompletion.allOf(config.rlpxServices().map { rlpxConfig ->
      val peerRepository = peerRepositories[rlpxConfig.peerRepository()]
      val repository = repositories[rlpxConfig.repository()]
      val genesisFile = repoToGenesisFile[repository]
      val genesisBlock = repository!!.retrieveGenesisBlock()
      val service = VertxRLPxService(
        vertx,
        rlpxConfig.port(),
        rlpxConfig.networkInterface(),
        rlpxConfig.advertisedPort(),
        SECP256K1.KeyPair.random(),
        listOf(
          EthSubprotocol(
            repository = repository,
            blockchainInfo = SimpleBlockchainInformation(
              UInt256.valueOf(genesisFile!!.chainId.toLong()), genesisBlock.header.difficulty,
              genesisBlock.header.hash, genesisBlock.header.hash, genesisFile.forks
            )
          )
        ),
        rlpxConfig.clientName(),
        WireConnectionPeerRepositoryAdapter(peerRepository!!)
      )
      services[rlpxConfig.getName()] = service
      service.start()
    }).await()

    Runtime.getRuntime().addShutdownHook(object : Thread() {
      override fun run() = this@EthereumClient.stop()
    })
  }

  fun stop() = runBlocking {
    vertx.close()
    AsyncCompletion.allOf(services.values.map(RLPxService::stop)).await()
    repositories.values.forEach(BlockchainRepository::close)
  }

//  private suspend fun requestPeerConnections() {
//    logger.info("Adding new peers, {} known peers", this.enrs.size)
//    val timeout = Instant.now().minus(5, ChronoUnit.MINUTES)
//    val completions = mutableListOf<AsyncResult<WireConnection>>()
//    for (service in services.values) {
//      var peerRequests = 0
//      for (enr in enrs.entries.stream().unordered()) {
//        if (enr.value.lastContacted == null || enr.value.lastContacted!!.isBefore(timeout)) {
//          peerRequests++
//          enr.value.lastContacted = Instant.now()
//          val connectAwait =
//            service.connectTo(enr.key.publicKey(), InetSocketAddress(enr.key.ip(), enr.key.tcp())).thenApply {
//              enr.value.connectionId = it.id()
//              enr.value.service = service
//              connectionsToENRs[it.id()] = enr.key
//              it
//            }
//          completions.add(connectAwait)
//          if (peerRequests >= 30) {
//            logger.info("Made over 30 peer requests, moving to next service")
//            break
//          }
//        }
//      }
//    }
//    try {
//      logger.info("Making a total of {} peer connections", completions.size)
//      AsyncResult.allOf(completions).await()
//    } catch (e: CancellationException) {
//      logger.error("Cancellation exception reported", e)
//    }
//  }
//
//  private fun handleStatusUpdate(conn: WireConnection, status: StatusMessage) {
//    connectionsToENRs[conn.id()]?.let {
//      enrs[it]?.let {
//        it.status = status
//      }
//    }
//  }
}
