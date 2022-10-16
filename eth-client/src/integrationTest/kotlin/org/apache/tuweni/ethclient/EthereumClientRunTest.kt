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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class, BouncyCastleExtension::class)
class EthereumClientRunTest {

  @Disabled
  @Test
  fun connectToDevNetwork(@VertxInstance vertx: Vertx) = runBlocking {
    val keyPair = SECP256K1.KeyPair.random()
    val config = EthereumClientConfig.fromString(
      """
      [peerRepository.default]
      type="memory"
      [storage.default]
      path="data"
      genesis="default"
      [genesis.default]
      path="classpath:/genesis/besu-dev.json"
      [rlpx.default]
      networkInterface="127.0.0.1"
      port=30301
      key="${keyPair.secretKey().bytes().toHexString()}"
      [static.default]
      repository="default"
      enodes=["enode://46a3f45f9e2d8870769b8e6760d47b9c55567cee1175016c36cbe4955dcf1e7042dbb7ce06af9d307ea4a1fef2162d53e14d8a1196fde663444e75a6cd59fcdf@127.0.0.1:33033"]
      [synchronizer.default]
      type="status"
      from=0
      to=100
      """.trimMargin()
    )
    val client = EthereumClient(vertx, config)
    client.start()
    delay(300000)
    println("Got ${client.peerRepositories["default"]!!.activeConnections().count()} connections")
  }

  @Test
  fun startTwoClientsAndConnectThem(@VertxInstance vertx: Vertx) = runBlocking {
    val keyPair = SECP256K1.KeyPair.random()
    val config1 = EthereumClientConfig.fromString(
      """
      [peerRepository.default]
      type="memory"
      [storage.default]
      path="data"
      genesis="default"
      [genesis.default]
      path="classpath:/default.json"
      [rlpx.default]
      networkInterface="127.0.0.1"
      port=30301
      key="${keyPair.secretKey().bytes().toHexString()}"
      """.trimMargin()
    )
    val config2 = EthereumClientConfig.fromString(
      """
      [peerRepository.default]
      type="memory"
      [storage.default]
      path="data2"
      genesis="default"
      [genesis.default]
      path="classpath:/default.json"
      [static.default]
      enodes=["enode://${keyPair.publicKey().toHexString()}@127.0.0.1:30301"]
      peerRepository="default"
      [rlpx.default]
      networkInterface="127.0.0.1"
      port=30304
      key="${SECP256K1.KeyPair.random().secretKey().bytes().toHexString()}"
      """.trimMargin()
    )
    val client1 = EthereumClient(vertx, config1)
    val client2 = EthereumClient(vertx, config2)
    client1.start()
    val connectionInfo = AsyncResult.incomplete<EthereumConnection>()
    client1.peerRepositories["default"]!!.addStatusListener { connectionInfo.complete(it) }
    client2.start()

    val conn = connectionInfo.await()
    assertNotNull(conn)
    assertNotNull(conn.status())
    assertEquals(client2.config.rlpxServices()[0].keyPair().publicKey(), conn.peer().id().publicKey())
    client1.stop()
    client2.stop()
  }

  // this actually connects the client to mainnet!
  @Disabled
  @Test
  fun connectToMainnet(@VertxInstance vertx: Vertx) = runBlocking {
    val config = EthereumClientConfig.fromString(
      """
        [peerRepository.default]
        type="memory"
        [storage.default]
        genesis="default"
        path="mainnet"
        [genesis.default]
        path=classpath:/mainnet.json
        [dns.default]
        enrLink="enrtree://AKA3AM6LPBYEUDMVNU3BSVQJ5AD45Y7YPOHJLEF6W26QOE4VTUDPE@all.mainnet.ethdisco.net"
      """.trimMargin()
    )
    val client = EthereumClient(vertx, config)
    client.start()
    delay(300000)
    println("Got ${client.peerRepositories["default"]!!.activeConnections().count()} connections")
  }
}
