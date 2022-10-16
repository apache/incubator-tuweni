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
package org.apache.tuweni.scuttlebutt.handshake.vertx

import io.vertx.core.Vertx
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.crypto.sodium.Sodium
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec.encodeRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCFlag
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

@ExtendWith(VertxExtension::class)
internal class VertxIntegrationTest {
  companion object {
    @JvmStatic
    @BeforeAll
    fun checkAvailable() {
      Assumptions.assumeTrue(Sodium.isAvailable(), "Sodium native library is not available")
    }
  }

  private class MyClientHandler(private val sender: Consumer<Bytes>, private val terminationFn: Runnable) :
    ClientHandler {
    override fun receivedMessage(message: Bytes) {}
    override fun streamClosed() {}
    fun sendMessage(bytes: Bytes) {
      sender.accept(bytes)
    }

    fun closeStream() {
      terminationFn.run()
    }
  }

  private class MyServerHandler : ServerHandler {
    var closed = false
    var received: Bytes? = null
    override fun receivedMessage(message: Bytes?) {
      received = message
    }

    override fun streamClosed() {
      closed = true
    }
  }

  @Test
  @Throws(Exception::class)
  fun connectToServer(@VertxInstance vertx: Vertx) = runBlocking {
    val serverKeyPair = Signature.KeyPair.random()
    val networkIdentifier = Bytes32.random()
    val serverHandlerRef = AtomicReference<MyServerHandler>()
    val server = SecureScuttlebuttVertxServer(
      vertx,
      InetSocketAddress("localhost", 20000),
      serverKeyPair,
      networkIdentifier
    ) { _, _ ->
      serverHandlerRef.set(MyServerHandler())
      serverHandlerRef.get()
    }
    server.start()
    val client = SecureScuttlebuttVertxClient(vertx, Signature.KeyPair.random(), networkIdentifier)
    val handler = client.connectTo(
      20000,
      "localhost",
      serverKeyPair.publicKey(),
      null
    ) { sender, terminationFn ->
      MyClientHandler(
        sender,
        terminationFn
      )
    } as MyClientHandler
    delay(1000)
    Assertions.assertNotNull(handler)
    val rpcRequestBody = "{\"name\": [\"whoami\"],\"type\": \"async\",\"args\":[]}"
    val rpcRequest = encodeRequest(rpcRequestBody, RPCFlag.BodyType.JSON)
    handler.sendMessage(rpcRequest)
    delay(1000)
    val serverHandler = serverHandlerRef.get()
    val receivedBytes = serverHandler.received
    val requestBody = rpcRequest.slice(9)
    Assertions.assertEquals(requestBody, receivedBytes)
    handler.closeStream()
    delay(1000)
    Assertions.assertTrue(serverHandler.closed)
    client.stop()
    server.stop()
  }
}
