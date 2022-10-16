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
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.bytes.Bytes
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
 * protocols, such as TCP, HTTP, UDP and Web sockets.
 *
 * It can be used to contact other Hobbits endpoints, or to expose endpoints to the network.
 *
 * @param vertx Vert.x instance
 * @param incompleteMessageHandler a handler to manage incomplete or invalid messages
 * @param coroutineContext the co-routine context for the transport
 */
class HobbitsTransport(
  private val vertx: Vertx,
  private val incompleteMessageHandler: (Bytes) -> Unit = {},
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : CoroutineScope {

  private val started = AtomicBoolean(false)

  private val httpEndpoints = mutableMapOf<String, Endpoint>()
  private val tcpEndpoints = mutableMapOf<String, Endpoint>()
  private val udpEndpoints = mutableMapOf<String, Endpoint>()
  private val wsEndpoints = mutableMapOf<String, Endpoint>()

  private var exceptionHandler: ((Throwable) -> Unit)? = { }

  private var httpClient: HttpClient? = null
  private var tcpClient: NetClient? = null
  private var udpClient: DatagramSocket? = null

  private val httpServers = mutableMapOf<String, HttpServer>()
  private val tcpServers = mutableMapOf<String, NetServer>()
  private val udpServers = mutableMapOf<String, DatagramSocket>()
  private val wsServers = mutableMapOf<String, HttpServer>()

  /**
   * Sets an exception handler that will be called whenever an exception occurs during transport.
   */
  fun exceptionHandler(handler: (Throwable) -> Unit) {
    exceptionHandler = handler
  }

  /**
   * Creates a new endpoint over http.
   * @param networkInterface the network interface to bind the endpoint to
   * @param port the port to serve traffic from
   * @param requestURI the request URI path to match
   * @param tls whether the endpoint should be secured using TLS
   * @param handler function called when a message is received
   */
  fun createHTTPEndpoint(
    id: String = "default",
    networkInterface: String = "0.0.0.0",
    port: Int = 9337,
    requestURI: String? = null,
    tls: Boolean = false,
    handler: (Message) -> Unit,
    portUpdateListener: (Int) -> Unit = {}
  ) {
    checkNotStarted()
    httpEndpoints[id] = Endpoint(networkInterface, port, requestURI, tls, handler, portUpdateListener)
  }

  /**
   * Creates a new endpoint over tcp persistent connections.
   * @param networkInterface the network interface to bind the endpoint to
   * @param port the port to serve traffic from
   * @param tls whether the endpoint should be secured using TLS
   * @param handler function called when a message is received
   */
  fun createTCPEndpoint(
    id: String = "default",
    networkInterface: String = "0.0.0.0",
    port: Int = 9237,
    tls: Boolean = false,
    handler: (Message) -> Unit,
    portUpdateListener: (Int) -> Unit = {}
  ) {
    checkNotStarted()
    tcpEndpoints[id] = Endpoint(networkInterface, port, null, tls, handler, portUpdateListener)
  }

  /**
   * Creates a new endpoint over UDP connections.
   * @param networkInterface the network interface to bind the endpoint to
   * @param port the port to serve traffic from
   * @param tls whether the endpoint should be secured using TLS
   * @param handler function called when a message is received
   */
  fun createUDPEndpoint(
    id: String = "default",
    networkInterface: String = "0.0.0.0",
    port: Int = 9137,
    handler: (Message) -> Unit
  ) {
    checkNotStarted()
    udpEndpoints[id] = Endpoint(networkInterface, port, null, false, handler)
  }

  /**
   * Creates a new endpoint over websocket connections.
   * @param networkInterface the network interface to bind the endpoint to
   * @param port the port to serve traffic from
   * @param requestURI the request URI path to match
   * @param tls whether the endpoint should be secured using TLS
   * @param handler function called when a message is received
   */
  fun createWSEndpoint(
    id: String = "default",
    networkInterface: String = "0.0.0.0",
    port: Int = 9037,
    requestURI: String? = null,
    tls: Boolean = false,
    handler: (Message) -> Unit,
    portUpdateListener: (Int) -> Unit = {}
  ) {
    checkNotStarted()
    wsEndpoints[id] = Endpoint(networkInterface, port, requestURI, tls, handler, portUpdateListener)
  }

  /**
   * Sends a message using the transport specified.
   *
   */
  suspend fun sendMessage(message: Message, transport: Transport, host: String, port: Int, requestURI: String = "") {
    checkStarted()
    val completion = AsyncCompletion.incomplete()
    when (transport) {
      Transport.HTTP -> {
        val req = httpClient!!.request(HttpMethod.POST, port, host, requestURI).await()
          .exceptionHandler(exceptionHandler)
        val response = req.send(Buffer.buffer(message.toBytes().toArrayUnsafe())).await()
        if (response.statusCode() == 200) {
          completion.complete()
        } else {
          completion.completeExceptionally(RuntimeException("${response.statusCode()}"))
        }
      }
      Transport.TCP -> {
        tcpClient!!.connect(port, host) { res ->
          if (res.failed()) {
            completion.completeExceptionally(res.cause())
          } else {
            res.result().exceptionHandler(exceptionHandler).end(Buffer.buffer(message.toBytes().toArrayUnsafe()))
            completion.complete()
          }
        }
      }
      Transport.UDP -> {
        udpClient!!.send(Buffer.buffer(message.toBytes().toArrayUnsafe()), port, host) { handler ->
          if (handler.failed()) {
            completion.completeExceptionally(handler.cause())
          } else {
            completion.complete()
          }
        }
      }
      Transport.WS -> {
        try {
          val websocket = httpClient!!.webSocket(
            port,
            host,
            requestURI
          ).await()
          websocket.exceptionHandler(exceptionHandler)
          websocket.writeBinaryMessage(Buffer.buffer(message.toBytes().toArrayUnsafe())).await()
          websocket.end().await()
          completion.complete()
        } catch (e: Exception) {
          completion.completeExceptionally(e)
        }
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

  /**
   * Starts the hobbits transport.
   */
  suspend fun start() {
    if (started.compareAndSet(false, true)) {
      httpClient = vertx.createHttpClient()
      tcpClient = vertx.createNetClient()
      udpClient = vertx.createDatagramSocket().exceptionHandler(exceptionHandler)

      val completions = mutableListOf<AsyncCompletion>()
      for ((id, endpoint) in httpEndpoints) {
        val completion = AsyncCompletion.incomplete()
        val httpServer = vertx.createHttpServer()
        httpServers[id] = httpServer

        httpServer.requestHandler {
          if (endpoint.requestURI == null || it.path().startsWith(endpoint.requestURI)) {
            it.bodyHandler {
              val bytes = Bytes.wrapBuffer(it)
              val message = Message.readMessage(bytes)
              if (message == null) {
                incompleteMessageHandler(bytes)
              } else {
                endpoint.handler(message)
              }
            }
            it.response().statusCode = 200
            it.response().end()
          } else {
            it.response().statusCode = 404
            it.response().end()
          }
        }.listen(endpoint.port, endpoint.networkInterface) {
          if (it.failed()) {
            completion.completeExceptionally(it.cause())
          } else {
            if (endpoint.port == 0) {
              endpoint.updatePort(it.result().actualPort())
            }
            completion.complete()
          }
        }
        completions.add(completion)
      }
      for ((id, endpoint) in tcpEndpoints) {
        val completion = AsyncCompletion.incomplete()
        val tcpServer = vertx.createNetServer()
        tcpServers[id] = tcpServer
        tcpServer.connectHandler { connectHandler ->
          connectHandler.handler { buffer ->
            val bytes = Bytes.wrapBuffer(buffer)
            val message = Message.readMessage(bytes)
            if (message == null) {
              incompleteMessageHandler(bytes)
            } else {
              endpoint.handler(message)
            }
          }
        }.listen(endpoint.port, endpoint.networkInterface) {
          if (it.failed()) {
            completion.completeExceptionally(it.cause())
          } else {
            if (endpoint.port == 0) {
              endpoint.updatePort(it.result().actualPort())
            }
            completion.complete()
          }
        }
        completions.add(completion)
      }
      for ((id, endpoint) in udpEndpoints) {
        val completion = AsyncCompletion.incomplete()

        val udpServer = vertx.createDatagramSocket()
        udpServers[id] = udpServer

        udpServer.handler { packet ->
          val bytes = Bytes.wrapBuffer(packet.data())
          val message = Message.readMessage(bytes)
          if (message == null) {
            incompleteMessageHandler(bytes)
          } else {
            endpoint.handler(message)
          }
        }.listen(endpoint.port, endpoint.networkInterface) {
          if (it.failed()) {
            completion.completeExceptionally(it.cause())
          } else {
            completion.complete()
          }
        }
        completions.add(completion)
      }
      for ((id, endpoint) in wsEndpoints) {
        val completion = AsyncCompletion.incomplete()
        val httpServer = vertx.createHttpServer()
        wsServers[id] = httpServer

        @Suppress("DEPRECATION")
        httpServer.webSocketHandler {
          if (endpoint.requestURI == null || it.path().startsWith(endpoint.requestURI)) {
            it.accept()

            it.binaryMessageHandler { buffer ->
              try {
                val bytes = Bytes.wrapBuffer(buffer)
                val message = Message.readMessage(bytes)
                if (message == null) {
                  incompleteMessageHandler(bytes)
                } else {
                  endpoint.handler(message)
                }
              } finally {
                it.end()
              }
            }
          } else {
            it.reject()
          }
        }.listen(endpoint.port, endpoint.networkInterface) {
          if (it.failed()) {
            completion.completeExceptionally(it.cause())
          } else {
            if (endpoint.port == 0) {
              endpoint.updatePort(it.result().actualPort())
            }
            completion.complete()
          }
        }
        completions.add(completion)
      }
      AsyncCompletion.allOf(completions).await()
    }
  }

  /**
   * Stops the hobbits transport.
   */
  fun stop() {
    if (started.compareAndSet(true, false)) {
      httpClient!!.close()
      tcpClient!!.close()
      udpClient!!.close()
      for (server in httpServers.values) {
        server.close()
      }
      for (server in tcpServers.values) {
        server.close()
      }
      for (server in udpServers.values) {
        server.close()
      }
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

internal data class Endpoint(
  val networkInterface: String,
  var port: Int,
  val requestURI: String?,
  val tls: Boolean,
  val handler: (Message) -> Unit,
  val portUpdateListener: (Int) -> Unit = {}
) {

  fun updatePort(newPort: Int) {
    port = newPort
    portUpdateListener(newPort)
  }
}

/**
 * Transport types supported.
 */
enum class Transport() {
  /**
   * HTTP transport
   */
  HTTP,

  /**
   * TCP transport
   */
  TCP,

  /**
   * UDP transport
   */
  UDP,

  /**
   * Websocket transport
   */
  WS
}
