// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
