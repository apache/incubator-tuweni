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
import kotlinx.coroutines.runBlocking
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.ByteBuffersDirectory
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.eth.EthSubprotocol
import org.apache.tuweni.devp2p.eth.SimpleBlockchainInformation
import org.apache.tuweni.discovery.DNSDaemon
import org.apache.tuweni.eth.genesis.GenesisFile
import org.apache.tuweni.eth.repository.BlockchainIndex
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.vertx.VertxRLPxService
import org.apache.tuweni.units.bigints.UInt256
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.net.InetSocketAddress
import java.security.Security
import kotlin.coroutines.CoroutineContext
/**
 * Main method run by the Ethereum client application.
 */
fun main(args: Array<String>) = runBlocking {
  Security.addProvider(BouncyCastleProvider())
  val client = EthClient()
  client.run(Vertx.vertx())
}

/**
 * Top-level class to run an Ethereum client.
 */
class EthClient(override val coroutineContext: CoroutineContext = Dispatchers.Default) : CoroutineScope {

  private val services = ArrayList<RLPxService>()

  internal suspend fun run(vertx: Vertx) {
    val contents = EthClient::class.java.getResourceAsStream("/mainnet.json").readAllBytes()
    val genesisFile = GenesisFile.read(contents)
    val genesisBlock = genesisFile.toBlock()
    val index = ByteBuffersDirectory()

    val analyzer = StandardAnalyzer()
    val config = IndexWriterConfig(analyzer)
    val writer = IndexWriter(index, config)

    val repository = BlockchainRepository.init(
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
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
            )
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
      listeners = setOf(this@EthClient::refreshPeers)
    )

    Runtime.getRuntime().addShutdownHook(object : Thread() {
      override fun run() = runBlocking {
        dnsDaemon.close()
        AsyncCompletion.allOf(services.map { it.stop() }).await()
        vertx.close()
      }
    })
  }

  private fun refreshPeers(enrs: List<EthereumNodeRecord>) = runBlocking {
    for (service in services) {
      for (enr in enrs.parallelStream().unordered().limit(5)) {
        try {
          service.connectTo(enr.publicKey(), InetSocketAddress(enr.ip(), enr.tcp())).await()
        } catch (e: CancellationException) {
        }
      }
    }
  }
}
