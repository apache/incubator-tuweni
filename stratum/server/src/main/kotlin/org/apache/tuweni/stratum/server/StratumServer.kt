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
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.core.net.closeAwait
import io.vertx.kotlin.core.net.listenAwait
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Server capable of handling connections from Stratum clients, authenticate and serve content for them.
 */
class StratumServer(
  val vertx: Vertx,
  private val port: Int,
  val networkInterface: String,
  extranonce: String = "",
  submitCallback: (PoWSolution) -> Boolean,
  seedSupplier: () -> Bytes32,
  hashrateCallback: (Bytes, Long) -> Boolean,
) {

  companion object {
    val logger = LoggerFactory.getLogger(StratumServer::class.java)
  }

  private val protocols = arrayOf(
    Stratum1EthProxyProtocol(submitCallback, seedSupplier, hashrateCallback),
    Stratum1Protocol(extranonce, submitCallback = submitCallback, seedSupplier = seedSupplier)
  )

  private val started = AtomicBoolean(false)
  private var tcpServer: NetServer? = null

  suspend fun start() {
    if (started.compareAndSet(false, true)) {
      val server = vertx.createNetServer(
        NetServerOptions().setPort(port).setHost(networkInterface).setTcpKeepAlive(true)
      )
      server.exceptionHandler { e -> logger.error(e.message, e) }
      server.connectHandler(this::handleConnection)
      server.listenAwait()
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
      tcpServer?.closeAwait()
    }
  }

  fun port(): Int {
    return tcpServer?.actualPort() ?: port
  }
}
