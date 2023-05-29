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
package org.apache.tuweni.stratum.server

import io.vertx.core.Vertx
import io.vertx.core.net.NetClientOptions
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.charset.StandardCharsets

@ExtendWith(VertxExtension::class)
class StratumServerTest {

  @Test
  fun testStartStopServer(@VertxInstance vertx: Vertx) {
    val server = StratumServer(vertx, 0, "127.0.0.1", null, "", { true }, Bytes32::random)
    runBlocking {
      server.start()
      assertNotEquals(0, server.port())
      server.stop()
    }
  }

  @Test
  fun testBadClient(@VertxInstance vertx: Vertx) {
    val server = StratumServer(vertx, 0, "127.0.0.1", null, "", { true }, Bytes32::random)
    val disconnected = AsyncCompletion.incomplete()
    runBlocking {
      server.start()
      val client = vertx.createNetClient()
      client.connect(server.port(), "127.0.0.1") { result ->
        assertTrue(result.succeeded())
        val socket = result.result()
        socket.closeHandler { disconnected.complete() }
        socket.write("{\"foo\":\"bar\"}\n")
      }
      disconnected.join()
    }
  }

  @Test
  fun testValidClient(@VertxInstance vertx: Vertx) {
    val server = StratumServer(vertx, 0, "127.0.0.1", null, "", { true }, Bytes32::random)
    runBlocking {
      server.start()
      val client = vertx.createNetClient(NetClientOptions().setTcpKeepAlive(true))
      var disconnected = false
      val message = AsyncResult.incomplete<String>()
      client.connect(server.port(), "127.0.0.1") { result ->
        assertTrue(result.succeeded())
        val socket = result.result()
        socket.closeHandler {
          disconnected = true
        }
        socket.write("{\"id\":0,\"method\":\"eth_submitLogin\",\"params\":[\"0xabcdef1234567891234.worker\"]}\n")
        socket.handler { buffer -> message.complete(buffer.toString(StandardCharsets.UTF_8)) }
      }

      assertTrue(message.get()!!.startsWith("{\"id\":0,\"jsonrpc\":\"2.0\",\"result\":true}\n"), message.get())
      assertFalse(disconnected)
    }
  }
}
