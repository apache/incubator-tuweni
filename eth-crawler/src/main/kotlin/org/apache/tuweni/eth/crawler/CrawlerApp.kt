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

import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.Vertx
import io.vertx.core.net.SocketAddress
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.devp2p.Scraper
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.flywaydb.core.Flyway
import java.nio.file.Paths
import java.security.Security

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
    run(vertx, config)
  }

  fun run(vertx: Vertx, config: CrawlerConfig) {
    val ds = HikariDataSource()
    ds.jdbcUrl = config.jdbcUrl()
    val flyway = Flyway.configure()
      .dataSource(ds)
      .load()
    flyway.migrate()

    val scraper = Scraper(
      vertx = vertx,
      initialURIs = config.bootNodes(),
      bindAddress = SocketAddress.inetSocketAddress(config.discoveryPort(), config.discoveryNetworkInterface()),
      repository = RelationalPeerRepository(ds)
    )
    Runtime.getRuntime().addShutdownHook(
      Thread {
        runBlocking {
          scraper.stop().await()
        }
      }
    )
    runBlocking {
      scraper.start().await()
    }
  }
}
