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
package org.apache.tuweni.scuttlebutt.rpc

import io.vertx.core.Vertx
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.io.Base64
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.apache.tuweni.scuttlebutt.handshake.vertx.ClientHandler
import org.apache.tuweni.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec.encodeRequest
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCHandler
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCRequestFailedException
import org.apache.tuweni.scuttlebutt.rpc.mux.ScuttlebuttStreamHandler
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.function.Consumer

/**
 * Test used against a Securescuttlebutt server.
 *
 * The test requires the ssb_dir, ssb_host and ssb_port to be set.
 */
@ExtendWith(VertxExtension::class)
internal class RPCIntegrationTest {
  class MyClientHandler(private val sender: Consumer<Bytes>, private val terminationFn: Runnable) :
    ClientHandler {
    override fun receivedMessage(message: Bytes) {
      RPCMessage(message)
    }

    override fun streamClosed() {
      terminationFn.run()
    }

    fun sendMessage(bytes: Bytes) {
      sender.accept(bytes)
    }
  }

  /**
   * This test tests the connection to a local patchwork installation. You need to run patchwork locally to perform that
   * work.
   *
   */
  @Test
  @Throws(Exception::class)
  fun runWithPatchWork(@VertxInstance vertx: Vertx) = runBlocking {
    val env = System.getenv()
    val host = env.getOrDefault("ssb_host", "localhost")
    val port = env.getOrDefault("ssb_port", "8008").toInt()
    val keyPair: Signature.KeyPair = Utils.localKeys
    val networkKeyBase64 = "1KHLiKZvAvjbY1ziZEHMXawbCEIM6qwjCDm3VYRan/s="
    val publicKey = keyPair.publicKey()
    val networkKeyBytes32 = Bytes32.wrap(Base64.decode(networkKeyBase64))
    val secureScuttlebuttVertxClient = SecureScuttlebuttVertxClient(vertx, keyPair, networkKeyBytes32)
    val onConnect = secureScuttlebuttVertxClient.connectTo(
      port,
      host,
      publicKey,
      null
    ) { sender, terminationFn ->
      MyClientHandler(
        sender,
        terminationFn
      )
    }
    val clientHandler = onConnect as MyClientHandler?
    delay(1000)
    Assertions.assertNotNull(clientHandler)
    // An RPC command that just tells us our public key (like ssb-server whoami on the command line.)
    val rpcRequestBody = "{\"name\": [\"whoami\"],\"type\": \"async\",\"args\":[]}"
    val rpcRequest = encodeRequest(rpcRequestBody, RPCFlag.BodyType.JSON)
    clientHandler!!.sendMessage(rpcRequest)
    for (i in 0..9) {
      clientHandler.sendMessage(encodeRequest(rpcRequestBody, RPCFlag.BodyType.JSON))
    }
    delay(10000)
    secureScuttlebuttVertxClient.stop().join()
  }

  @Test
  @Throws(Exception::class)
  fun testWithPatchwork(@VertxInstance vertx: Vertx) = runBlocking {
    val rpcHandler = makeRPCHandler(vertx)
    val rpcMessages: MutableList<RPCResponse> = ArrayList()
    for (i in 0..9) {
      val function = RPCFunction("whoami")
      val asyncRequest = RPCAsyncRequest(function, ArrayList())
      val res = rpcHandler.makeAsyncRequest(asyncRequest)
      rpcMessages.add(res)
    }
    Assertions.assertEquals(10, rpcMessages.size)
  }

  @Test
  @Throws(Exception::class)
  fun postMessageTest(@VertxInstance vertx: Vertx) = runBlocking {
    val rpcHandler = makeRPCHandler(vertx)
    val rpcMessages: MutableList<RPCResponse> = ArrayList()
    for (i in 0..19) {
      // Note: in a real use case, this would more likely be a Java class with these fields
      val params = HashMap<String, String>()
      params["type"] = "post"
      params["text"] = "test test $i"
      val asyncRequest = RPCAsyncRequest(RPCFunction("publish"), listOf(params))
      val rpcMessageAsyncResult = rpcHandler.makeAsyncRequest(asyncRequest)
      rpcMessages.add(rpcMessageAsyncResult)
    }
    rpcMessages.forEach(Consumer { msg: RPCResponse -> println(msg.asString()) })
  }

  /**
   * We expect this to complete the AsyncResult with an exception.
   */
  @Test
  @Throws(Exception::class)
  fun postMessageThatIsTooLong(@VertxInstance vertx: Vertx) = runBlocking {
    val rpcHandler = makeRPCHandler(vertx)
    val longString = String(CharArray(40000)).replace("\u0000", "a")
    // Note: in a real use case, this would more likely be a Java class with these fields
    val params = HashMap<String, String>()
    params["type"] = "post"
    params["text"] = longString
    val asyncRequest = RPCAsyncRequest(RPCFunction("publish"), listOf(params))
    val exception = Assertions.assertThrows(
      RPCRequestFailedException::class.java
    ) {
      runBlocking {
        rpcHandler.makeAsyncRequest(asyncRequest)
      }
    }
    Assertions.assertEquals(
      "Encoded message must not be larger than 8192 bytes. Current size is 40264",
      exception.cause!!.message
    )
  }

  @Throws(Exception::class)
  private suspend fun makeRPCHandler(vertx: Vertx): RPCHandler {
    val keyPair: Signature.KeyPair = Utils.localKeys
    val networkKeyBase64 = "1KHLiKZvAvjbY1ziZEHMXawbCEIM6qwjCDm3VYRan/s="
    val networkKeyBytes32 = Bytes32.wrap(Base64.decode(networkKeyBase64))
    val env = System.getenv()
    val host = env.getOrDefault("ssb_host", "localhost")
    val port = env.getOrDefault("ssb_port", "8008").toInt()
    val secureScuttlebuttVertxClient = SecureScuttlebuttVertxClient(vertx, keyPair, networkKeyBytes32)
    val onConnect = secureScuttlebuttVertxClient
      .connectTo(
        port,
        host,
        keyPair.publicKey(),
        null
      ) { sender, terminationFn ->
        RPCHandler(
          vertx,
          sender,
          terminationFn
        )
      } as RPCHandler
    return onConnect
  }

  @Test
  @Throws(Exception::class)
  fun streamTest(@VertxInstance vertx: Vertx) = runBlocking {
    val handler = makeRPCHandler(vertx)
    val publicKey: Signature.PublicKey = Utils.localKeys.publicKey()
    val pubKey = "@" + Base64.encode(publicKey.bytes()) + ".ed25519"
    val params: MutableMap<String, String> = HashMap()
    params["id"] = pubKey
    val streamEnded = AsyncResult.incomplete<Void>()
    val streamRequest = RPCStreamRequest(RPCFunction("createUserStream"), listOf<Map<String, String>>(params))
    handler.openStream(
      streamRequest
    ) {
      object : ScuttlebuttStreamHandler {
        override fun onMessage(message: RPCResponse) {
          print(message.asString())
        }

        override fun onStreamEnd() {
          streamEnded.complete(null)
        }

        override fun onStreamError(ex: Exception) {
          streamEnded.completeExceptionally(ex)
        }
      }
    }

    // Wait until the stream is complete
    streamEnded.get()
  }
}
