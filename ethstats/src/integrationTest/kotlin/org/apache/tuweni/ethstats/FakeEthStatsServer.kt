// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethstats

import io.vertx.core.Vertx
import io.vertx.core.http.ServerWebSocket
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking

class FakeEthStatsServer(val vertx: Vertx, val networkInterface: String, var port: Int) {

  val server = vertx.createHttpServer()
  init {
    server.webSocketHandler(this::connect)
    runBlocking {
      server.listen(port, networkInterface).await()
      port = server.actualPort()
    }
  }
  private val results = mutableListOf<String>()

  fun connect(serverWebSocket: ServerWebSocket) {
    val websocket = serverWebSocket
    websocket.accept()
    websocket.writeTextMessage("{\"emit\":[\"ready\"]}")
    websocket.handler { buffer ->
      results.add(buffer.toString())
    }
  }

  fun getResults(): List<String> {
    return results
  }

  fun waitForMessages(numberOfMessages: Int) {
    for (i in 0..1000) {
      if (getResults().size >= numberOfMessages) {
        return
      }
      Thread.sleep(100)
    }
  }

  fun messagesContain(test: String): Boolean {
    for (message in getResults()) {
      if (message.contains(test)) {
        return true
      }
    }
    return false
  }
}
