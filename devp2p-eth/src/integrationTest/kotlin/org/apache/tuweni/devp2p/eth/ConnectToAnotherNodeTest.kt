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

import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.lucene.index.IndexWriter
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.genesis.GenesisFile
import org.apache.tuweni.eth.repository.BlockchainIndex
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.MemoryTransactionPool
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.LuceneIndexWriter
import org.apache.tuweni.junit.LuceneIndexWriterExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.rlpx.vertx.VertxRLPxService
import org.apache.tuweni.units.bigints.UInt256
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetSocketAddress

@ExtendWith(LuceneIndexWriterExtension::class, VertxExtension::class, BouncyCastleExtension::class)
class ConnectToAnotherNodeTest {

  private val meter = SdkMeterProvider.builder().build().get("connect")

  /**
   * To run this test, run an Ethereum mainnet node at home and point this test to it.
   *
   */
  @Disabled
  @Test
  fun testCollectHeaders(@LuceneIndexWriter writer: IndexWriter, @VertxInstance vertx: Vertx) = runBlocking {
    val contents = ConnectToAnotherNodeTest::class.java.getResourceAsStream("/mainnet.json").readAllBytes()
    val genesisFile = GenesisFile.read(contents)
    val genesisBlock = genesisFile.toBlock()

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
    val service = VertxRLPxService(
      vertx,
      30304,
      "127.0.0.1",
      30304,
      SECP256K1.KeyPair.random(),
      listOf(
        EthSubprotocol(
          repository = repository,
          blockchainInfo = SimpleBlockchainInformation(
            UInt256.valueOf(genesisFile.chainId.toLong()),
            genesisBlock.header.difficulty,
            genesisBlock.header.hash,
            UInt256.valueOf(42L),
            genesisBlock.header.hash,
            genesisFile.forks
          ),
          pendingTransactionsPool = MemoryTransactionPool()
        )
      ),
      "Tuweni Experiment 0.1",
      meter
    )
    service.start().await()
    service.connectTo(
      SECP256K1.PublicKey.fromHexString(
        "b1c9e33ebfd9446151688f0abaf171dac6df31ea5205a200f2cbaf5f8be" +
          "d241c9f93732f25109e16badea1aa657a6078240657688cbbddb91a50aa8c7c34a9cc"
      ),
      InetSocketAddress("192.168.88.46", 30303)
    ).await()

//    val client = service.getClient(EthSubprotocol.ETH62) as EthRequestsManager
//    client.requestBlockHeaders(genesisBlock.header.hash, 100, 0, false).await()
//
//    val header = repository.findBlockByHashOrNumber(UInt256.valueOf(99L).toBytes())
//    Assertions.assertFalse(header.isEmpty())
//
//    val header3 = repository.findBlockByHashOrNumber(UInt256.valueOf(101L).toBytes())
//    Assertions.assertTrue(header3.isEmpty())
//    val header2 = repository.findBlockByHashOrNumber(UInt256.valueOf(100L).toBytes())
//    Assertions.assertTrue(header2.isEmpty())
    Thread.sleep(100000)
    service.stop().await()
  }

  @Disabled("flaky")
  @Test
  fun twoServers(@LuceneIndexWriter writer: IndexWriter, @VertxInstance vertx: Vertx) = runBlocking {
    val contents = EthHandlerTest::class.java.getResourceAsStream("/mainnet.json").readAllBytes()
    val genesisBlock = GenesisFile.read(contents).toBlock()

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
    val service = VertxRLPxService(
      vertx,
      0,
      "127.0.0.1",
      0,
      SECP256K1.KeyPair.random(),
      listOf(
        EthSubprotocol(
          repository = repository,
          blockchainInfo = SimpleBlockchainInformation(
            UInt256.ZERO,
            genesisBlock.header.difficulty,
            genesisBlock.header.hash,
            UInt256.valueOf(42L),
            genesisBlock.header.hash,
            emptyList()
          ),
          pendingTransactionsPool = MemoryTransactionPool()
        )
      ),
      "Tuweni Experiment 0.1",
      meter
    )

    val repository2 = BlockchainRepository.init(
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      BlockchainIndex(writer),
      genesisBlock
    )
    val service2kp = SECP256K1.KeyPair.random()
    val service2 = VertxRLPxService(
      vertx,
      0,
      "127.0.0.1",
      0,
      service2kp,
      listOf(
        EthSubprotocol(
          repository = repository2,
          blockchainInfo = SimpleBlockchainInformation(
            UInt256.ZERO,
            genesisBlock.header.difficulty,
            genesisBlock.header.hash,
            UInt256.valueOf(42L),
            genesisBlock.header.hash,
            emptyList()
          ),
          pendingTransactionsPool = MemoryTransactionPool()
        )
      ),
      "Tuweni Experiment 0.1",
      meter
    )
    val result = AsyncCompletion.allOf(service.start(), service2.start()).then {
      service.connectTo(service2kp.publicKey(), InetSocketAddress("127.0.0.1", service2.actualPort()))
    }

    result.await()
    Assertions.assertNotNull(result.get())
    AsyncCompletion.allOf(service.stop(), service2.stop()).await()
  }
}
