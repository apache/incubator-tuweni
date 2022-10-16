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

import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.eth.JSONRPCResponse
import org.apache.tuweni.eth.StringOrLong
import org.apache.tuweni.io.Base64
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.Executors

@Suppress("DEPRECATION")
@ExtendWith(VertxExtension::class)
class JSONRPCServerTest {

  companion object {
    @JvmStatic
    @BeforeAll
    fun checkNotWindows() {
      Assumptions.assumeTrue(
        !System.getProperty("os.name").toLowerCase().contains("win"),
        "Server ports cannot bind on Windows"
      )
    }
  }

  @Test
  fun testNoAuth(@VertxInstance vertx: Vertx): Unit = runBlocking {
    val server = JSONRPCServer(
      vertx,
      port = 0,
      methodHandler = {
        JSONRPCResponse(StringOrLong(3), "")
      },
      coroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )
    server.start()
    try {
      val client = vertx.createHttpClient()
      val request = client.request(HttpMethod.POST, server.port(), server.networkInterface, "/").await()
      val response = request.send("{\"id\":1,\"method\":\"eth_client\",\"params\":[]}").await()
      assertEquals(200, response.statusCode())
    } finally {
      server.stop()
    }
  }

  @Test
  fun testBasicAuth(@VertxInstance vertx: Vertx): Unit = runBlocking {
    val server = JSONRPCServer(
      vertx,
      port = 0,
      methodHandler = {
        JSONRPCResponse(StringOrLong(3), "")
      },
      useBasicAuthentication = true,
      basicAuthenticationPassword = "pass",
      basicAuthenticationUsername = "user",
      basicAuthRealm = "my realm",
      coroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )
    server.start()
    try {
      val client = vertx.createHttpClient()
      val request = client.request(HttpMethod.POST, server.port(), server.networkInterface, "/").await()
      val response = request.send("{\"id\":1,\"method\":\"eth_client\",\"params\":[]}").await()
      assertEquals(401, response.statusCode())
      runBlocking {
        val authedRequest = client.request(HttpMethod.POST, server.port(), server.networkInterface, "/").await()
        authedRequest.putHeader("Authorization", "Basic " + Base64.encodeBytes("user:pass".toByteArray()))
        val authedResponse = authedRequest.send("{\"id\":1,\"method\":\"eth_client\",\"params\":[]}").await()
        assertEquals(200, authedResponse.statusCode())
      }
      runBlocking {
        val authedRequest = client.request(HttpMethod.POST, server.port(), server.networkInterface, "/").await()
        authedRequest.putHeader(
          "Authorization",
          "Basic " + Base64.encodeBytes("user:passbad".toByteArray())
        )
        val authedResponse = authedRequest.send("{\"id\":1,\"method\":\"eth_client\",\"params\":[]}").await()
        assertEquals(401, authedResponse.statusCode())
      }
    } finally {
      server.stop()
    }
  }
}
