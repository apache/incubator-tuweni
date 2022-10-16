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
package org.apache.tuweni.jsonrpc.methods

import com.netflix.concurrency.limits.limit.FixedLimit
import com.netflix.concurrency.limits.limiter.SimpleLimiter
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.common.Labels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.tuweni.eth.JSONRPCRequest
import org.apache.tuweni.eth.JSONRPCResponse
import org.apache.tuweni.eth.StringOrLong
import org.apache.tuweni.eth.methodNotEnabled
import org.apache.tuweni.eth.methodNotFound
import org.apache.tuweni.eth.tooManyRequests
import org.apache.tuweni.kv.KeyValueStore
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

class MethodsRouter(val methodsMap: Map<String, suspend (JSONRPCRequest) -> JSONRPCResponse>) {

  suspend fun handleRequest(request: JSONRPCRequest): JSONRPCResponse {
    val methodHandler = methodsMap[request.method]
    if (methodHandler == null) {
      return methodNotFound
    } else {
      return methodHandler(request)
    }
  }
}

class MeteredHandler(
  private val successCounter: LongCounter,
  private val failCounter: LongCounter,
  private val delegateHandler: suspend (JSONRPCRequest) -> JSONRPCResponse
) {

  suspend fun handleRequest(request: JSONRPCRequest): JSONRPCResponse {
    val resp = delegateHandler(request)
    val labels = Labels.of("method", request.method)
    if (resp.error != null) {
      failCounter.add(1, labels)
    } else {
      successCounter.add(1, labels)
    }
    return resp
  }
}

class MethodAllowListHandler(
  private val allowedMethods: List<String>,
  private val delegateHandler: suspend (JSONRPCRequest) -> JSONRPCResponse
) {

  suspend fun handleRequest(request: JSONRPCRequest): JSONRPCResponse {
    var found = false
    for (method in allowedMethods) {
      if (request.method.startsWith(method)) {
        found = true
        break
      }
    }
    if (!found) {
      return methodNotEnabled.copy(id = request.id)
    }
    return delegateHandler(request)
  }
}

class ThrottlingHandler(
  private val threshold: Int,
  private val delegateHandler: suspend (JSONRPCRequest) -> JSONRPCResponse
) {

  private val limiter: SimpleLimiter<Void> = SimpleLimiter
    .newBuilder()
    .limit(FixedLimit.of(threshold))
    .build()

  suspend fun handleRequest(request: JSONRPCRequest): JSONRPCResponse {
    val listener = limiter.acquire(null)
    if (listener.isEmpty) {
      return tooManyRequests.copy(id = request.id)
    } else {
      try {
        val response = delegateHandler(request)
        listener.get().onSuccess()
        return response
      } catch (t: Throwable) {
        listener.get().onDropped()
        throw RuntimeException(t)
      }
    }
  }
}

class CachingHandler(
  private val allowedMethods: List<String>,
  private val cacheStore: KeyValueStore<String, JSONRPCResponse>,
  private val cacheHitCounter: LongCounter,
  private val cacheMissCounter: LongCounter,
  private val delegateHandler: suspend (JSONRPCRequest) -> JSONRPCResponse
) {

  suspend fun handleRequest(request: JSONRPCRequest): JSONRPCResponse {
    var found = false
    for (method in allowedMethods) {
      if (request.method.startsWith(method)) {
        found = true
        break
      }
    }
    if (!found) {
      return delegateHandler(request)
    } else {
      val serializedRequest = request.serializeRequest()
      val response = cacheStore.get(serializedRequest)
      return if (response == null) {
        cacheMissCounter.add(1)
        val newResponse = delegateHandler(request)
        if (newResponse.error == null) {
          cacheStore.put(serializedRequest, newResponse)
        }
        newResponse
      } else {
        cacheHitCounter.add(1)
        response.copy(id = request.id)
      }
    }
  }
}

class CachingPollingHandler(
  private val cachedRequests: List<JSONRPCRequest>,
  private val pollPeriodMillis: Long,
  private val cacheStore: KeyValueStore<JSONRPCRequest, JSONRPCResponse>,
  private val cacheHitCounter: LongCounter,
  private val cacheMissCounter: LongCounter,
  override val coroutineContext: CoroutineContext = Dispatchers.Default,
  private val delegateHandler: suspend (JSONRPCRequest) -> JSONRPCResponse
) : CoroutineScope {

  companion object {
    private val logger = LoggerFactory.getLogger(CachingPollingHandler::class.java)
  }

  init {
    poll()
  }

  private fun poll() {
    launch {
      try {
        var id = 1337L
        for (cachedRequest in cachedRequests) {
          val newResponse = delegateHandler(cachedRequest.copy(id = StringOrLong(id)))
          id++
          if (newResponse.error == null) {
            cacheStore.put(cachedRequest, newResponse)
          } else {
            logger.warn("{}, got error:\n{}", cachedRequest, newResponse.error)
          }
        }
      } catch (e: Exception) {
        logger.error("Error polling JSON-RPC endpoint", e)
      }
      delay(pollPeriodMillis)
      poll()
    }
  }

  suspend fun handleRequest(request: JSONRPCRequest): JSONRPCResponse {
    var found = false
    if (cachedRequests.contains(request)) {
      found = true
    }
    if (!found) {
      return delegateHandler(request)
    } else {
      val response = cacheStore.get(request)
      if (response == null) {
        cacheMissCounter.add(1)
        val newResponse = delegateHandler(request)
        if (newResponse.error == null) {
          cacheStore.put(request, newResponse)
        }
        return newResponse
      } else {
        cacheHitCounter.add(1)
        return response.copy(id = request.id)
      }
    }
  }
}

class LoggingHandler(
  private val delegateHandler: suspend (JSONRPCRequest) -> JSONRPCResponse,
  loggerName: String
) {

  private val logger = LoggerFactory.getLogger(loggerName)

  suspend fun handleRequest(request: JSONRPCRequest): JSONRPCResponse {
    logger.info(request.toString())
    val response = delegateHandler.invoke(request)
    logger.info(response.toString())
    return response
  }
}

class ConstantStringResult(val result: String) {
  suspend fun handle(request: JSONRPCRequest): JSONRPCResponse {
    return JSONRPCResponse(id = request.id, result = result)
  }
}

class ConstantBooleanResult(val result: Boolean) {
  suspend fun handle(request: JSONRPCRequest): JSONRPCResponse {
    return JSONRPCResponse(id = request.id, result = result)
  }
}

class FunctionCallResult(val result: () -> Any?) {

  suspend fun handle(request: JSONRPCRequest): JSONRPCResponse {
    return JSONRPCResponse(id = request.id, result = result())
  }
}
