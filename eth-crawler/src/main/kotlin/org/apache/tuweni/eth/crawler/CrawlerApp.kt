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
package org.apache.tuweni.eth.crawler

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.Vertx
import io.vertx.core.net.SocketAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.ExpiringSet
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.Scraper
import org.apache.tuweni.devp2p.eth.EthHelloSubprotocol
import org.apache.tuweni.devp2p.eth.SimpleBlockchainInformation
import org.apache.tuweni.devp2p.eth.logger
import org.apache.tuweni.discovery.DNSDaemon
import org.apache.tuweni.discovery.DNSDaemonListener
import org.apache.tuweni.eth.genesis.GenesisFile
import org.apache.tuweni.ethstats.EthStatsServer
import org.apache.tuweni.metrics.MetricsService
import org.apache.tuweni.rlpx.MemoryWireConnectionsRepository
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.vertx.VertxRLPxService
import org.apache.tuweni.rlpx.wire.DisconnectReason
import org.apache.tuweni.units.bigints.UInt256
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.flywaydb.core.Flyway
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Paths
import java.security.Security
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Application running as a daemon and quietly collecting information about Ethereum nodes.
 */
object CrawlerApp {

  @JvmStatic
  fun main(args: Array<String>) {
    val configFile = Paths.get(if (args.isNotEmpty()) args[0] else "config.toml")
    Security.addProvider(BouncyCastleProvider())
    val vertx = Vertx.vertx()
    val config = CrawlerConfig(configFile)
    if (config.config.hasErrors()) {
      for (error in config.config.errors()) {
        println(error.message)
      }
      System.exit(1)
    }
    val app = CrawlerApplication(vertx, config)
    app.run()
  }
}

class CrawlerApplication(
  val vertx: Vertx,
  val config: CrawlerConfig,
  override val coroutineContext: CoroutineDispatcher =
    Executors.newFixedThreadPool(
      config.numberOfThreads()
    ) {
      val thread = Thread("crawler")
      thread.isDaemon = true
      thread
    }.asCoroutineDispatcher()
) : CoroutineScope {

  private val metricsService = MetricsService(
    "eth-crawler",
    port = config.metricsPort(),
    networkInterface = config.metricsNetworkInterface(),
    enableGrpcPush = config.metricsGrpcPushEnabled(),
    enablePrometheus = config.metricsPrometheusEnabled()
  )

  private fun createCoroutineContext() = Executors.newFixedThreadPool(
    config.numberOfThreads()
  ) {
    val thread = Thread("crawler")
    thread.isDaemon = true
    thread
  }.asCoroutineDispatcher()

  fun run() {
    val dbConfig = HikariConfig()
    dbConfig.maximumPoolSize = config.jdbcConnections()
    dbConfig.jdbcUrl = config.jdbcUrl()
    val ds = HikariDataSource(dbConfig)
    val flyway = Flyway.configure()
      .dataSource(ds)
      .load()
    flyway.migrate()
    val crawlerMeter = metricsService.meterSdkProvider["crawler"]
    val repo = RelationalPeerRepository(ds, config.peerCacheExpiration(), config.clientIdsInterval())
    val statsJob =
      StatsJob(repo, config.upgradesVersions(), crawlerMeter, config.clientsStatsDelay(), coroutineContext)

    logger.info("Initial bootnodes: ${config.bootNodes()}")
    val scraper = Scraper(
      vertx = vertx,
      initialURIs = config.bootNodes(),
      bindAddress = SocketAddress.inetSocketAddress(config.discoveryPort(), config.discoveryNetworkInterface()),
      repository = repo,
      coroutineContext = createCoroutineContext()
    )

    val dnsDaemon = DNSDaemon(
      vertx = vertx,
      seq = 0L,
      enrLink = config.discoveryDNS(),
      period = config.discoveryDNSPollingPeriod(),
      listener = object : DNSDaemonListener {
        override fun newRecords(seq: Long, records: List<EthereumNodeRecord>) {
          launch {
            records.forEach {
              repo.get(it.ip().hostAddress, it.tcp()!!, it.publicKey())
            }
          }
        }
      }
    )

    val contents = if (config.network() == null) {
      Files.readAllBytes(Paths.get(config.genesisFile()))
    } else {
      CrawlerApp::class.java.getResourceAsStream("/${config.network()}.json").readAllBytes()
    }

    val genesisFile = GenesisFile.read(contents)
    val genesisBlock = genesisFile.toBlock()
    val blockchainInformation = SimpleBlockchainInformation(
      UInt256.valueOf(genesisFile.chainId.toLong()),
      genesisBlock.header.difficulty,
      genesisBlock.header.hash,
      UInt256.ZERO,
      genesisBlock.header.hash,
      genesisFile.forks
    )
    val expiringConnectionIds = ExpiringSet<String>()

    val ethHelloProtocol = EthHelloSubprotocol(
      blockchainInfo = blockchainInformation,
      listener = { conn, status ->
        expiringConnectionIds.add(conn.uri())
        repo.recordInfo(conn, status)
      }
    )
    val meter = metricsService.meterSdkProvider.get("rlpx-crawler")
    val wireConnectionsRepository = MemoryWireConnectionsRepository()
    wireConnectionsRepository.addDisconnectionListener {
      if (expiringConnectionIds.add(it.uri())) {
        repo.recordInfo(it, null)
      }
    }
    wireConnectionsRepository.addConnectionListener {
      launch {
        delay(config.rlpxDisconnectionDelay())
        it.disconnect(DisconnectReason.CLIENT_QUITTING)
      }
    }

    val rlpxService = VertxRLPxService(
      vertx,
      30303,
      "127.0.0.1",
      30303,
      SECP256K1.KeyPair.random(),
      listOf(ethHelloProtocol),
      "Apache Tuweni network crawler",
      meter,
      wireConnectionsRepository
    )
    repo.addListener {
      launch {
        connect(rlpxService, it.nodeId, InetSocketAddress(it.endpoint.address, it.endpoint.tcpPort ?: 30303))
      }
    }
    val restService = CrawlerRESTService(
      port = config.restPort(),
      networkInterface = config.restNetworkInterface(),
      repository = repo,
      stats = statsJob,
      maxRequestsPerSec = config.maxRequestsPerSec(),
      meter = meter,
      allowedOrigins = config.corsAllowedOrigins()
    )
    val refreshLoop = AtomicBoolean(true)
    val ethstatsDataRepository = EthstatsDataRepository(ds)
    val ethstatsServer = EthStatsServer(
      vertx,
      config.ethstatsNetworkInterface(),
      config.ethstatsPort(),
      config.ethstatsSecret(),
      controller = CrawlerEthstatsController(ethstatsDataRepository),
      coroutineContext = createCoroutineContext()
    )

    Runtime.getRuntime().addShutdownHook(
      Thread {
        runBlocking {
          statsJob.stop()
          refreshLoop.set(false)
          scraper.stop().await()
          dnsDaemon.close()
          rlpxService.stop().await()
          restService.stop().await()
          async {
            ethstatsServer.stop()
          }.await()
          metricsService.close()
        }
      }
    )
    runBlocking {
      statsJob.start()
      restService.start().await()
      launch {
        while (refreshLoop.get()) {
          try {
            for (connectionInfo in repo.getPeers(System.currentTimeMillis() - 60 * 60 * 1000)) {
              connect(
                rlpxService,
                connectionInfo.nodeId,
                InetSocketAddress(connectionInfo.host, if (connectionInfo.port == 0) 30303 else connectionInfo.port)
              )
            }
          } catch (e: Exception) {
            logger.error("Error connecting to peers", e)
          }
          delay(5 * 60 * 1000L)
        }
      }
      rlpxService.start().await()
      dnsDaemon.start()
      scraper.start()
      ethstatsServer.start()
      val peerSeen = ExpiringSet<PeerConnectionInfo>(5 * 60 * 1000L)
      launch {
        while (refreshLoop.get()) {
          try {
            launch {
              logger.info("Requesting pending peers")
              val pendingPeers = repo.getPendingPeers(100)
              logger.info("Requesting connections to ${pendingPeers.size} peers")
              pendingPeers.map { connectionInfo ->
                async {
                  if (peerSeen.add(connectionInfo)) {
                    connect(
                      rlpxService,
                      connectionInfo.nodeId,
                      InetSocketAddress(
                        connectionInfo.host,
                        if (connectionInfo.port == 0) 30303 else connectionInfo.port
                      )
                    ).await()
                  }
                }
              }.awaitAll()
            }
          } catch (e: Exception) {
            logger.error("Error connecting to pending peers", e)
          }
          delay(10 * 1000L)
        }
      }
    }
  }

  fun connect(rlpxService: RLPxService, key: SECP256K1.PublicKey, address: InetSocketAddress): AsyncCompletion {
    return rlpxService.connectTo(key, address).thenAccept {
      rlpxService.disconnect(it, DisconnectReason.CLIENT_QUITTING)
    }
  }
}
