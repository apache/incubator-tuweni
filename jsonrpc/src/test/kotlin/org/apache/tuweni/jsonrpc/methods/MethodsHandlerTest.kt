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
import org.apache.tuweni.eth.JSONRPCRequest
import org.apache.tuweni.eth.JSONRPCResponse
import org.apache.tuweni.eth.methodNotFound
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Collections

@ExtendWith(BouncyCastleExtension::class)
class MethodsHandlerTest {

  @Test
  fun testMissingMethod() {
    val methodsRouter = MethodsRouter(emptyMap())
    assertEquals(methodNotFound, methodsRouter.handleRequest(JSONRPCRequest(1, "web3_sha3", arrayOf("0xdeadbeef"))))
  }

  @Test
  fun testRouteMethod() {
    val methodsRouter = MethodsRouter(mapOf(Pair("web3_sha3", ::sha3)))
    assertEquals(JSONRPCResponse(1, result = "0xd4fd4e189132273036449fc9e11198c739161b4c0116a9a2dccdfa1c492006f1"), methodsRouter.handleRequest(JSONRPCRequest(1, "web3_sha3", arrayOf("0xdeadbeef"))))
  }

  @Test
  fun testCountSuccess() {
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
      JSONRPCResponse(1)
    }
    meteredHandler.handleRequest(JSONRPCRequest(1, "foo", emptyArray()))
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
  fun testFailMeter() {
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
      JSONRPCResponse(1, error = "foo")
    }
    meteredHandler.handleRequest(JSONRPCRequest(1, "foo", emptyArray()))
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
