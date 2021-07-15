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
package org.apache.tuweni.metrics

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.exporter.prometheus.PrometheusCollector
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.HTTPServer
import java.net.InetSocketAddress

class MetricsService(jobName: String, reportingIntervalMillis: Long = 5000, port: Int = 9090, networkInterface: String = "0.0.0.0", enablePrometheus: Boolean = true, enableGrpcPush: Boolean = true) {

  private val server: HTTPServer?
  val meterSdkProvider: SdkMeterProvider
  val openTelemetry: OpenTelemetrySdk
  private val spanProcessor: BatchSpanProcessor
  private val periodicReader: IntervalMetricReader?

  init {
    val exporter = OtlpGrpcMetricExporter.getDefault()
    val resource = Resource.getDefault()
      .merge(
        Resource.create(
          Attributes.builder().put(ResourceAttributes.SERVICE_NAME, jobName).build()
        )
      )
    meterSdkProvider = SdkMeterProvider.builder().setResource(resource).build()
    if (enableGrpcPush) {
      val builder = IntervalMetricReader.builder()
        .setExportIntervalMillis(reportingIntervalMillis)
        .setMetricProducers(setOf(meterSdkProvider))
        .setMetricExporter(exporter)
      periodicReader = builder.buildAndStart()
      periodicReader.start()
    } else {
      periodicReader = null
    }
    spanProcessor = BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().build()).build()
    openTelemetry = OpenTelemetrySdk.builder()
      .setTracerProvider(SdkTracerProvider.builder().addSpanProcessor(spanProcessor).build())
      .build()

    if (enablePrometheus) {
      val prometheusRegistry = CollectorRegistry(true)
      PrometheusCollector.builder()
        .setMetricProducer(meterSdkProvider)
        .build().register<PrometheusCollector>(prometheusRegistry)
      server = HTTPServer(InetSocketAddress(networkInterface, port), prometheusRegistry, true)
    } else {
      server = null
    }
  }

  fun close() {
    periodicReader?.shutdown()
    spanProcessor.shutdown()
    server?.stop()
  }
}
