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
package org.apache.tuweni.jsonrpc

import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import io.vertx.kotlin.core.http.closeAwait
import io.vertx.kotlin.core.http.listenAwait

class JSONRPCServer(val vertx: Vertx, private val callback: (HttpServerRequest) -> Unit) {

  var server = vertx.createHttpServer()

  suspend fun start() {
    server.requestHandler(callback).listenAwait(0)
  }

  fun port(): Int {
    return server.actualPort()
  }

  suspend fun stop() {
    server.closeAwait()
  }
}
