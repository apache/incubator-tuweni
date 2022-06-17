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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JSONRPCConfigTest {

  @Test
  fun testDefaultConfig() {
    val configAsString = JSONRPCConfig().config.toToml()
    assertEquals(
      """## Allowed JSON-RPC methods
#allowedMethods = ["eth_", "net_version"]
## Allowed IP ranges
#allowedRanges = ["0.0.0.0/0"]
## Whether the JSON-RPC server should authenticate incoming requests with HTTP Basic Authentication
#basicAuth = false
## HTTP Basic Auth password
#basicAuthPassword = ""
## HTTP Basic Auth realm
#basicAuthRealm = "Apache Tuweni JSON-RPC proxy"
## HTTP Basic Auth username
#basicAuthUsername = ""
## Enable caching
#cacheEnabled = true
## Lifespan time for entries on cache in milliseconds
#cacheLifespan = 5000
## Max idle time for entries on cache in milliseconds
#cacheMaxIdle = 1000
## Location of cache storage
#cacheStoragePath = ""
## Cached JSON-RPC methods
#cachedMethods = ["eth_blockNumber", "eth_getBlockByNumber", "eth_getBlockByHash", "eth_getTransactionReceipt", "eth_getTransactionByHash", "eth_getLogs"]
## File recording client connection fingerprints
#clientFingerprintsFile = "fingerprints.txt"
## Enable basic authentication for the endpoint
#endpointBasicAuthEnabled = false
## Basic authentication password for the endpoint
#endpointBasicAuthPassword = ""
## Basic authentication username for the endpoint
#endpointBasicAuthUsername = ""
## JSON-RPC endpoint
#endpointUrl = "http://localhost:8545"
## Maximum concurrent requests
#maxConcurrentRequests = 30
## Metrics GRPC push endpoint
#metricsGrpcEndpoint = "http://localhost:4317"
## Enable pushing metrics to gRPC service
#metricsGrpcPushEnabled = false
## Metrics GRPC push timeout
#metricsGrpcTimeout = 2000
## Metric service network interface
#metricsNetworkInterface = "localhost"
## Metric service port
#metricsPort = 9090
## Enable exposing metrics on the Prometheus endpoint
#metricsPrometheusEnabled = false
## JSON-RPC server network interface
#networkInterface = "127.0.0.1"
## Number of threads for each thread pool
#numberOfThreads = 10
## JSON-RPC server port
#port = 18545
## Rejected IP ranges
#rejectedRanges = []
## Whether the JSON-RPC server should serve data over SSL
#ssl = false
""".split("\n").joinToString(System.lineSeparator()),
      configAsString
    )
  }
}
