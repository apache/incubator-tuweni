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
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.devp2p.eth.EthRequestsManager
import org.apache.tuweni.devp2p.eth.EthSubprotocol
import org.apache.tuweni.devp2p.eth.EthSubprotocol.Companion.ETH66
import org.apache.tuweni.devp2p.eth.SimpleBlockchainInformation
import org.apache.tuweni.devp2p.parseEnodeUri
import org.apache.tuweni.eth.genesis.GenesisFile
import org.apache.tuweni.eth.repository.BlockchainIndex
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.MemoryTransactionPool
import org.apache.tuweni.ethclient.metrics.MetricsService
import org.apache.tuweni.kv.InfinispanKeyValueStore
import org.apache.tuweni.kv.LevelDBKeyValueStore
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.vertx.VertxRLPxService
import org.apache.tuweni.units.bigints.UInt256
import org.infinispan.Cache
import org.infinispan.configuration.cache.ConfigurationBuilder
import org.infinispan.configuration.global.GlobalConfigurationBuilder
import org.infinispan.manager.DefaultCacheManager
import org.infinispan.persistence.rocksdb.configuration.RocksDBStoreConfigurationBuilder
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
  override val coroutineContext: CoroutineContext = Dispatchers.Unconfined,
) : CoroutineScope {

  companion object {
    val logger = LoggerFactory.getLogger(EthereumClient::class.java)
  }

  private var metricsService: MetricsService? = null
  private val genesisFiles = HashMap<String, GenesisFile>()
  private val services = HashMap<String, RLPxService>()
  private val storageRepositories = HashMap<String, BlockchainRepository>()
  val peerRepositories = HashMap<String, EthereumPeerRepository>()
  private val dnsClients = HashMap<String, DNSClient>()
  private val synchronizers = HashMap<String, Synchronizer>()

  private val managerHandler = mutableListOf<DefaultCacheManager>()

  suspend fun start() {
    logger.info("Starting Ethereum client...")
    val metricsService = MetricsService(
      "tuweni",
      port = config.metricsPort(),
      networkInterface = config.metricsNetworkInterface(),
      enableGrpcPush = config.metricsGrpcPushEnabled(),
      enablePrometheus = config.metricsPrometheusEnabled()
    )
    this.metricsService = metricsService
    config.peerRepositories().forEach {
      peerRepositories[it.getName()] = MemoryEthereumPeerRepository()
    }

    config.genesisFiles().forEach {
      genesisFiles[it.getName()] = it.genesisFile()
    }

    val repoToGenesisFile = HashMap<BlockchainRepository, GenesisFile>()

    config.dataStores().forEach { dataStore ->
      val genesisFile = genesisFiles[dataStore.getGenesisFile()]
      val genesisBlock = genesisFile!!.toBlock()
      val dataStorage = dataStore.getStoragePath()
      val index = NIOFSDirectory(dataStorage.resolve("index"))
      val analyzer = StandardAnalyzer()
      val config = IndexWriterConfig(analyzer)
      val writer = IndexWriter(index, config)
      val builder = GlobalConfigurationBuilder().serialization().marshaller(PersistenceMarshaller())

      val manager = DefaultCacheManager(builder.build())
      managerHandler.add(manager)
      val headersCache: Cache<Bytes, Bytes> = manager.createCache(
        "headers",
        ConfigurationBuilder().persistence().addStore(RocksDBStoreConfigurationBuilder::class.java)
          .location(dataStorage.resolve("headers").toAbsolutePath().toString())
          .expiredLocation(dataStorage.resolve("headers-expired").toAbsolutePath().toString()).build()
      )
      val repository = BlockchainRepository.init(
        LevelDBKeyValueStore.open(dataStorage.resolve("bodies")),
        InfinispanKeyValueStore.open(headersCache),
        LevelDBKeyValueStore.open(dataStorage.resolve("metadata")),
        LevelDBKeyValueStore.open(dataStorage.resolve("transactionReceipts")),
        LevelDBKeyValueStore.open(dataStorage.resolve("transactions")),
        LevelDBKeyValueStore.open(dataStorage.resolve("state")),
        BlockchainIndex(writer),
        genesisBlock,
        metricsService.meterSdkProvider["${dataStore.getName()}_storage"]
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
      logger.info("Started DNS client ${it.getName()} for ${it.enrLink()}")
    }

    AsyncCompletion.allOf(
      config.rlpxServices().map { rlpxConfig ->
        logger.info("Creating RLPx service ${rlpxConfig.getName()}")
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
        val blockchainInfo = SimpleBlockchainInformation(
          UInt256.valueOf(genesisFile!!.chainId.toLong()), genesisBlock.header.difficulty,
          genesisBlock.header.hash, genesisBlock.header.number, genesisBlock.header.hash, genesisFile.forks
        )
        logger.info("Initializing with blockchain information $blockchainInfo")
        val ethSubprotocol = EthSubprotocol(
          repository = repository,
          blockchainInfo = blockchainInfo,
          pendingTransactionsPool = MemoryTransactionPool(),
          listener = adapter::listenToStatus,
          selectionStrategy = { ConnectionManagementStrategy(peerRepositoryAdapter = adapter) }
        )
        val meter = metricsService.meterSdkProvider["${rlpxConfig.getName()}_rlpx"]
        val service = VertxRLPxService(
          vertx,
          rlpxConfig.port(),
          rlpxConfig.networkInterface(),
          rlpxConfig.advertisedPort(),
          rlpxConfig.keyPair(),
          listOf(ethSubprotocol),
          rlpxConfig.clientName(),
          meter,
          adapter
        )
        services[rlpxConfig.getName()] = service
        service.start().thenRun {
          logger.info("Started Ethereum client ${rlpxConfig.getName()}")
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

    for (staticPeers in config.staticPeers()) {
      val peerRepository = peerRepositories[staticPeers.peerRepository()]
      if (peerRepository == null) {
        val message = (
          if (peerRepositories.isEmpty()) "none" else peerRepositories.keys.joinToString(
            ","
          )
          ) + " defined"
        throw IllegalArgumentException(
          "Repository ${staticPeers.peerRepository()} not found, $message"
        )
      }
      for (enode in staticPeers.enodes()) {
        // TODO add config validation for enode uris
        val uri = parseEnodeUri(URI.create(enode))
        peerRepository.storeIdentity(uri.endpoint.address, uri.endpoint.tcpPort ?: uri.endpoint.udpPort, uri.nodeId)
      }
    }

    Runtime.getRuntime().addShutdownHook(object : Thread() {
      override fun run() = this@EthereumClient.stop()
    })
    logger.info("Started Ethereum client")
  }

  fun stop() = runBlocking {
    vertx.close()
    dnsClients.values.forEach(DNSClient::stop)
    synchronizers.values.forEach {
      it.stop()
    }
    managerHandler.forEach{ it.stop() }
    AsyncCompletion.allOf(services.values.map(RLPxService::stop)).await()
    storageRepositories.values.forEach(BlockchainRepository::close)
    metricsService?.close()
    Unit
  }
}
