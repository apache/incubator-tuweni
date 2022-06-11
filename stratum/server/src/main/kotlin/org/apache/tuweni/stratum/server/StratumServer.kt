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
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.KeyCertOptions
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket
import io.vertx.core.net.SelfSignedCertificate
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * Simple main function to run the server with a self-signed certificate.
 */
fun main() = runBlocking {
  val selfSignedCertificate = SelfSignedCertificate.create()
  val server = StratumServer(
    Vertx.vertx(), port = 10000, networkInterface = "0.0.0.0", sslOptions = selfSignedCertificate.keyCertOptions(),
    submitCallback = { true }, seedSupplier = { Bytes32.random() }, hashrateCallback = { _, _ -> true }
  )
  server.start()
  Runtime.getRuntime().addShutdownHook(
    Thread {
      runBlocking {
        StratumServer.logger.info("Shutting down...")
        server.stop()
      }
    }
  )
}

/**
 * Server capable of handling connections from Stratum clients, authenticate and serve content for them.
 */
class StratumServer(
  val vertx: Vertx,
  private val port: Int,
  val networkInterface: String,
  private val sslOptions: KeyCertOptions?,
  extranonce: String = "",
  submitCallback: suspend (PoWSolution) -> Boolean,
  seedSupplier: () -> Bytes32,
  hashrateCallback: (Bytes, Long) -> Boolean,
  override val coroutineContext: CoroutineContext = vertx.dispatcher(),
) : CoroutineScope {

  companion object {
    val logger = LoggerFactory.getLogger(StratumServer::class.java)
  }

  private val protocols = arrayOf(
    Stratum1EthProxyProtocol(submitCallback, seedSupplier, hashrateCallback, this.coroutineContext),
    Stratum1Protocol(extranonce, submitCallback = submitCallback, seedSupplier = seedSupplier, coroutineContext = this.coroutineContext)
  )

  private val started = AtomicBoolean(false)
  private var tcpServer: NetServer? = null

  fun setNewWork(powInput: PoWInput) {
    for (protocol in protocols) {
      launch {
        protocol.setCurrentWorkTask(powInput)
      }
    }
  }

  suspend fun start() {
    if (started.compareAndSet(false, true)) {
      val options = NetServerOptions().setPort(port).setHost(networkInterface).setTcpKeepAlive(true)
      sslOptions?.let {
        options.setKeyCertOptions(it)
      }
      val server = vertx.createNetServer(options)
      server.exceptionHandler { e -> logger.error(e.message, e) }
      server.connectHandler(this::handleConnection)
      server.listen().await()
      tcpServer = server
    }
  }

  private fun handleConnection(socket: NetSocket) {
    socket.exceptionHandler { e -> logger.error(e.message, e) }
    val conn = StratumConnection(
      protocols, socket::close
    ) { bytes -> socket.write(Buffer.buffer(bytes)) }
    socket.handler(conn::handleBuffer)
    socket.closeHandler {
      logger.trace("Client initiated socket close")
      conn.close()
    }
  }

  suspend fun stop() {
    if (started.compareAndSet(true, false)) {
      tcpServer?.close()?.await()
    }
  }

  fun port(): Int {
    return tcpServer?.actualPort() ?: port
  }
}
