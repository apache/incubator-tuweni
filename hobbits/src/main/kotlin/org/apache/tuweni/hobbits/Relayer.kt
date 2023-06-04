// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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

  private val transport = HobbitsTransport(vertx, { println("Invalid message: $it") }, coroutineContext)
  init {
    val toURI = URI.create(to)
    val uri = URI.create(bind)
    when (uri.scheme) {
      "http" -> {
        transport.createHTTPEndpoint(
          networkInterface = uri.host,
          port = uri.port,
          handler = {
            async {
              interceptor(it)
              transport.sendMessage(it, Transport.HTTP, toURI.host, toURI.port, toURI.path)
            }
          }
        )
      }
      "tcp" -> {
        transport.createTCPEndpoint(
          networkInterface = uri.host,
          port = uri.port,
          handler = {
            async {
              interceptor(it)
              transport.sendMessage(it, Transport.TCP, toURI.host, toURI.port, toURI.path)
            }
          }
        )
      }
      "udp" -> {
        transport.createUDPEndpoint(
          networkInterface = uri.host,
          port = uri.port,
          handler = {
            async {
              interceptor(it)
              transport.sendMessage(it, Transport.UDP, toURI.host, toURI.port, toURI.path)
            }
          }
        )
      }
      "ws" -> {
        transport.createWSEndpoint(
          networkInterface = uri.host,
          port = uri.port,
          handler = {
            async {
              interceptor(it)
              transport.sendMessage(it, Transport.WS, toURI.host, toURI.port, toURI.path)
            }
          }
        )
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
