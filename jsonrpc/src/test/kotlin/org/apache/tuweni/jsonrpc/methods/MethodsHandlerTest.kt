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

import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader
import io.opentelemetry.sdk.metrics.export.MetricProducer
import io.opentelemetry.sdk.metrics.testing.InMemoryMetricExporter
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.eth.JSONRPCError
import org.apache.tuweni.eth.JSONRPCRequest
import org.apache.tuweni.eth.JSONRPCResponse
import org.apache.tuweni.eth.StringOrLong
import org.apache.tuweni.eth.methodNotFound
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.kv.MapKeyValueStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Collections

@ExtendWith(BouncyCastleExtension::class)
class MethodsHandlerTest {

  @Test
  fun testMissingMethod() = runBlocking {
    val methodsRouter = MethodsRouter(emptyMap())
    assertEquals(
      methodNotFound,
      methodsRouter.handleRequest(JSONRPCRequest(StringOrLong(1), "web3_sha3", arrayOf("0xdeadbeef")))
    )
  }

  @Test
  fun testRouteMethod() = runBlocking {
    val methodsRouter = MethodsRouter(mapOf(Pair("web3_sha3", ::sha3)))
    assertEquals(
      JSONRPCResponse(
        StringOrLong(1),
        result = "0xd4fd4e189132273036449fc9e11198c739161b4c0116a9a2dccdfa1c492006f1"
      ),
      methodsRouter.handleRequest(JSONRPCRequest(StringOrLong(1), "web3_sha3", arrayOf("0xdeadbeef")))
    )
  }

  @Test
  fun testCountSuccess() = runBlocking {
    val exporter = InMemoryMetricExporter.create()
    val meterSdk = SdkMeterProvider.builder().build()
    val meter = meterSdk.get("handler")
    val intervalMetricReader =
      IntervalMetricReader.builder()
        .setMetricExporter(exporter)
        .setMetricProducers(Collections.singletonList(meterSdk) as Collection<MetricProducer>)
        .setExportIntervalMillis(1000)
        .build()
    intervalMetricReader.start()
    val successCounter = meter.longCounterBuilder("success").build()
    val failCounter = meter.longCounterBuilder("fail").build()
    val meteredHandler = MeteredHandler(successCounter, failCounter) {
      JSONRPCResponse(StringOrLong(1))
    }
    meteredHandler.handleRequest(JSONRPCRequest(StringOrLong(1), "foo", emptyArray()))
    Thread.sleep(1200)
    var metricValue = 0L
    for (metric in exporter.finishedMetricItems) {
      if (metric.name == "success") {
        metricValue = metric.longSumData.points.first().value
      }
    }
    assertEquals(1L, metricValue)
  }

  @Test
  fun testFailMeter() = runBlocking {
    val exporter = InMemoryMetricExporter.create()
    val meterSdk = SdkMeterProvider.builder().build()
    val meter = meterSdk.get("handler")
    val intervalMetricReader =
      IntervalMetricReader.builder()
        .setMetricExporter(exporter)
        .setMetricProducers(Collections.singletonList(meterSdk) as Collection<MetricProducer>)
        .setExportIntervalMillis(1000)
        .build()
    intervalMetricReader.start()
    val successCounter = meter.longCounterBuilder("success").build()
    val failCounter = meter.longCounterBuilder("fail").build()
    val meteredHandler = MeteredHandler(successCounter, failCounter) {
      JSONRPCResponse(StringOrLong(1), error = JSONRPCError(123, "foo"))
    }
    meteredHandler.handleRequest(JSONRPCRequest(StringOrLong(1), "foo", emptyArray()))
    Thread.sleep(1200)
    var metricValue = 0L
    for (metric in exporter.finishedMetricItems) {
      if (metric.name == "fail") {
        metricValue = metric.longSumData.points.first().value
      }
    }
    assertEquals(1L, metricValue)
  }
}

class MethodAllowListHandlerTest {

  @Test
  fun testAllowedMethod() = runBlocking {
    val filter = MethodAllowListHandler(listOf("eth_")) { JSONRPCResponse(StringOrLong(1), "foo") }
    val resp = filter.handleRequest(JSONRPCRequest(StringOrLong(1), "eth_client", emptyArray()))
    assertNull(resp.error)
  }

  @Test
  fun testForbiddenMethod() = runBlocking {
    val filter = MethodAllowListHandler(listOf("eth_")) { JSONRPCResponse(StringOrLong(1), "foo") }
    val resp = filter.handleRequest(JSONRPCRequest(StringOrLong(1), "foo_client", emptyArray()))
    assertNotNull(resp.error)
    val respContents = resp.error as JSONRPCError
    assertEquals(-32604, respContents.code)
    assertEquals("Method not enabled", respContents.message)
  }
}

class ThrottlingHandlerTest {

  @Test
  fun testThrottling(): Unit = runBlocking {
    val handler = ThrottlingHandler(4) {
      runBlocking {
        delay(500)
        JSONRPCResponse(id = StringOrLong(1))
      }
    }
    async {
      val response = handler.handleRequest(JSONRPCRequest(StringOrLong(2), "foo", arrayOf()))
      assertEquals(StringOrLong(1), response.id)
    }
    async {
      val response = handler.handleRequest(JSONRPCRequest(StringOrLong(3), "foo", arrayOf()))
      assertEquals(StringOrLong(1), response.id)
    }
    async {
      val response = handler.handleRequest(JSONRPCRequest(StringOrLong(4), "foo", arrayOf()))
      assertEquals(StringOrLong(1), response.id)
    }
    async {
      val response = handler.handleRequest(JSONRPCRequest(StringOrLong(5), "foo", arrayOf()))
      assertEquals(StringOrLong(1), response.id)
    }
    async {
      delay(200)
      val response = handler.handleRequest(JSONRPCRequest(StringOrLong(6), "foo", arrayOf()))
      assertEquals(-32000, response.error?.code)
    }
    async {
      delay(1000)
      val response = handler.handleRequest(JSONRPCRequest(StringOrLong(7), "foo", arrayOf()))
      assertEquals(StringOrLong(1), response.id)
    }
  }
}

class CachingHandlerTest {

  @Test
  fun testCache() = runBlocking {
    val map = HashMap<String, JSONRPCResponse>()
    val kv = MapKeyValueStore.open(map)
    val meterSdk = SdkMeterProvider.builder().build()
    val meter = meterSdk.get("handler")
    val handler = CachingHandler(
      listOf("foo"),
      kv,
      meter.longCounterBuilder("foo").build(),
      meter.longCounterBuilder("bar").build()
    ) {
      if (it.params.isNotEmpty()) {
        JSONRPCResponse(id = StringOrLong(1), error = JSONRPCError(1234, ""))
      } else {
        JSONRPCResponse(id = StringOrLong(1))
      }
    }
    assertEquals(0, map.size)
    handler.handleRequest(JSONRPCRequest(id = StringOrLong(1), method = "foo", params = arrayOf()))
    assertEquals(1, map.size)
    handler.handleRequest(JSONRPCRequest(id = StringOrLong(1), method = "bar", params = arrayOf()))
    assertEquals(1, map.size)
    handler.handleRequest(JSONRPCRequest(id = StringOrLong(1), method = "foo", params = arrayOf()))
    assertEquals(1, map.size)
    handler.handleRequest(JSONRPCRequest(id = StringOrLong(1), method = "foo", params = arrayOf("bleh")))
    assertEquals(1, map.size)
  }
}

class CachingPollingHandlerTest {

  @Test
  fun testCache() = runBlocking {
    val map = HashMap<JSONRPCRequest, JSONRPCResponse>()
    val kv = MapKeyValueStore.open(map)
    val meterSdk = SdkMeterProvider.builder().build()
    val meter = meterSdk.get("handler")
    val handler = CachingPollingHandler(
      listOf(JSONRPCRequest(StringOrLong(1), "foo", arrayOf())),
      1000,
      kv,
      meter.longCounterBuilder("foo").build(),
      meter.longCounterBuilder("bar").build()
    ) {
      if (it.params.isNotEmpty()) {
        JSONRPCResponse(id = StringOrLong(1), error = JSONRPCError(1234, ""))
      } else {
        JSONRPCResponse(id = StringOrLong(1))
      }
    }
    delay(500)
    assertEquals(1, map.size)
    handler.handleRequest(JSONRPCRequest(id = StringOrLong(1), method = "foo", params = arrayOf()))
    assertEquals(1, map.size)
    handler.handleRequest(JSONRPCRequest(id = StringOrLong(1), method = "bar", params = arrayOf()))
    assertEquals(1, map.size)
    handler.handleRequest(JSONRPCRequest(id = StringOrLong(1), method = "foo", params = arrayOf()))
    assertEquals(1, map.size)
    val errorResp =
      handler.handleRequest(JSONRPCRequest(id = StringOrLong(1), method = "foo", params = arrayOf("bleh")))
    assertEquals(1, map.size)
    assertNotNull(errorResp.error)
  }
}
