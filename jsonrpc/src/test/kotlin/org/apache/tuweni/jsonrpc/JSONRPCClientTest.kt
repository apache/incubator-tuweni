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
package org.apache.tuweni.jsonrpc

import io.vertx.core.Handler
import io.vertx.core.Vertx
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.JSONRPCRequest
import org.apache.tuweni.eth.JSONRPCResponse
import org.apache.tuweni.eth.StringOrLong
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.net.ConnectException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(BouncyCastleExtension::class, VertxExtension::class)
class JSONRPCClientTest {

  companion object {
    val handler = AtomicReference<Handler<JSONRPCRequest>>()
    var server: JSONRPCServer? = null

    @JvmStatic
    @BeforeAll
    fun runServer(@VertxInstance vertx: Vertx): Unit = runBlocking {
      Assumptions.assumeTrue(!System.getProperty("os.name").lowercase().contains("win"), "Server ports cannot bind on Windows")
      server = JSONRPCServer(
        vertx,
        port = 0,
        methodHandler = {
          handler.get().handle(it)
          JSONRPCResponse(StringOrLong(3), "")
        },
        coroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
      )
      server!!.start()
    }

    @JvmStatic
    @AfterAll
    fun stopServer(): Unit = runBlocking {
      server?.stop()
    }
  }

  @Test
  fun testSendTransaction(@VertxInstance vertx: Vertx) = runBlocking {
    val keyPair =
      SECP256K1.KeyPair.fromSecretKey(SECP256K1.SecretKey.fromBytes(Bytes32.rightPad(Bytes.fromHexString("0102"))))
    JSONRPCClient(vertx, "http://localhost:" + server!!.port()).use {
      val tx = Transaction(
        UInt256.ONE,
        Wei.valueOf(2),
        Gas.valueOf(13),
        Address.fromHexString("0x0102030405060708090a0b0c0d0e0f0102030405"),
        Wei.valueOf(42),
        Bytes.EMPTY,
        keyPair

      )
      val sent = CompletableDeferred<String>()
      handler.set { request ->
        sent.complete(request.params.get(0) as String)
        JSONRPCResponse(request.id, "")
      }

      val hash = it.sendRawTransaction(tx)
      assertEquals("", hash)
      assertEquals(
        "0xf85d01020d940102030405060708090a0b0c0d0e0f01020304052a801ba01356bce3ee0043871c3bb" +
          "289597a903985d6f0235446283069031a613b286aeca02f7cf0fa0e4b160bc4d48fb256b4989f067de773b" +
          "0ac4c721d5222e4e38cc6ca",
        sent.await()
      )
    }
  }

  @Test
  fun testGetBalanceToMissingClient(@VertxInstance vertx: Vertx) {
    JSONRPCClient(vertx, "http://localhost:1234").use {
      assertThrows<ConnectException> {
        runBlocking { it.getBalance_latest(Address.fromHexString("0x0102030405060708090a0b0c0d0e0f0102030405")) }
      }
    }
  }
}
