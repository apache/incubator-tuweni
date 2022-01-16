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
