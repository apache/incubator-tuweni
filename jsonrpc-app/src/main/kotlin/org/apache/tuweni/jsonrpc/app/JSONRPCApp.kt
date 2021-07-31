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
package org.apache.tuweni.jsonrpc.app

import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.tracing.opentelemetry.OpenTelemetryOptions
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.eth.internalError
import org.apache.tuweni.jsonrpc.JSONRPCClient
import org.apache.tuweni.jsonrpc.JSONRPCServer
import org.apache.tuweni.jsonrpc.methods.MeteredHandler
import org.apache.tuweni.jsonrpc.methods.MethodAllowListHandler
import org.apache.tuweni.metrics.MetricsService
import org.apache.tuweni.net.ip.IPRangeChecker
import org.apache.tuweni.net.tls.VertxTrustOptions
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.security.Security
import java.util.concurrent.Executors

val logger = LoggerFactory.getLogger(JSONRPCApp::class.java)

/**
 * Application running a JSON-RPC service that is able to connect to clients, get information, cache and distribute it.
 */
object JSONRPCApp {

  @JvmStatic
  fun main(args: Array<String>) {
    val configFile = Paths.get(if (args.isNotEmpty()) args[0] else "config.toml")
    Security.addProvider(BouncyCastleProvider())

    val config = JSONRPCConfig(configFile)
    if (config.config.hasErrors()) {
      for (error in config.config.errors()) {
        println(error.message)
      }
      System.exit(1)
    }
    val metricsService = MetricsService(
      "json-rpc",
      port = config.metricsPort(),
      networkInterface = config.metricsNetworkInterface(),
      enableGrpcPush = config.metricsGrpcPushEnabled(),
      enablePrometheus = config.metricsPrometheusEnabled()
    )
    val vertx = Vertx.vertx(VertxOptions().setTracingOptions(OpenTelemetryOptions(metricsService.openTelemetry)))
    val app = JSONRPCApplication(vertx, config, metricsService)
    app.run()
  }
}

class JSONRPCApplication(
  val vertx: Vertx,
  val config: JSONRPCConfig,
  val metricsService: MetricsService
) {

  fun run() {
    val client = JSONRPCClient(vertx, config.endpointPort(), config.endpointHost(), basicAuthenticationEnabled = config.endpointBasicAuthEnabled(), basicAuthenticationUsername = config.endpointBasicAuthUsername(), basicAuthenticationPassword = config.endpointBasicAuthPassword())
    // TODO allow more options such as allowlist of certificates, enforce client authentication.
    val trustOptions = VertxTrustOptions.recordClientFingerprints(config.clientFingerprintsFile())

    val allowListHandler = MethodAllowListHandler(config.allowedMethods()) { req ->
      runBlocking {
        try {
          client.sendRequest(req).await()
        } catch (e: Exception) {
          logger.error("Error sending JSON-RPC request", e)
          internalError.copy(id = req.id)
        }
      }
    }

    val meter = metricsService.meterSdkProvider.get("jsonrpc")
    val successCounter = meter.longCounterBuilder("success").build()
    val failureCounter = meter.longCounterBuilder("failure").build()

    val handler = MeteredHandler(successCounter, failureCounter, allowListHandler::handleRequest)
    val server = JSONRPCServer(
      vertx, config.port(), config.networkInterface(),
      config.ssl(),
      trustOptions,
      config.useBasicAuthentication(),
      config.basicAuthUsername(),
      config.basicAuthPassword(),
      config.basicAuthRealm(),
      IPRangeChecker.create(config.allowedRanges(), config.rejectedRanges()),
      Executors.newFixedThreadPool(
        config.numberOfThreads()
      ) {
        val thread = Thread("jsonrpc")
        thread.isDaemon = true
        thread
      }.asCoroutineDispatcher(),
      handler::handleRequest
    )
    Runtime.getRuntime().addShutdownHook(
      Thread {
        runBlocking {
          server.stop().await()
          client.close()
        }
      }
    )
    runBlocking {
      server.start().await()
      logger.info("JSON-RPC server started")
    }
  }
}
