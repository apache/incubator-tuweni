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
import org.apache.tuweni.eth.JSONRPCRequest
import org.apache.tuweni.eth.JSONRPCResponse
import org.apache.tuweni.eth.methodNotEnabled
import org.apache.tuweni.eth.methodNotFound
import org.apache.tuweni.eth.tooManyRequests
import org.apache.tuweni.kv.KeyValueStore

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
  private val delegateHandler: suspend (JSONRPCRequest) -> JSONRPCResponse,
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
  private val delegateHandler: suspend (JSONRPCRequest) -> JSONRPCResponse,
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
  private val delegateHandler: suspend (JSONRPCRequest) -> JSONRPCResponse,
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
  private val delegateHandler: suspend (JSONRPCRequest) -> JSONRPCResponse,
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
      val serializedRequest = serializeRequest(request)
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

  private fun serializeRequest(request: JSONRPCRequest): String {
    return request.method + "|" + request.params.joinToString(",")
  }
}
