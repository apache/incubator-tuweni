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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.net.URI
import kotlin.coroutines.CoroutineContext

/**
 * Relays messages between two endpoints, with an interceptor reading passed messages.
 *
 * @param vertx a Vert.x instance
 * @param bind the endpoint to bind to
 * @param to the endpoint to send to
 * @param interceptor the interceptor function consuming messages being relayed
 * @param coroutineContext the coroutine context of the relayer
 */
class Relayer(
  vertx: Vertx,
  bind: String,
  to: String,
  interceptor: (Message) -> Unit,
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : CoroutineScope {

  private val transport = HobbitsTransport(vertx, coroutineContext)
  init {
    val toURI = URI.create(to)
    val uri = URI.create(bind)
    when (uri.scheme) {
      "http" -> {
        transport.createHTTPEndpoint(networkInterface = uri.host, port = uri.port, handler = {
          async {
            interceptor(it)
            transport.sendMessage(it, Transport.HTTP, toURI.host, toURI.port, toURI.path)
          }
        })
      }
      "tcp" -> {
        transport.createTCPEndpoint(networkInterface = uri.host, port = uri.port, handler = {
          async {
            interceptor(it)
            transport.sendMessage(it, Transport.TCP, toURI.host, toURI.port, toURI.path)
          }
        })
      }
      "udp" -> {
        transport.createUDPEndpoint(networkInterface = uri.host, port = uri.port, handler = {
          async {
            interceptor(it)
            transport.sendMessage(it, Transport.UDP, toURI.host, toURI.port, toURI.path)
          }
        })
      }
      "ws" -> {
        transport.createWSEndpoint(networkInterface = uri.host, port = uri.port, handler = {
          async {
            interceptor(it)
            transport.sendMessage(it, Transport.WS, toURI.host, toURI.port, toURI.path)
          }
        })
      }
    }
  }

  /**
   * Starts the relayer.
   */
  suspend fun start() {
    transport.start()
  }

  /**
   * Stops the relayer.
   */
  fun stop() {
    transport.stop()
  }
}
