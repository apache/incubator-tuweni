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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.NIOFSDirectory
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.eth.EthSubprotocol
import org.apache.tuweni.devp2p.eth.SimpleBlockchainInformation
import org.apache.tuweni.devp2p.eth.StatusMessage
import org.apache.tuweni.discovery.DNSDaemon
import org.apache.tuweni.eth.genesis.GenesisFile
import org.apache.tuweni.eth.repository.BlockchainIndex
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.kv.LevelDBKeyValueStore
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.vertx.VertxRLPxService
import org.apache.tuweni.rlpx.wire.WireConnection
import org.apache.tuweni.units.bigints.UInt256
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.file.Path
import java.nio.file.Paths
import java.security.Security
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * Main method run by the Ethereum client application.
 */
fun main(args: Array<String>) = runBlocking {
  Security.addProvider(BouncyCastleProvider())
  val client = EthClient()
  client.run(Vertx.vertx(), Paths.get("data"))
}

/**
 * Top-level class to run an Ethereum client.
 */
@UseExperimental(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EthClient(override val coroutineContext: CoroutineContext = Dispatchers.Unconfined) : CoroutineScope {

  companion object {
    val logger = LoggerFactory.getLogger(EthClient::class.java)
  }

  private val connectionsToENRs: MutableMap<String, EthereumNodeRecord> = ConcurrentHashMap()
  private val enrs: MutableMap<EthereumNodeRecord, PeerStatus> = ConcurrentHashMap()
  private val services = ArrayList<RLPxService>()

  internal suspend fun run(vertx: Vertx, dataStorage: Path) {
    val contents = EthClient::class.java.getResourceAsStream("/mainnet.json").readAllBytes()
    val genesisFile = GenesisFile.read(contents)
    val genesisBlock = genesisFile.toBlock()
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
    AsyncCompletion.allOf((0..4).map {
      val service = VertxRLPxService(
        vertx, 0, "0.0.0.0", 30303, SECP256K1.KeyPair.random(),
        listOf(
          EthSubprotocol(
            repository = repository,
            blockchainInfo = SimpleBlockchainInformation(
              UInt256.valueOf(genesisFile.chainId.toLong()), genesisBlock.header.difficulty,
              genesisBlock.header.hash, genesisBlock.header.hash, genesisFile.forks
            ),
            listener = this::handleStatusUpdate
          )
        ),
        "Apache Tuweni"
      )
      services.add(service)
      service.start()
    }).await()

    val dnsDaemon = DNSDaemon(
      enrLink = "enrtree://AKA3AM6LPBYEUDMVNU3BSVQJ5AD45Y7YPOHJLEF6W26QOE4VTUDPE@all.mainnet.ethdisco.net",
      period = 30000,
      listeners = setOf { enrs ->

        enrs.forEach { this.enrs[it] = PeerStatus(null, null, null, null) }
      }
    )

    vertx.setPeriodic(10000) {
      printServices()
    }

    Runtime.getRuntime().addShutdownHook(object : Thread() {
      override fun run() = runBlocking {
        vertx.close()
        dnsDaemon.close()
        AsyncCompletion.allOf(services.map(RLPxService::stop)).await()
        repository.close()
      }
    })
  }

  private suspend fun requestPeerConnections() {
    logger.info("Adding new peers, {} known peers", this.enrs.size)
    val timeout = Instant.now().minus(5, ChronoUnit.MINUTES)
    val completions = mutableListOf<AsyncResult<WireConnection>>()
    for (service in services) {
      var peerRequests = 0
      for (enr in enrs.entries.stream().unordered()) {
        if (enr.value.lastContacted == null || enr.value.lastContacted!!.isBefore(timeout)) {
          peerRequests++
          enr.value.lastContacted = Instant.now()
          val connectAwait =
            service.connectTo(enr.key.publicKey(), InetSocketAddress(enr.key.ip(), enr.key.tcp())).thenApply {
              enr.value.connectionId = it.id()
              enr.value.service = service
              connectionsToENRs[it.id()] = enr.key
              it
            }
          completions.add(connectAwait)
          if (peerRequests >= 30) {
            logger.info("Made over 30 peer requests, moving to next service")
            break
          }
        }
      }
    }
    try {
      logger.info("Making a total of {} peer connections", completions.size)
      AsyncResult.allOf(completions).await()
    } catch (e: CancellationException) {
      logger.error("Cancellation exception reported", e)
    }
  }

  private fun handleStatusUpdate(conn: WireConnection, status: StatusMessage) {
    connectionsToENRs[conn.id()]?.let {
      enrs[it]?.let {
        it.status = status
      }
    }
  }

  private fun printServices() {
    var counter = 0
    enrs.forEach { record, status ->
      if (status.lastContacted != null && status.connectionId != null) {
        val conn = status.service!!.repository()[status.connectionId!!]
        conn?.let {
          if (it.disconnectReason == null) {
            counter++
          }
          logger.debug(
            "Connected with {}, lastContacted: {}, disconnectReason: {}",
            record.ip(),
            status.lastContacted,
            it.disconnectReason?.text
          )
        }
      }
    }
    logger.info("{} active peers", counter)
    if (counter < 50) {
      logger.info("Missing {} peers, requesting more", 50 - counter)
      async {
        requestPeerConnections()
      }
    }
  }
}
