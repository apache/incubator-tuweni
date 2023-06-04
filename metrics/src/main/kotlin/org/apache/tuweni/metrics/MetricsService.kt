// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.metrics

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class MetricsService(
  jobName: String,
  reportingIntervalMillis: Long = 5000,
  port: Int = 9090,
  networkInterface: String = "0.0.0.0",
  enablePrometheus: Boolean = true,
  enableGrpcPush: Boolean = true,
  grpcEndpoint: String = "http://localhost:4317",
  grpcTimeout: Long = 2000,
) {

  companion object {
    private val logger = LoggerFactory.getLogger(MetricsService::class.java)
  }

  private val server: PrometheusHttpServer?
  val meterSdkProvider: SdkMeterProvider
  val openTelemetry: OpenTelemetrySdk
  private val spanProcessor: BatchSpanProcessor
  private val periodicReader: PeriodicMetricReader?

  init {
    val exporter = OtlpGrpcMetricExporter.builder().setEndpoint(grpcEndpoint).setTimeout(
      grpcTimeout,
      TimeUnit.MILLISECONDS,
    ).build()
    logger.info("Starting metrics service")
    val resource = Resource.getDefault()
      .merge(
        Resource.create(
          Attributes.builder().put(ResourceAttributes.SERVICE_NAME, jobName).build(),
        ),
      )
    val sdkMeterProviderBuilder = SdkMeterProvider.builder().setResource(resource)
    if (enableGrpcPush) {
      logger.info("Starting GRPC push metrics service")
      val builder = PeriodicMetricReader.builder(exporter)
        .setInterval(reportingIntervalMillis, TimeUnit.MILLISECONDS)
      periodicReader = builder.build()
      sdkMeterProviderBuilder.registerMetricReader(periodicReader)
    } else {
      periodicReader = null
    }

    spanProcessor = BatchSpanProcessor.builder(
      OtlpGrpcSpanExporter.builder().setEndpoint(grpcEndpoint)
        .setTimeout(grpcTimeout, TimeUnit.MILLISECONDS).build(),
    ).build()
    openTelemetry = OpenTelemetrySdk.builder()
      .setTracerProvider(SdkTracerProvider.builder().setResource(resource).addSpanProcessor(spanProcessor).build())
      .build()

    if (enablePrometheus) {
      logger.info("Starting Prometheus metrics service")
      server = PrometheusHttpServer.builder().setHost(networkInterface).setPort(port).build()
      sdkMeterProviderBuilder.registerMetricReader(server).build()
    } else {
      server = null
    }

    meterSdkProvider = sdkMeterProviderBuilder.build()
  }

  fun close() {
    periodicReader?.shutdown()
    spanProcessor.shutdown()
    server?.shutdown()
  }
}
