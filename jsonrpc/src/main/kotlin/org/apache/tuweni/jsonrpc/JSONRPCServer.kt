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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.kotlin.core.http.closeAwait
import io.vertx.kotlin.core.http.listenAwait
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.eth.JSONRPCRequest
import org.apache.tuweni.eth.JSONRPCResponse
import org.apache.tuweni.eth.internalError
import org.apache.tuweni.eth.parseError
import org.slf4j.LoggerFactory
import java.io.IOException
import kotlin.coroutines.CoroutineContext

class JSONRPCServer(val vertx: Vertx, val port: Int, val networkInterface: String = "127.0.0.1", val methodHandler: (JSONRPCRequest) -> JSONRPCResponse, override val coroutineContext: CoroutineContext = Dispatchers.Default) : CoroutineScope {

  companion object {
    val logger = LoggerFactory.getLogger(JSONRPCServer::class.java)
    val mapper = ObjectMapper()
    init {
      mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    }
  }
  private var httpServer: HttpServer? = null

  fun start() = async {
    httpServer = vertx.createHttpServer(HttpServerOptions().setPort(port).setHost(networkInterface))
    httpServer?.exceptionHandler {
      logger.error(it.message, it)
    }
    httpServer?.requestHandler { httpRequest ->
      httpRequest.exceptionHandler {
        logger.error(it.message, it)
        httpRequest.response().end(mapper.writeValueAsString(internalError))
      }
      httpRequest.bodyHandler {
        val responses = mutableListOf<Deferred<JSONRPCResponse>>()
        val requests: Array<JSONRPCRequest>
        try {
          requests = mapper.readerFor(Array<JSONRPCRequest>::class.java).readValue(it.bytes)
        } catch (e: IOException) {
          logger.warn("Invalid request", e)
          httpRequest.response().end(mapper.writeValueAsString(parseError))
          return@bodyHandler
        }
        for (request in requests) {
          responses.add(handleRequest(request))
        }
        // TODO replace with launch.
        runBlocking {
          val readyResponses = responses.awaitAll()
          if (readyResponses.size == 1) {
            httpRequest.response().end(mapper.writeValueAsString(readyResponses.get(0)))
          } else {
            httpRequest.response().end(mapper.writeValueAsString(readyResponses))
          }
        }
      }
    }
    httpServer?.listenAwait()
  }

  private fun handleRequest(request: JSONRPCRequest): Deferred<JSONRPCResponse> = async {
    methodHandler(request)
  }

  fun stop() = async {
    httpServer?.closeAwait()
  }

  fun port(): Int = httpServer?.actualPort() ?: port
}
