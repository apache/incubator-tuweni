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
@file:Suppress("DEPRECATION")

package org.apache.tuweni.jsonrpc

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.net.TrustOptions
import io.vertx.core.tracing.TracingPolicy
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import io.vertx.ext.auth.authorization.Authorization
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BasicAuthHandler
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.JSONRPCRequest
import org.apache.tuweni.eth.JSONRPCResponse
import org.apache.tuweni.eth.internalError
import org.apache.tuweni.eth.parseError
import org.apache.tuweni.net.ip.IPRangeChecker
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.IllegalArgumentException
import kotlin.coroutines.CoroutineContext

class JSONRPCServer(
  val vertx: Vertx,
  private val port: Int,
  val networkInterface: String = "127.0.0.1",
  val ssl: Boolean = false,
  val trustOptions: TrustOptions? = null,
  val useBasicAuthentication: Boolean = false,
  val basicAuthenticationUsername: String? = null,
  val basicAuthenticationPassword: String? = null,
  val basicAuthRealm: String = "Apache Tuweni JSON-RPC proxy",
  val ipRangeChecker: IPRangeChecker = IPRangeChecker.allowAll(),
  val openTelemetry: OpenTelemetry = OpenTelemetry.noop(),
  override val coroutineContext: CoroutineContext = Dispatchers.Default,
  val methodHandler: suspend (JSONRPCRequest) -> JSONRPCResponse
) : CoroutineScope {

  companion object {
    val logger = LoggerFactory.getLogger(JSONRPCServer::class.java)
    val mapper = ObjectMapper()

    init {
      mapper.registerModule(EthJsonModule())
      mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    }
  }

  private var httpServer: HttpServer? = null

  init {
    if (useBasicAuthentication) {
      if (basicAuthenticationUsername == null) {
        throw IllegalArgumentException("Cannot use basic authentication without specifying a username")
      }
      if (basicAuthenticationPassword == null) {
        throw IllegalArgumentException("Cannot use basic authentication without specifying a password")
      }
    }
  }

  suspend fun start() {
    val serverOptions = HttpServerOptions().setPort(port).setHost(networkInterface).setSsl(ssl).setTracingPolicy(TracingPolicy.ALWAYS)
    trustOptions?.let {
      serverOptions.setTrustOptions(it)
    }
    httpServer = vertx.createHttpServer(serverOptions)
    httpServer?.connectionHandler {
      val remoteAddress = it.remoteAddress().hostAddress()
      if (!ipRangeChecker.check(remoteAddress)) {
        logger.debug("Rejecting IP {}", remoteAddress)
        it.close()
      }
    }
    httpServer?.exceptionHandler {
      logger.error(it.message, it)
    }
    val router = Router.router(vertx)
    if (useBasicAuthentication) {
      router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)))
      val basicAuthHandler = BasicAuthHandler.create(
        { authInfo, resultHandler ->
          if (basicAuthenticationUsername == authInfo.getString("username") && basicAuthenticationPassword == authInfo.getString(
              "password"
            )
          ) {
            resultHandler.handle(Future.succeededFuture(JSONRPCUser(authInfo)))
          } else {
            resultHandler.handle(Future.failedFuture("Invalid credentials"))
          }
        },
        basicAuthRealm
      )
      router.route().handler(basicAuthHandler)
    }
    router.route().handler { context ->
      val tracer = openTelemetry.getTracer("jsonrpcserver")
      val span = tracer.spanBuilder("handleRequest").setSpanKind(SpanKind.SERVER).startSpan()
      val httpRequest = context.request()
      httpRequest.exceptionHandler {
        logger.error(it.message, it)
        httpRequest.response().end(mapper.writeValueAsString(internalError))
        span.setStatus(StatusCode.ERROR)
        span.end()
      }
      httpRequest.bodyHandler {
        var requests: Array<JSONRPCRequest>
        try {
          requests = mapper.readerFor(Array<JSONRPCRequest>::class.java).readValue(it.bytes)
        } catch (e: IOException) {
          try {
            requests = arrayOf(mapper.readerFor(JSONRPCRequest::class.java).readValue(it.bytes))
          } catch (e: IOException) {
            logger.warn("Invalid request", e)
            httpRequest.response().end(mapper.writeValueAsString(parseError))
            span.setStatus(StatusCode.ERROR)
            span.end()
            return@bodyHandler
          }
        }

        launch(vertx.dispatcher()) {
          val responses = mutableListOf<Deferred<JSONRPCResponse>>()
          for (request in requests) {
            logger.trace("Request {}", request)
            responses.add(handleRequest(request))
          }
          val readyResponses = responses.awaitAll()
          if (readyResponses.size == 1) {
            httpRequest.response().end(mapper.writeValueAsString(readyResponses.get(0)))
          } else {
            httpRequest.response().end(mapper.writeValueAsString(readyResponses))
          }
          span.end()
        }
      }
    }
    httpServer?.requestHandler(router)
    httpServer?.listen()?.await()
    logger.info("Started JSON-RPC server on $networkInterface:${port()}")
  }

  private suspend fun handleRequest(request: JSONRPCRequest): Deferred<JSONRPCResponse> =
    coroutineScope {
      async {
        methodHandler(request)
      }
    }

  suspend fun stop() {
    httpServer?.close()?.await()
  }

  fun port(): Int = httpServer?.actualPort() ?: port
}

private class JSONRPCUser(val principal: JsonObject) : User {
  override fun attributes(): JsonObject {
    return JsonObject()
  }

  override fun isAuthorized(authority: Authorization?, resultHandler: Handler<AsyncResult<Boolean>>?): User {
    resultHandler?.handle(Future.succeededFuture(true))
    return this
  }

  override fun clearCache(): User {
    return this
  }

  override fun principal(): JsonObject = principal

  override fun setAuthProvider(authProvider: AuthProvider?) {
  }
}
