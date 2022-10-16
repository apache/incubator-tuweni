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
import org.apache.tuweni.devp2p.DiscoveryService
import org.apache.tuweni.devp2p.eth.EthRequestsManager
import org.apache.tuweni.devp2p.eth.EthSubprotocol
import org.apache.tuweni.devp2p.eth.EthSubprotocol.Companion.ETH66
import org.apache.tuweni.devp2p.eth.SimpleBlockchainInformation
import org.apache.tuweni.devp2p.parseEnodeUri
import org.apache.tuweni.devp2p.proxy.ProxyClient
import org.apache.tuweni.devp2p.proxy.ProxySubprotocol
import org.apache.tuweni.devp2p.proxy.TcpDownstream
import org.apache.tuweni.devp2p.proxy.TcpUpstream
import org.apache.tuweni.eth.genesis.GenesisFile
import org.apache.tuweni.eth.repository.BlockchainIndex
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.MemoryTransactionPool
import org.apache.tuweni.ethclient.sync.CanonicalSynchronizer
import org.apache.tuweni.ethclient.sync.FromBestBlockHeaderSynchronizer
import org.apache.tuweni.ethclient.sync.FromUnknownParentSynchronizer
import org.apache.tuweni.ethclient.sync.PeerStatusEthSynchronizer
import org.apache.tuweni.ethclient.sync.Synchronizer
import org.apache.tuweni.ethclient.validator.ChainIdValidator
import org.apache.tuweni.ethclient.validator.TransactionsHashValidator
import org.apache.tuweni.ethclient.validator.Validator
import org.apache.tuweni.kv.InfinispanKeyValueStore
import org.apache.tuweni.kv.KeyValueStore
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.kv.PersistenceMarshaller
import org.apache.tuweni.metrics.MetricsService
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
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext

/**
 * Top-level class to run an Ethereum client.
 */
class EthereumClient(
  val vertx: Vertx,
  val config: EthereumClientConfig,
  override val coroutineContext: CoroutineContext = Dispatchers.Unconfined
) : CoroutineScope {

  companion object {
    val logger = LoggerFactory.getLogger(EthereumClient::class.java)
  }

  private var metricsService: MetricsService? = null
  private val genesisFiles = mutableMapOf<String, GenesisFile>()
  private val rlpxServices = mutableMapOf<String, RLPxService>()
  val storageRepositories = mutableMapOf<String, BlockchainRepository>()
  val peerRepositories = mutableMapOf<String, EthereumPeerRepository>()
  private val dnsClients = mutableMapOf<String, DNSClient>()
  private val discoveryServices = mutableMapOf<String, DiscoveryService>()
  private val synchronizers = mutableMapOf<String, Synchronizer>()
  private val validators = mutableMapOf<String, Validator>()

  private val managerHandler = mutableListOf<DefaultCacheManager>()

  private fun createStore(dataStorage: Path, manager: DefaultCacheManager, name: String): KeyValueStore<Bytes, Bytes> {
    val headersCache: Cache<Bytes, Bytes> = manager.createCache(
      name,
      ConfigurationBuilder().persistence().addStore(RocksDBStoreConfigurationBuilder::class.java)
        .location(dataStorage.resolve(name).toAbsolutePath().toString())
        .expiredLocation(dataStorage.resolve("$name-expired").toAbsolutePath().toString()).build()
    )
    return InfinispanKeyValueStore.open(headersCache)
  }

  suspend fun start() {
    logger.info("Starting Ethereum client...")
    if (config.metricsEnabled()) {
      val metricsService = MetricsService(
        "tuweni",
        port = config.metricsPort(),
        networkInterface = config.metricsNetworkInterface(),
        enableGrpcPush = config.metricsGrpcPushEnabled(),
        enablePrometheus = config.metricsPrometheusEnabled()
      )
      this.metricsService = metricsService
    }
    config.peerRepositories().forEach {
      peerRepositories[it.getName()] =
        when (it.getType()) {
          "memory" -> MemoryEthereumPeerRepository()
          else -> TODO()
        }
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

      val repository = BlockchainRepository.init(
        createStore(dataStorage, manager, "bodies"),
        createStore(dataStorage, manager, "headers"),
        createStore(dataStorage, manager, "metadata"),
        createStore(dataStorage, manager, "transactionReceipts"),
        createStore(dataStorage, manager, "transactions"),
        createStore(dataStorage, manager, "state"),
        BlockchainIndex(writer),
        genesisBlock,
        metricsService?.meterSdkProvider?.get("${dataStore.getName()}_storage")
      )
      storageRepositories[dataStore.getName()] = repository
      repoToGenesisFile[repository] = genesisFile
    }

    config.dnsClients().forEach {
      val peerRepository = peerRepositories[it.peerRepository()]
      if (peerRepository == null) {
        val message = (
          if (peerRepositories.isEmpty()) {
            "none"
          } else {
            peerRepositories.keys.joinToString(
              ","
            )
          }
          ) + " defined"
        throw IllegalArgumentException(
          "Repository $peerRepository not found, $message"
        )
      }
      val dnsClient = DNSClient(vertx, it, MapKeyValueStore.open(), peerRepository)
      dnsClients[it.getName()] = dnsClient
      dnsClient.start()
      logger.info("Started DNS client ${it.getName()} for ${it.enrLink()}")
    }

    config.discoveryServices().forEach {
      val peerRepository = peerRepositories[it.getPeerRepository()]
      if (peerRepository == null) {
        val message = (
          if (peerRepositories.isEmpty()) {
            "none"
          } else {
            peerRepositories.keys.joinToString(
              ","
            )
          }
          ) + " defined"
        throw IllegalArgumentException(
          "Repository $peerRepository not found, $message"
        )
      }
      // TODO right now this doesn't use the peer repository since there is no impl satisfying both libraries.
      val discoveryService = DiscoveryService.open(
        vertx,
        keyPair = it.getIdentity(),
        port = it.getPort(),
        host = it.getNetworkInterface()
      )
      discoveryServices[it.getName()] = discoveryService
      logger.info("Started discovery service ${it.getName()}")
    }
    val adapters = mutableMapOf<String, WireConnectionPeerRepositoryAdapter>()

    AsyncCompletion.allOf(
      config.rlpxServices().map { rlpxConfig ->
        logger.info("Creating RLPx service ${rlpxConfig.getName()}")
        val peerRepository = peerRepositories[rlpxConfig.peerRepository()]
        if (peerRepository == null) {
          val message = (
            if (peerRepositories.isEmpty()) {
              "none"
            } else {
              peerRepositories.keys.joinToString(
                ","
              )
            }
            ) + " defined"
          throw IllegalArgumentException(
            "Repository ${rlpxConfig.peerRepository()} not found, $message"
          )
        }
        val repository = storageRepositories[rlpxConfig.repository()]
        if (repository == null) {
          val message = (
            if (storageRepositories.isEmpty()) {
              "none"
            } else {
              storageRepositories.keys.joinToString(
                ","
              )
            }
            ) + " defined"
          throw IllegalArgumentException(
            "Repository ${rlpxConfig.repository()} not found, $message"
          )
        }
        val genesisFile = repoToGenesisFile[repository]
        val genesisBlock = repository.retrieveGenesisBlock()
        val adapter = WireConnectionPeerRepositoryAdapter(peerRepository)
        val blockchainInfo = SimpleBlockchainInformation(
          UInt256.valueOf(genesisFile!!.chainId.toLong()),
          genesisBlock.header.difficulty,
          genesisBlock.header.hash,
          genesisBlock.header.number,
          genesisBlock.header.hash,
          genesisFile.forks
        )
        logger.info("Initializing with blockchain information $blockchainInfo")
        val ethSubprotocol = EthSubprotocol(
          repository = repository,
          blockchainInfo = blockchainInfo,
          pendingTransactionsPool = MemoryTransactionPool(),
          listener = adapter::listenToStatus,
          selectionStrategy = { ConnectionManagementStrategy(peerRepositoryAdapter = adapter) }
        )
        val meter = metricsService?.meterSdkProvider?.get("${rlpxConfig.getName()}_rlpx")
        val proxySubprotocol = ProxySubprotocol()
        val service = VertxRLPxService(
          vertx,
          rlpxConfig.port(),
          rlpxConfig.networkInterface(),
          rlpxConfig.advertisedPort(),
          rlpxConfig.keyPair(),
          listOf(ethSubprotocol, proxySubprotocol),
          rlpxConfig.clientName(),
          meter,
          adapter
        )
        adapters[rlpxConfig.getName()] = adapter
        rlpxServices[rlpxConfig.getName()] = service
        peerRepository.addIdentityListener {
          service.connectTo(
            it.publicKey(),
            InetSocketAddress(it.networkInterface(), it.port())
          )
        }
        service.start().thenRun {
          logger.info("Started Ethereum client ${rlpxConfig.getName()}")
          val proxyClient = service.getClient(ProxySubprotocol.ID) as ProxyClient
          for (proxyConfig in config.proxies()) {
            if ("" != proxyConfig.upstream()) {
              val uri = URI.create("tcp://${proxyConfig.upstream()}")
              val target = TcpUpstream(vertx, uri.host, uri.port)
              runBlocking {
                target.start()
              }
              proxyClient.registeredSites[proxyConfig.name()] = target
            }
            if ("" != proxyConfig.downstream()) {
              val uri = URI.create("tcp://${proxyConfig.downstream()}")
              // TODO close downstream on exit
              val target = TcpDownstream(vertx, proxyConfig.name(), uri.host, uri.port, proxyClient)
              runBlocking {
                target.start()
              }
            }
          }
          logger.info("Finished configuring Ethereum client ${rlpxConfig.getName()}")
        }
      }
    ).thenRun {
      for (sync in config.synchronizers()) {
        val syncRepository = storageRepositories[sync.getRepository()] ?: throw IllegalArgumentException("Repository ${sync.getRepository()} missing for synchronizer ${sync.getName()}")
        val syncService = rlpxServices[sync.getRlpxService()] ?: throw IllegalArgumentException("Service ${sync.getRlpxService()} missing for synchronizer ${sync.getName()}")
        val syncPeerRepository = peerRepositories[sync.getPeerRepository()] ?: throw IllegalArgumentException("Peer repository ${sync.getPeerRepository()} missing for synchronizer ${sync.getName()}")
        val adapter = adapters[sync.getRlpxService()] ?: throw IllegalArgumentException("Service ${sync.getRlpxService()} missing for synchronizer ${sync.getName()}")

        when (sync.getType()) {
          SynchronizerType.BEST -> {
            val bestSynchronizer = FromBestBlockHeaderSynchronizer(
              repository = syncRepository,
              client = syncService.getClient(ETH66) as EthRequestsManager,
              peerRepository = syncPeerRepository,
              from = sync.getFrom(),
              to = sync.getTo()
            )
            bestSynchronizer.start()
            synchronizers[sync.getName()] = bestSynchronizer
          }
          SynchronizerType.STATUS -> {
            val synchronizer = PeerStatusEthSynchronizer(
              repository = syncRepository,
              client = syncService.getClient(ETH66) as EthRequestsManager,
              peerRepository = syncPeerRepository,
              adapter = adapter,
              from = sync.getFrom(),
              to = sync.getTo()
            )
            synchronizer.start()
            synchronizers[sync.getName()] = synchronizer
          }
          SynchronizerType.PARENT -> {
            val parentSynchronizer = FromUnknownParentSynchronizer(
              repository = syncRepository,
              client = syncService.getClient(ETH66) as EthRequestsManager,
              peerRepository = syncPeerRepository,
              from = sync.getFrom(),
              to = sync.getTo()
            )
            parentSynchronizer.start()
            synchronizers[sync.getName()] = parentSynchronizer
          }
          SynchronizerType.CANONICAL -> {
            val fromRepository = storageRepositories.get(sync.getFromRepository())
              ?: throw IllegalArgumentException("Missing repository for canonical repository ${sync.getFromRepository()}")
            val canonicalSynchronizer = CanonicalSynchronizer(
              repository = syncRepository,
              client = syncService.getClient(ETH66) as EthRequestsManager,
              peerRepository = syncPeerRepository,
              from = sync.getFrom(),
              to = sync.getTo(),
              fromRepository = fromRepository
            )
            canonicalSynchronizer.start()
            synchronizers[sync.getName()] = canonicalSynchronizer
          }
        }
      }

      for (validator in config.validators()) {
        when (validator.getType()) {
          ValidatorType.CHAINID -> {
            val chainId = validator.getChainId()
              ?: throw IllegalArgumentException("chainId required for validator ${validator.getName()}")
            val chainIdValidator = ChainIdValidator(
              from = validator.getFrom(),
              to = validator.getTo(),
              chainId = chainId
            )
            validators[validator.getName()] = chainIdValidator
          }
          ValidatorType.TRANSACTIONHASH -> {
            val transactionsHashValidator = TransactionsHashValidator(from = validator.getFrom(), to = validator.getTo())
            validators[validator.getName()] = transactionsHashValidator
          }
        }
      }
    }.await()

    for (staticPeers in config.staticPeers()) {
      val peerRepository = peerRepositories[staticPeers.peerRepository()]
      if (peerRepository == null) {
        val message = (
          if (peerRepositories.isEmpty()) {
            "none"
          } else {
            peerRepositories.keys.joinToString(
              ","
            )
          }
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
    dnsClients.values.forEach(DNSClient::stop)
    AsyncCompletion.allOf(discoveryServices.values.map(DiscoveryService::shutdownAsync)).await()
    synchronizers.values.forEach {
      it.stop()
    }
    managerHandler.forEach {
      it.stop()
    }
    AsyncCompletion.allOf(rlpxServices.values.map(RLPxService::stop)).await()
    storageRepositories.values.forEach(BlockchainRepository::close)
    metricsService?.close()
    Unit
  }
}
