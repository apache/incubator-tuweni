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
package org.apache.tuweni.devp2p.proxy

import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.vertx.core.Vertx
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.apache.tuweni.rlpx.vertx.VertxRLPxService
import org.apache.tuweni.rlpx.wire.WireConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetSocketAddress

@Disabled("flaky")
@ExtendWith(VertxExtension::class, BouncyCastleExtension::class)
class SendDataToAnotherNodeTest {

  private val meter = SdkMeterProvider.builder().build().get("connect")

  @Test
  fun testConnectTwoProxyNodes(@VertxInstance vertx: Vertx) = runBlocking {
    val service = VertxRLPxService(
      vertx,
      0,
      "127.0.0.1",
      0,
      SECP256K1.KeyPair.random(),
      listOf(
        ProxySubprotocol()
      ),
      "Tuweni Experiment 0.1",
      meter
    )

    val service2kp = SECP256K1.KeyPair.random()
    val service2 = VertxRLPxService(
      vertx,
      0,
      "127.0.0.1",
      0,
      service2kp,
      listOf(ProxySubprotocol()),
      "Tuweni Experiment 0.1",
      meter
    )
    val recorder = RecordingClientHandler()
    service.start().await()
    service2.start().await()
    val client = service.getClient(ProxySubprotocol.ID) as ProxyClient
    client.registeredSites["datasink"] = recorder

    var conn: WireConnection? = null
    for (i in 1..5) {
      try {
        conn = service.connectTo(service2kp.publicKey(), InetSocketAddress("127.0.0.1", service2.actualPort())).get()
        break
      } catch (e: Exception) {
        delay(100)
      }
    }
    assertNotNull(conn)
    delay(100)
    assertTrue(conn!!.agreedSubprotocols().contains(ProxySubprotocol.ID))
    val client2 = service2.getClient(ProxySubprotocol.ID) as ProxyClient
    assertTrue(client2.knownSites().contains("datasink"))
    client2.request("datasink", Bytes.wrap("Hello world".toByteArray()))
    client2.request("datasink", Bytes.wrap("foo".toByteArray()))
    client2.request("datasink", Bytes.wrap("foobar".toByteArray()))

    assertEquals(recorder.messages[0], "Hello world")
    assertEquals(recorder.messages[1], "foo")
    assertEquals(recorder.messages[2], "foobar")
  }
}

class RecordingClientHandler : ClientHandler {

  val messages = mutableListOf<String>()

  override suspend fun handleRequest(message: Bytes): Bytes {
    messages.add(String(message.toArrayUnsafe()))
    return Bytes.wrap("OK".toByteArray())
  }
}
