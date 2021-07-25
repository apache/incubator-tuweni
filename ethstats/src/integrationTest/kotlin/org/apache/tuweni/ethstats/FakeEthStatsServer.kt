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
