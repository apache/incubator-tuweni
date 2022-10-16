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
import io.vertx.core.net.NetServerOptions
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.lang.Thread.sleep

@ExtendWith(VertxExtension::class, BouncyCastleExtension::class)
class ProxyEthereumClientTest {

  @Test
  fun proxyTalkingToEachOther(@VertxInstance vertx: Vertx) = runBlocking {
    // start a service saying hello on port 14000:
    val server = vertx.createNetServer(NetServerOptions().setPort(14000).setHost("127.0.0.1"))
    server.connectHandler { socket ->
      socket.handler {
        socket.write("Hello World!")
      }
    }
    server.listen().await()

    val identity = SECP256K1.KeyPair.random()
    val identity2 = SECP256K1.KeyPair.random()

    val config1 = EthereumClientConfig.fromString(
      """
      [peerRepository.default]
      type="memory"
      [storage.default]
      path="proxydata"
      genesis="default"
      [genesis.default]
      path="classpath:/default.json"
      [static.default]
      peerRepository="default"
      [proxy.foo]
      name="foo"
      upstream="localhost:14000"
      [rlpx.default]
      port=30303
      key="${identity.secretKey().bytes().toHexString()}"
      """.trimMargin()
    )
    val config2 = EthereumClientConfig.fromString(
      """
      [peerRepository.default]
      type="memory"
      [storage.default]
      path="proxydata2"
      genesis="default"
      [genesis.default]
      path="classpath:/default.json"
      [static.default]
      peerRepository="default"
      enodes=["enode://${identity.publicKey().toHexString()}@localhost:30303"]
      [proxy.bar]
      name="bar"
      upstream="localhost:14001"
      [proxy.foo]
      name="foo"
      downstream="127.0.0.1:15000"
      [rlpx.default]
      port=30304
      key="${identity2.secretKey().bytes().toHexString()}"
      """.trimMargin()
    )
    assertEquals(1, config1.proxies().size)
    assertEquals(2, config2.proxies().size)
    val client1 = EthereumClient(vertx, config1)
    val client2 = EthereumClient(vertx, config2)

    client1.start()

    client2.start()
    // TODO listen for proxy client connection status
    sleep(5000)

    val receivedMessage = AsyncResult.incomplete<String>()

    val netClient = vertx.createNetClient()
    val socket = netClient.connect(15000, "127.0.0.1").await()
    socket.handler {
      receivedMessage.complete(it.toString())
    }
    socket.write("Test")

    val received = receivedMessage.await()
    assertEquals("Hello World!", received)

    client1.stop()
    client2.stop()
  }
}
