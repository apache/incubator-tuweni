// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
