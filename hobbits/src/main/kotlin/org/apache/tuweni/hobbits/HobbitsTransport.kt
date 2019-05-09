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
import io.vertx.core.buffer.Buffer
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.coroutines.await
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * Hobbits is a peer-to-peer transport stack specified at https://www.github.com/deltap2p/hobbits.
 *
 * This class works as a transport mechanism that can leverage a variety of network transport
 * mechanisms, such as TCP, HTTP, UDP and Web sockets.
 *
 * It can be used to contact other Hobbits endpoints, or to expose endpoints to the network.
 *
 */
class HobbitsTransport(
  private val vertx: Vertx,
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : CoroutineScope {

  private val started = AtomicBoolean(false)

  private val httpEndpoints = mutableMapOf<String, Endpoint>()
  private val tcpEndpoints = mutableMapOf<String, Endpoint>()
  private val udpEndpoints = mutableMapOf<String, Endpoint>()
  private val wsEndpoints = mutableMapOf<String, Endpoint>()

  private var httpClient: HttpClient? = null
  private var tcpClient: NetClient? = null
  private var udpClient: DatagramSocket? = null

  private var httpServer: HttpServer? = null
  private var tcpServer: NetServer? = null

  /**
   * Creates a new endpoint over http.
   * @param networkInterface the network interface to bind the endpoint to
   * @param port the port to serve traffic from
   * @param tls whether the endpoint should be secured using TLS
   */
  fun createHTTPEndpoint(
    id: String = "default",
    networkInterface: String = "0.0.0.0",
    port: Int = 9337,
    tls: Boolean = false
  ) {
    checkNotStarted()
    httpEndpoints[id] = Endpoint(networkInterface, port, tls)
  }

  /**
   * Creates a new endpoint over tcp persistent connections.
   * @param networkInterface the network interface to bind the endpoint to
   * @param port the port to serve traffic from
   * @param tls whether the endpoint should be secured using TLS
   */
  fun createTCPEndpoint(
    id: String = "default",
    networkInterface: String = "0.0.0.0",
    port: Int = 9237,
    tls: Boolean = false
  ) {
    checkNotStarted()
    tcpEndpoints[id] = Endpoint(networkInterface, port, tls)
  }

  /**
   * Creates a new endpoint over UDP connections.
   * @param networkInterface the network interface to bind the endpoint to
   * @param port the port to serve traffic from
   * @param tls whether the endpoint should be secured using TLS
   */
  fun createUDPEndpoint(id: String = "default", networkInterface: String = "0.0.0.0", port: Int = 9137) {
    checkNotStarted()
    udpEndpoints[id] = Endpoint(networkInterface, port, false)
  }

  /**
   * Creates a new endpoint over websocket connections.
   * @param networkInterface the network interface to bind the endpoint to
   * @param port the port to serve traffic from
   * @param tls whether the endpoint should be secured using TLS
   */
  fun createWSEndpoint(
    id: String = "default",
    networkInterface: String = "0.0.0.0",
    port: Int = 9037,
    tls: Boolean = false
  ) {
    checkNotStarted()
    wsEndpoints[id] = Endpoint(networkInterface, port, tls)
  }

  /**
   * Sends a message using the transport specified.
   *
   */
  suspend fun sendMessage(message: Message, transport: Transport, host: String, port: Int) {
    checkStarted()
    val completion = AsyncCompletion.incomplete()
    when (transport) {
      Transport.HTTP -> {
        @Suppress("DEPRECATION")
        val req = httpClient!!.request(HttpMethod.POST, port, host, "/").handler {
          if (it.statusCode() == 200) {
            completion.complete()
          } else {
            completion.completeExceptionally(RuntimeException())
          }
        }
        req.end(Buffer.buffer(message.toBytes().toArrayUnsafe()))
      }
      Transport.TCP -> {
        tcpClient!!.connect(port, host) { res ->
          if (res.failed()) {
            completion.completeExceptionally(res.cause())
          } else {
            res.result().end(Buffer.buffer(message.toBytes().toArrayUnsafe()))
            completion.complete()
          }
        }
      }
      Transport.UDP -> {
        TODO()
      }
      Transport.WS -> {
        TODO()
      }
    }
    completion.await()
  }

  private fun findEndpoint(endpointId: String): Endpoint? {
    val uri = URI.create(endpointId)
    if (uri.scheme == null) {
      if (httpEndpoints.containsKey(endpointId)) {
        return httpEndpoints[endpointId]
      }
      if (tcpEndpoints.containsKey(endpointId)) {
        return tcpEndpoints[endpointId]
      }
      if (udpEndpoints.containsKey(endpointId)) {
        return udpEndpoints[endpointId]
      }
      if (wsEndpoints.containsKey(endpointId)) {
        return wsEndpoints[endpointId]
      }
      return null
    }
    when (uri.scheme) {
      "http" -> {
        return httpEndpoints[uri.host]
      }
      "tcp" -> {
        return tcpEndpoints[uri.host]
      }
      "udp" -> {
        return udpEndpoints[uri.host]
      }
      "ws" -> {
        return wsEndpoints[uri.host]
      }
      else -> {
        throw IllegalArgumentException("Unsupported endpoint $endpointId")
      }
    }
  }

  suspend fun start() {
    if (started.compareAndSet(false, true)) {
      httpClient = vertx.createHttpClient()
      tcpClient = vertx.createNetClient()
      udpClient = vertx.createDatagramSocket()

      httpServer = vertx.createHttpServer()
      tcpServer = vertx.createNetServer()

      val completions = mutableListOf<AsyncCompletion>()
      for (endpoint in httpEndpoints.values) {
        val completion = AsyncCompletion.incomplete()
        httpServer!!.listen(endpoint.port, endpoint.networkInterface) {
          if (it.failed()) {
            completion.completeExceptionally(it.cause())
          } else {
            completion.complete()
          }
        }
        completions.add(completion)
      }
      for (endpoint in tcpEndpoints.values) {
        val completion = AsyncCompletion.incomplete()
        tcpServer!!.listen(endpoint.port, endpoint.networkInterface) {
          if (it.failed()) {
            completion.completeExceptionally(it.cause())
          } else {
            completion.complete()
          }
        }
        completions.add(completion)
      }
      AsyncCompletion.allOf(completions).await()
    }
  }

  fun stop() {
    if (started.compareAndSet(true, false)) {
      httpClient!!.close()
      tcpClient!!.close()
      udpClient!!.close()
    }
  }

  private fun checkNotStarted() {
    if (started.get()) {
      throw IllegalStateException("Server already started")
    }
  }

  private fun checkStarted() {
    if (!started.get()) {
      throw IllegalStateException("Server not started")
    }
  }
}

internal data class Endpoint(val networkInterface: String, val port: Int, val tls: Boolean)

enum class Transport() {
  HTTP,
  TCP,
  UDP,
  WS
}
