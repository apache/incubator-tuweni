// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.proxy

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.coroutines.await
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

class TcpDownstream(
  val vertx: Vertx,
  val site: String,
  val host: String,
  val port: Int,
  val client: ProxyClient,
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : CoroutineScope {

  companion object {
    val logger = LoggerFactory.getLogger(TcpDownstream::class.java)
  }

  var tcpServer: NetServer? = null

  fun start() = async {
    val server = vertx.createNetServer()
    server.connectHandler {
      handleSocket(it)
    }.listen(port, host).await()
    tcpServer = server
    logger.info("Started downstream proxy server on $host:$port")
  }

  fun close() = async {
    tcpServer?.close()
  }

  fun handleSocket(socket: NetSocket) {
    socket.handler {
      async {
        val response = client.request(site, Bytes.wrapBuffer(it))
        socket.write(Buffer.buffer(response.toArrayUnsafe()))
      }
    }.exceptionHandler {
      logger.error("Error proxying request", it)
    }
  }
}
