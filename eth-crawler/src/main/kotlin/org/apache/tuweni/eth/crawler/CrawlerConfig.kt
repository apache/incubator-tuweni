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
package org.apache.tuweni.eth.crawler

import org.apache.tuweni.config.Configuration
import org.apache.tuweni.config.PropertyValidator
import org.apache.tuweni.config.Schema
import org.apache.tuweni.config.SchemaBuilder
import java.net.URI
import java.nio.file.Path

class CrawlerConfig(val filePath: Path) {

  companion object {
    val mainnetEthereumBootnodes = listOf(
      "enode://d860a01f9722d78051619d1e2351aba3f43f943f6f00718d1b9baa4101932a1f5011f16bb2b1bb35db20d6fe28fa0bf09636d26a87d31de9ec6203eeedb1f666@18.138.108.67:30303", // Singapore AWS
      "enode://22a8232c3abc76a16ae9d6c3b164f98775fe226f0917b0ca871128a74a8e9630b458460865bab457221f1d448dd9791d24c4e5d88786180ac185df813a68d4de@3.209.45.79:30303", // Virginia AWS
      "enode://ca6de62fce278f96aea6ec5a2daadb877e51651247cb96ee310a318def462913b653963c155a0ef6c7d50048bba6e6cea881130857413d9f50a621546b590758@34.255.23.113:30303", // Ireland AWS
      "enode://279944d8dcd428dffaa7436f25ca0ca43ae19e7bcf94a8fb7d1641651f92d121e972ac2e8f381414b80cc8e5555811c2ec6e1a99bb009b3f53c4c69923e11bd8@35.158.244.151:30303", // Frankfurt AWS
      "enode://8499da03c47d637b20eee24eec3c356c9a2e6148d6fe25ca195c7949ab8ec2c03e3556126b0d7ed644675e78c4318b08691b7b57de10e5f0d40d05b09238fa0a@52.187.207.27:30303", // Australia Azure
      "enode://103858bdb88756c71f15e9b5e09b56dc1be52f0a5021d46301dbbfb7e130029cc9d0d6f73f693bc29b665770fff7da4d34f3c6379fe12721b5d7a0bcb5ca1fc1@191.234.162.198:30303", // Brazil Azure
      "enode://715171f50508aba88aecd1250af392a45a330af91d7b90701c436b618c86aaa1589c9184561907bebbb56439b8f8787bc01f49a7c77276c58c1b09822d75e8e8@52.231.165.108:30303", // South Korea Azure
      "enode://5d6d7cd20d6da4bb83a1d28cadb5d409b64edf314c0335df658c1a54e32c7c4a7ab7823d57c39b6a757556e68ff1df17c748b698544a55cb488b52479a92b60f@104.42.217.25:30303", // West US Azure

      // Old Ethereum Foundation Bootnodes
      "enode://a979fb575495b8d6db44f750317d0f4622bf4c2aa3365d6af7c284339968eef29b69ad0dce72a4d8db5ebb4968de0e3bec910127f134779fbcb0cb6d3331163c@52.16.188.185:30303", // IE
      "enode://3f1d12044546b76342d59d4a05532c14b85aa669704bfe1f864fe079415aa2c02d743e03218e57a33fb94523adb54032871a6c51b2cc5514cb7c7e35b3ed0a99@13.93.211.84:30303", // US-WEST
      "enode://78de8a0916848093c73790ead81d1928bec737d565119932b98c6b100d944b7a95e94f847f689fc723399d2e31129d182f7ef3863f2b4c820abbf3ab2722344d@191.235.84.50:30303", // BR
      "enode://158f8aab45f6d19c6cbf4a089c2670541a8da11978a2f90dbf6a502a4a3bab80d288afdbeb7ec0ef6d92de563767f3b1ea9e8e334ca711e9f8e2df5a0385e8e6@13.75.154.138:30303", // AU
      "enode://1118980bf48b0a3640bdba04e0fe78b1add18e1cd99bf22d53daac1fd9972ad650df52176e7c7d89d1114cfef2bc23a2959aa54998a46afcf7d91809f0855082@52.74.57.123:30303", // SG

      // Ethereum Foundation Aleth Bootnodes
      "enode://979b7fa28feeb35a4741660a16076f1943202cb72b6af70d327f053e248bab9ba81760f39d0701ef1d8f89cc1fbd2cacba0710a12cd5314d5e0c9021aa3637f9@5.1.83.226:30303" // DE
    )

    val mainnetDiscoveryDNS = "enrtree://AKA3AM6LPBYEUDMVNU3BSVQJ5AD45Y7YPOHJLEF6W26QOE4VTUDPE@all.mainnet.ethdisco.net"

    fun schema(): Schema {
      val schema = SchemaBuilder.create()
        .addInteger("discoveryPort", 11000, "Discovery service port", PropertyValidator.isValidPort())
        .addString("discoveryNetworkInterface", "127.0.0.1", "Discovery network interface", null)
        .addInteger("rlpxPort", 11000, "RLPx service port", PropertyValidator.inRange(1, 65536))
        .addString("rlpxNetworkInterface", "127.0.0.1", "RLPx network interface", null)
        .addListOfString("bootnodes", mainnetEthereumBootnodes, "Bootnodes to discover other peers from", null)
        .addString("discoveryDNS", mainnetDiscoveryDNS, "DNS discovery crawler", null)
        .addLong("discoveryDNSPollingPeriod", 60 * 1000L, "DNS Discovery Polling Period in milliseconds", null)
        .addString(
          "jdbcUrl",
          System.getProperty("DATABASE_URL", System.getenv("DATABASE_URL")),
          "JDBC URL of the form jdbc:posgresql://localhost:5432",
          PropertyValidator.isPresent()
        )
        .addInteger("jdbcConnections", 25, "Number of JDBC connections for the connections pool", null)
        .addString("network", "mainnet", "Network to use instead of providing a genesis file.", null)
        .addString("genesisFile", "", "Genesis file to use in hello", null)
        .addInteger("restPort", 1337, "REST port", null)
        .addString("restNetworkInterface", "0.0.0.0", "REST network interface", null)
        .addString("ethstatsNetworkInterface", "0.0.0.0", "Ethstats network interface", null)
        .addInteger("ethstatsPort", 1338, "Ethstats port", null)
        .addString("ethstatsSecret", "changeme", "Ethstats shared secret", null)
        .addLong("peerCacheExpiration", 5 * 60 * 1000L, "Peer data cache expiration", null)
        .addLong("clientIdsInterval", 24 * 60 * 60 * 1000 * 2L, "Client IDs Interval - number of milliseconds to go back in time", null)
        .addLong("clientsStatsDelay", 30 * 1000L, "Delay between client stats calculations", null)
        .addLong("rlpxDisconnectionDelay", 10 * 1000L, "RLPx connections disconnection delay", null)
        .addInteger("maxRequestsPerSec", 30, "Number of requests per second over HTTP", null)
        .addInteger("numberOfThreads", 10, "Number of Threads for each thread pool", null)
        .addInteger("metricsPort", 9090, "Metric service port", PropertyValidator.isValidPort())
        .addString("metricsNetworkInterface", "localhost", "Metric service network interface", null)
        .addBoolean("metricsGrpcPushEnabled", false, "Enable pushing metrics to gRPC service", null)
        .addBoolean("metricsPrometheusEnabled", false, "Enable exposing metrics on the Prometheus endpoint", null)
        .addString("corsAllowedOrigins", "*", "CORS allowed domains filter for REST service", null)

      val upgradesSection = SchemaBuilder.create()
        .addString("name", null, "Upgrade name, eg London or Magneto", PropertyValidator.isNotBlank())
        .addListOfMap("versions", listOf(), "List of minimum version mappings for the upgrade", null)
        .toSchema()
      schema.addSection("upgrades", upgradesSection)
      return schema.toSchema()
    }
  }

  val config = Configuration.fromToml(filePath, schema())

  fun bootNodes() = config.getListOfString("bootnodes").map { URI.create(it) }

  fun discoveryPort() = config.getInteger("discoveryPort")

  fun discoveryNetworkInterface() = config.getString("discoveryNetworkInterface")

  fun discoveryDNS() = config.getString("discoveryDNS")

  fun discoveryDNSPollingPeriod() = config.getLong("discoveryDNSPollingPeriod")

  fun jdbcUrl() = config.getString("jdbcUrl")

  fun genesisFile() = config.getString("genesisFile")
  fun network() = config.getString("network")
  fun restPort() = config.getInteger("restPort")
  fun restNetworkInterface() = config.getString("restNetworkInterface")

  fun ethstatsPort() = config.getInteger("ethstatsPort")
  fun ethstatsNetworkInterface() = config.getString("ethstatsNetworkInterface")
  fun ethstatsSecret() = config.getString("ethstatsSecret")

  fun peerCacheExpiration() = config.getLong("peerCacheExpiration")
  fun clientIdsInterval() = config.getLong("clientIdsInterval")
  fun clientsStatsDelay() = config.getLong("clientsStatsDelay")

  fun jdbcConnections() = config.getInteger("jdbcConnections")
  fun rlpxDisconnectionDelay() = config.getLong("rlpxDisconnectionsDelay")
  fun maxRequestsPerSec() = config.getInteger("maxRequestsPerSec")
  fun numberOfThreads() = config.getInteger("numberOfThreads")
  fun metricsPort() = config.getInteger("metricsPort")
  fun metricsNetworkInterface() = config.getString("metricsNetworkInterface")
  fun metricsGrpcPushEnabled() = config.getBoolean("metricsGrpcPushEnabled")
  fun metricsPrometheusEnabled() = config.getBoolean("metricsPrometheusEnabled")
  fun corsAllowedOrigins() = config.getString("corsAllowedOrigins")

  fun upgradesVersions(): List<UpgradeConfig> {
    val upgrades = config.sections("upgrades")
    val result = mutableListOf<UpgradeConfig>()
    for (upgrade in upgrades) {
      val section = config.getConfigurationSection(upgrade)
      val versions = mutableMapOf<String, String>()
      for (map in section.getListOfMap("versions")) {
        for (entry in map.entries) {
          versions.put(entry.key.lowercase(), entry.value.toString())
        }
      }

      val upgradeConfig = UpgradeConfig(section.getString("name"), versions)
      result.add(upgradeConfig)
    }
    return result
  }
}

data class UpgradeConfig(val name: String, val versions: Map<String, String>)
