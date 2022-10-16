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
package org.apache.tuweni.hobbits

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.atomic.AtomicInteger

@ExtendWith(VertxExtension::class)
class HobbitsTransportTest {

  @Test
  fun testLifecycle(@VertxInstance vertx: Vertx) = runBlocking {
    val server = HobbitsTransport(vertx)
    server.start()
    server.start()
    server.stop()
  }

  @Test
  fun sendMessageBeforeStart(@VertxInstance vertx: Vertx) = runBlocking {
    val server = HobbitsTransport(vertx)
    val exception: IllegalStateException = assertThrows {
      runBlocking {
        server.sendMessage(
          Message(protocol = Protocol.RPC, headers = Bytes.EMPTY, body = Bytes.EMPTY),
          Transport.TCP,
          "localhost",
          9000
        )
      }
    }
    assertEquals("Server not started", exception.message)
  }

  @Test
  fun registerEndpointAfterStart(@VertxInstance vertx: Vertx) = runBlocking {
    val server = HobbitsTransport(vertx)
    server.start()
    val exception: IllegalStateException = assertThrows {
      server.createHTTPEndpoint(networkInterface = "127.0.0.1", handler = {})
    }
    assertEquals("Server already started", exception.message)
  }

  @Test
  fun sendMessage(@VertxInstance vertx: Vertx) = runBlocking {
    val completion = AsyncResult.incomplete<Bytes>()
    val listening = vertx.createNetServer()
    listening.connectHandler {
      it.handler {
        completion.complete(Bytes.wrapBuffer(it))
      }
    }.listen(0, "localhost") {
      runBlocking {
        val server = HobbitsTransport(vertx)
        server.start()
        val msg = Message(protocol = Protocol.RPC, headers = Bytes.EMPTY, body = Bytes.EMPTY)
        server.sendMessage(msg, Transport.TCP, "localhost", listening.actualPort())
        val result = completion.await()
        assertEquals(msg.toBytes(), result)
      }
    }
  }

  @Test
  fun registerEndpoints(@VertxInstance vertx: Vertx) = runBlocking {
    val server = HobbitsTransport(vertx)
    val httpPort = AtomicInteger()
    val tcpPort = AtomicInteger()
    val wsPort = AtomicInteger()
    server.createHTTPEndpoint("myhttp", "localhost", port = 0, handler = {}, portUpdateListener = httpPort::set)
    server.createUDPEndpoint("myudp", "localhost", handler = {}, port = 32009)
    server.createTCPEndpoint("mytcp", "localhost", port = 0, handler = {}, portUpdateListener = tcpPort::set)
    server.createWSEndpoint("myws", "localhost", port = 0, handler = {}, portUpdateListener = wsPort::set)
    server.start()
    assertNotEquals(0, tcpPort.get())
    assertNotEquals(0, httpPort.get())
    assertNotEquals(0, wsPort.get())
    server.stop()
  }
}
