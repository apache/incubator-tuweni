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

import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.tracing.opentelemetry.OpenTelemetryOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.app.commons.ApplicationUtils
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.JSONRPCResponse
import org.apache.tuweni.eth.internalError
import org.apache.tuweni.jsonrpc.JSONRPCClient
import org.apache.tuweni.jsonrpc.JSONRPCServer
import org.apache.tuweni.jsonrpc.methods.CachingHandler
import org.apache.tuweni.jsonrpc.methods.LoggingHandler
import org.apache.tuweni.jsonrpc.methods.MeteredHandler
import org.apache.tuweni.jsonrpc.methods.MethodAllowListHandler
import org.apache.tuweni.jsonrpc.methods.ThrottlingHandler
import org.apache.tuweni.kv.InfinispanKeyValueStore
import org.apache.tuweni.metrics.MetricsService
import org.apache.tuweni.net.ip.IPRangeChecker
import org.apache.tuweni.net.tls.VertxTrustOptions
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.infinispan.Cache
import org.infinispan.commons.dataconversion.MediaType
import org.infinispan.commons.io.ByteBuffer
import org.infinispan.commons.io.ByteBufferImpl
import org.infinispan.commons.marshall.AbstractMarshaller
import org.infinispan.configuration.cache.ConfigurationBuilder
import org.infinispan.configuration.global.GlobalConfigurationBuilder
import org.infinispan.manager.DefaultCacheManager
import org.infinispan.marshall.persistence.PersistenceMarshaller
import org.infinispan.persistence.rocksdb.configuration.RocksDBStoreConfigurationBuilder
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.security.Security
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

val logger = LoggerFactory.getLogger(JSONRPCApp::class.java)

/**
 * Application running a JSON-RPC service that is able to connect to clients, get information, cache and distribute it.
 */
object JSONRPCApp {

  @JvmStatic
  fun main(args: Array<String>) {
    if (args.contains("--version")) {
      println("Apache Tuweni JSON-RPC proxy ${ApplicationUtils.version}")
      exitProcess(0)
    }
    if (args.contains("--help") || args.contains("-h")) {
      println("USAGE: jsonrpc <config file>")
      exitProcess(0)
    }
    ApplicationUtils.renderBanner("Loading JSON-RPC proxy")
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
      enablePrometheus = config.metricsPrometheusEnabled(),
      grpcEndpoint = config.metricsGrpcEndpoint(),
      grpcTimeout = config.metricsGrpcTimeout()
    )
    val vertx = Vertx.vertx(VertxOptions().setTracingOptions(OpenTelemetryOptions(metricsService.openTelemetry)))
    val app = JSONRPCApplication(vertx, config, metricsService)
    app.run()
  }
}

class JSONRPCApplication(
  val vertx: Vertx,
  val config: JSONRPCConfig,
  val metricsService: MetricsService,
  override val coroutineContext: CoroutineContext = Dispatchers.Unconfined
) : CoroutineScope {

  fun run() {
    logger.info("JSON-RPC proxy starting")
    val client = JSONRPCClient(vertx, config.endpointUrl(), basicAuthenticationEnabled = config.endpointBasicAuthEnabled(), basicAuthenticationUsername = config.endpointBasicAuthUsername(), basicAuthenticationPassword = config.endpointBasicAuthPassword())
    // TODO allow more options such as allowlist of certificates, enforce client authentication.
    val trustOptions = VertxTrustOptions.recordClientFingerprints(config.clientFingerprintsFile())

    val allowListHandler = MethodAllowListHandler(config.allowedMethods()) { req ->
      try {
        client.sendRequest(req).await()
      } catch (e: Exception) {
        logger.error("Error sending JSON-RPC request", e)
        internalError.copy(id = req.id)
      }
    }

    val meter = metricsService.meterSdkProvider.get("jsonrpc")
    val successCounter = meter.longCounterBuilder("success").build()
    val failureCounter = meter.longCounterBuilder("failure").build()

    val nextHandler = if (config.cacheEnabled()) {
      val builder = GlobalConfigurationBuilder().serialization().marshaller(PersistenceMarshaller())
      val manager = DefaultCacheManager(builder.build())
      val cache: Cache<String, JSONRPCResponse> = manager.createCache(
        "responses",
        ConfigurationBuilder().persistence().addStore(RocksDBStoreConfigurationBuilder::class.java)
          .location(Paths.get(config.cacheStoragePath(), "storage").toAbsolutePath().toString())
          .expiredLocation(Paths.get(config.cacheStoragePath(), "expired").toAbsolutePath().toString()).expiration().lifespan(config.cacheLifespan()).maxIdle(config.cacheMaxIdle()).build()
      )

      val cachingHandler =
        CachingHandler(
          config.cachedMethods(),
          InfinispanKeyValueStore(cache),
          meter.longCounterBuilder("cacheHits").build(),
          meter.longCounterBuilder("cacheMisses").build(),
          allowListHandler::handleRequest
        )
      cachingHandler::handleRequest
    } else {
      allowListHandler::handleRequest
    }

    val throttlingHandler = ThrottlingHandler(config.maxConcurrentRequests(), nextHandler)

    val loggingHandler = LoggingHandler(throttlingHandler::handleRequest, "jsonrpclog")

    val handler = MeteredHandler(successCounter, failureCounter, loggingHandler::handleRequest)
    val server = JSONRPCServer(
      vertx,
      config.port(), config.networkInterface(),
      config.ssl(),
      trustOptions,
      config.useBasicAuthentication(),
      config.basicAuthUsername(),
      config.basicAuthPassword(),
      config.basicAuthRealm(),
      IPRangeChecker.create(config.allowedRanges(), config.rejectedRanges()),
      metricsService.openTelemetry,
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
          server.stop()
          client.close()
        }
      }
    )
    runBlocking {
      server.start()
      logger.info("JSON-RPC server started")
    }
  }
}

class PersistenceMarshaller : AbstractMarshaller() {

  companion object {
    val mapper = ObjectMapper()
  }

  override fun objectFromByteBuffer(buf: ByteArray?, offset: Int, length: Int) = mapper.readValue(Bytes.wrap(buf!!, offset, length).toArrayUnsafe(), JSONRPCResponse::class.java)

  override fun objectToBuffer(o: Any?, estimatedSize: Int): ByteBuffer = ByteBufferImpl.create(mapper.writeValueAsBytes(o))

  override fun isMarshallable(o: Any?): Boolean = o is JSONRPCResponse

  override fun mediaType(): MediaType = MediaType.APPLICATION_OCTET_STREAM
}
