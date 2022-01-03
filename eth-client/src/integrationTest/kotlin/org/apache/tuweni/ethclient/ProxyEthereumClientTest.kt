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
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class, BouncyCastleExtension::class)
class ProxyEthereumClientTest {

  @Test
  fun proxyTalkingToEachOther(@VertxInstance vertx: Vertx) = runBlocking {
    // start a service saying hello on port 14000:
    val server = vertx.createNetServer(NetServerOptions().setPort(14000))
    server.connectHandler {
      it.write("Hello World!")
    }

    val config1 = EthereumClientConfig.fromString(
      """
      [metrics]
      networkInterface="127.0.0.1"
      port=9091
      [storage.default]
      path="data"
      genesis="default"
      [genesis.default]
      path="classpath:/default.json"
      [static.default]
      peerRepository="default"
      [proxy.foo]
      downstream=localhost:14000
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
      [genesis.default]
      path="classpath:/default.json"
      [static.default]
      peerRepository="default"
      [proxy.bar]
      upstream=localhost:14001
      [proxy.foo]
      downstream=localhost:15000
      """.trimMargin()
    )
    val client1 = EthereumClient(vertx, config1)
    val client2 = EthereumClient(vertx, config2)
    client1.start()
    client2.start()
    // TODO connect the servers!

    client1.stop()
    client2.stop()
  }
}
