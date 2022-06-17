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

import org.apache.tuweni.config.Configuration
import org.apache.tuweni.config.PropertyValidator
import org.apache.tuweni.config.SchemaBuilder
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections

/**
 * Configuration of the JSON-RPC server as a TOML-based file.
 */
class JSONRPCConfig(val filePath: Path? = null) {

  companion object {

    fun schema() = SchemaBuilder.create()
      .addInteger("numberOfThreads", 10, "Number of threads for each thread pool", null)
      .addInteger("metricsPort", 9090, "Metric service port", PropertyValidator.isValidPort())
      .addString("metricsNetworkInterface", "localhost", "Metric service network interface", null)
      .addBoolean("metricsGrpcPushEnabled", false, "Enable pushing metrics to gRPC service", null)
      .addBoolean("metricsPrometheusEnabled", false, "Enable exposing metrics on the Prometheus endpoint", null)
      .addInteger("port", 18545, "JSON-RPC server port", PropertyValidator.isValidPort())
      .addString("networkInterface", "127.0.0.1", "JSON-RPC server network interface", null)
      .addString("clientFingerprintsFile", "fingerprints.txt", "File recording client connection fingerprints", null)
      .addBoolean("ssl", false, "Whether the JSON-RPC server should serve data over SSL", null)
      .addBoolean("basicAuth", false, "Whether the JSON-RPC server should authenticate incoming requests with HTTP Basic Authentication", null)
      .addString("basicAuthUsername", "", "HTTP Basic Auth username", null)
      .addString("basicAuthPassword", "", "HTTP Basic Auth password", null)
      .addString("basicAuthRealm", "Apache Tuweni JSON-RPC proxy", "HTTP Basic Auth realm", null)
      .addListOfString("allowedMethods", listOf("eth_", "net_version"), "Allowed JSON-RPC methods", null)
      .addListOfString("allowedRanges", Collections.singletonList("0.0.0.0/0"), "Allowed IP ranges", null)
      .addListOfString("rejectedRanges", Collections.emptyList(), "Rejected IP ranges", null)
      .addString("endpointUrl", "http://localhost:8545", "JSON-RPC endpoint", null)
      .addBoolean("endpointBasicAuthEnabled", false, "Enable basic authentication for the endpoint", null)
      .addString("endpointBasicAuthUsername", "", "Basic authentication username for the endpoint", null)
      .addString("endpointBasicAuthPassword", "", "Basic authentication password for the endpoint", null)
      .addListOfString("cachedMethods", listOf("eth_blockNumber", "eth_getBlockByNumber", "eth_getBlockByHash", "eth_getTransactionReceipt", "eth_getTransactionByHash", "eth_getLogs"), "Cached JSON-RPC methods", null)
      .addBoolean("cacheEnabled", true, "Enable caching", null)
      .addString("cacheStoragePath", "", "Location of cache storage", null)
      .addInteger("maxConcurrentRequests", 30, "Maximum concurrent requests", null)
      .addString("metricsGrpcEndpoint", "http://localhost:4317", "Metrics GRPC push endpoint", null)
      .addLong("metricsGrpcTimeout", 2000, "Metrics GRPC push timeout", null)
      .addLong("cacheLifespan", 5000, "Lifespan time for entries on cache in milliseconds", null)
      .addLong("cacheMaxIdle", 1000, "Max idle time for entries on cache in milliseconds", null)
      .toSchema()
  }

  val config = if (filePath != null) Configuration.fromToml(filePath, schema()) else Configuration.empty(schema())

  fun numberOfThreads() = config.getInteger("numberOfThreads")
  fun metricsPort() = config.getInteger("metricsPort")
  fun metricsNetworkInterface() = config.getString("metricsNetworkInterface")
  fun metricsGrpcPushEnabled() = config.getBoolean("metricsGrpcPushEnabled")
  fun metricsPrometheusEnabled() = config.getBoolean("metricsPrometheusEnabled")

  fun port() = config.getInteger("port")
  fun networkInterface() = config.getString("networkInterface")
  fun clientFingerprintsFile(): Path = Paths.get(config.getString("clientFingerprintsFile"))
  fun ssl() = config.getBoolean("ssl")
  fun useBasicAuthentication() = config.getBoolean("basicAuth")
  fun basicAuthUsername() = config.getString("basicAuthUsername")
  fun basicAuthPassword() = config.getString("basicAuthPassword")
  fun basicAuthRealm() = config.getString("basicAuthRealm")
  fun allowedMethods() = config.getListOfString("allowedMethods")
  fun allowedRanges() = config.getListOfString("allowedRanges")
  fun rejectedRanges() = config.getListOfString("rejectedRanges")

  fun endpointUrl() = config.getString("endpointUrl")

  fun endpointBasicAuthEnabled() = config.getBoolean("endpointBasicAuthEnabled")
  fun endpointBasicAuthUsername() = config.getString("endpointBasicAuthUsername")
  fun endpointBasicAuthPassword() = config.getString("endpointBasicAuthPassword")
  fun cachedMethods() = config.getListOfString("cachedMethods")
  fun cacheEnabled() = config.getBoolean("cacheEnabled")
  fun cacheStoragePath() = config.getString("cacheStoragePath")
  fun maxConcurrentRequests() = config.getInteger("maxConcurrentRequests")
  fun metricsGrpcEndpoint() = config.getString("metricsGrpcEndpoint")
  fun metricsGrpcTimeout() = config.getLong("metricsGrpcTimeout")
  fun cacheLifespan() = config.getLong("cacheLifespan")
  fun cacheMaxIdle() = config.getLong("cacheMaxIdle")
}
