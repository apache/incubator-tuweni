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
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class, BouncyCastleExtension::class)
class EthereumClientRunTest {

  @Test
  fun startTwoClientsAndConnectThem(@VertxInstance vertx: Vertx) = runBlocking {
    val keyPair = SECP256K1.KeyPair.random()
    val config1 = EthereumClientConfig.fromString(
      """
      [metrics]
      networkInterface="127.0.0.1"
      port=9091
      [storage.default]
      path="data"
      genesis="default"
      [static.default]
      peerRepository="default"
      [rlpx.default]
      networkInterface="127.0.0.1"
      port=30301
      keyPair="${keyPair.secretKey().bytes().toHexString()}"
      """.trimMargin()
    )
    val config2 = EthereumClientConfig.fromString(
      """
      [metrics]
      networkInterface="127.0.0.1"
      port=9092
      [storage.default]
      path="data2"
      genesis="default"
      [static.default]
      enodes=["enode://${keyPair.publicKey().toHexString()}@127.0.0.1:30301"]
      peerRepository="default"
      """.trimMargin()
    )
    val client1 = EthereumClient(vertx, config1)
    val client2 = EthereumClient(vertx, config2)
    client1.start()
    client2.start()
    // TODO make sure the connection happens!
    client1.stop()
    client2.stop()
  }

  // this actually connects the client to mainnet!
  @Disabled
  @Test
  fun connectToMainnet(@VertxInstance vertx: Vertx) = runBlocking {
    val config = EthereumClientConfig.fromString(
      "" +
        "[storage.default]\n" +
        "genesis=\"default\"\n" +
        "path=\"mainnet\"\n" +
        "[genesis.default]\n" +
        "path=classpath:/mainnet.json\n" +
        "[dns.default]\n" +
        "enrLink=\"enrtree://AKA3AM6LPBYEUDMVNU3BSVQJ5AD45Y7YPOHJLEF6W26QOE4VTUDPE@all.mainnet.ethdisco.net\""
    )
    val client = EthereumClient(vertx, config)
    client.start()
    delay(300000)
    println("Got ${client.peerRepositories["default"]!!.activeConnections().count()} connections")
  }
}
