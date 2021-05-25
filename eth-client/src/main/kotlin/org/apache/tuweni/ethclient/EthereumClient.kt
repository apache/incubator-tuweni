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
import org.apache.tuweni.devp2p.eth.EthRequestsManager
import org.apache.tuweni.devp2p.eth.EthSubprotocol
import org.apache.tuweni.devp2p.eth.EthSubprotocol.Companion.ETH66
import org.apache.tuweni.devp2p.eth.SimpleBlockchainInformation
import org.apache.tuweni.devp2p.proxy.ProxyClient
import org.apache.tuweni.devp2p.proxy.ProxySubprotocol
import org.apache.tuweni.devp2p.proxy.target.TcpEndpoint
import org.apache.tuweni.eth.genesis.GenesisFile
import org.apache.tuweni.eth.repository.BlockchainIndex
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.MemoryTransactionPool
import org.apache.tuweni.kv.LevelDBKeyValueStore
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.vertx.VertxRLPxService
import org.apache.tuweni.units.bigints.UInt256
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.URI
import kotlin.coroutines.CoroutineContext

/**
 * Top-level class to run an Ethereum client.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EthereumClient(
  val vertx: Vertx,
  val config: EthereumClientConfig,
  override val coroutineContext: CoroutineContext = Dispatchers.Unconfined
) : CoroutineScope {

  companion object {
    val logger = LoggerFactory.getLogger(EthereumClient::class.java)
  }

  private val genesisFiles = HashMap<String, GenesisFile>()
  private val services = HashMap<String, RLPxService>()
  private val storageRepositories = HashMap<String, BlockchainRepository>()
  val peerRepositories = HashMap<String, EthereumPeerRepository>()
  private val dnsClients = HashMap<String, DNSClient>()
  private val synchronizers = HashMap<String, Synchronizer>()

  suspend fun start() {
    config.peerRepositories().forEach {
      peerRepositories[it.getName()] = MemoryEthereumPeerRepository()
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
      storageRepositories[dataStore.getName()] = repository
      repoToGenesisFile[repository] = genesisFile
    }

    config.dnsClients().forEach {
      val peerRepository = peerRepositories[it.peerRepository()]
      if (peerRepository == null) {
        val message = (
          if (peerRepositories.isEmpty()) "none" else peerRepositories.keys.joinToString(
            ","
          )
          ) + " defined"
        throw IllegalArgumentException(
          "Repository $peerRepository not found, $message"
        )
      }
      val dnsClient = DNSClient(it, MapKeyValueStore.open(), peerRepository)
      dnsClients[it.getName()] = dnsClient
      dnsClient.start()
      logger.info("Started DNS client ${it.getName()}")
    }

    AsyncCompletion.allOf(
      config.rlpxServices().map { rlpxConfig ->
        val peerRepository = peerRepositories[rlpxConfig.peerRepository()]
        if (peerRepository == null) {
          val message = (
            if (peerRepositories.isEmpty()) "none" else peerRepositories.keys.joinToString(
              ","
            )
            ) + " defined"
          throw IllegalArgumentException(
            "Repository ${rlpxConfig.peerRepository()} not found, $message"
          )
        }
        val repository = storageRepositories[rlpxConfig.repository()]
        if (repository == null) {
          val message = (
            if (storageRepositories.isEmpty()) "none" else storageRepositories.keys.joinToString(
              ","
            )
            ) + " defined"
          throw IllegalArgumentException(
            "Repository ${rlpxConfig.repository()} not found, $message"
          )
        }
        val genesisFile = repoToGenesisFile[repository]
        val genesisBlock = repository.retrieveGenesisBlock()
        val adapter = WireConnectionPeerRepositoryAdapter(peerRepository)
        val ethSubprotocol = EthSubprotocol(
          repository = repository,
          blockchainInfo = SimpleBlockchainInformation(
            UInt256.valueOf(genesisFile!!.chainId.toLong()), genesisBlock.header.difficulty,
            genesisBlock.header.hash, genesisBlock.header.number, genesisBlock.header.hash, genesisFile.forks
          ),
          pendingTransactionsPool = MemoryTransactionPool(),
          listener = adapter::listenToStatus,
          selectionStrategy = { ConnectionManagementStrategy(peerRepositoryAdapter = adapter) }
        )
        val proxySubprotocol = ProxySubprotocol()
        val service = VertxRLPxService(
          vertx,
          rlpxConfig.port(),
          rlpxConfig.networkInterface(),
          rlpxConfig.advertisedPort(),
          SECP256K1.KeyPair.random(),
          listOf(ethSubprotocol, proxySubprotocol),
          rlpxConfig.clientName(),
          adapter
        )
        services[rlpxConfig.getName()] = service
        service.start().thenRun {
          logger.info("Started Ethereum client ${rlpxConfig.getName()}")
          val proxyClient = service.getClient(ProxySubprotocol.ID) as ProxyClient
          for (proxyConfig in config.proxies()) {
            val uri = URI.create(proxyConfig.upstream())
            val target = TcpEndpoint(vertx, uri.host, uri.port)
            target.start()
            proxyClient.registeredSites[proxyConfig.name()] = target
          }
          peerRepository.addIdentityListener {
            service.connectTo(
              it.publicKey(),
              InetSocketAddress(it.networkInterface(), it.port())
            )
          }
          val synchronizer = PeerStatusEthSynchronizer(
            repository = repository,
            client = service.getClient(ETH66) as EthRequestsManager,
            peerRepository = peerRepository,
            adapter = adapter
          )
          synchronizers[rlpxConfig.getName() + "status"] = synchronizer
          synchronizer.start()
          val parentSynchronizer = FromUnknownParentSynchronizer(
            repository = repository,
            client = service.getClient(ETH66) as EthRequestsManager,
            peerRepository = peerRepository
          )
          synchronizers[rlpxConfig.getName() + "parent"] = parentSynchronizer
          parentSynchronizer.start()
          val bestSynchronizer = FromBestBlockSynchronizer(
            repository = repository,
            client = service.getClient(ETH66) as EthRequestsManager,
            peerRepository = peerRepository
          )
          synchronizers[rlpxConfig.getName() + "best"] = bestSynchronizer
          bestSynchronizer.start()
        }
      }
    ).await()

    Runtime.getRuntime().addShutdownHook(object : Thread() {
      override fun run() = this@EthereumClient.stop()
    })
  }

  fun stop() = runBlocking {
    vertx.close()
    dnsClients.values.forEach(DNSClient::stop)
    synchronizers.values.forEach {
      it.stop()
    }
    AsyncCompletion.allOf(services.values.map(RLPxService::stop)).await()
    storageRepositories.values.forEach(BlockchainRepository::close)
  }
}
