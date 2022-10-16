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
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.lucene.index.IndexWriter
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Transaction
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
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetSocketAddress

/**
 * This test sends continuously new transactions to all the peers of the service for 10 minutes.
 *
 * The test will connect to a live instance located at port 30303 on localhost.
 */
@Disabled
@ExtendWith(LuceneIndexWriterExtension::class, VertxExtension::class, BouncyCastleExtension::class)
class SendPendingTransactionsTest {

  private val meter = SdkMeterProvider.builder().build().get("connect")

  private val peerId = "b1c9e33ebfd9446151688f0abaf171dac6df31ea5205a200f2cbaf5f8be" +
    "d241c9f93732f25109e16badea1aa657a6078240657688cbbddb91a50aa8c7c34a9cc"

  @Test
  fun testSendPendingTransactions(@LuceneIndexWriter writer: IndexWriter, @VertxInstance vertx: Vertx) = runBlocking {
    val contents = ConnectToAnotherNodeTest::class.java.getResourceAsStream("/besu-dev.json").readAllBytes()
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
      SECP256K1.PublicKey.fromHexString(peerId),
      InetSocketAddress("127.0.0.1", 30303)
    ).await()

    var loop = true
    async {
      delay(10 * 60 * 1000)
      loop = false
    }
    val client = service.getClient(EthSubprotocol.ETH65) as EthClient
    while (loop) {
      val tx = Transaction(
        UInt256.valueOf(1),
        Wei.valueOf(2),
        Gas.valueOf(2),
        Address.fromBytes(Bytes.random(20)),
        Wei.valueOf(2),
        Bytes.random(12),
        SECP256K1.KeyPair.random()
      )
      client.submitPooledTransaction(tx)
      delay(100)
    }
  }
}
