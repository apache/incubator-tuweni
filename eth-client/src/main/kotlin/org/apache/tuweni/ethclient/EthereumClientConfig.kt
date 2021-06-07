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
package org.apache.tuweni.ethclient

import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.config.Configuration
import org.apache.tuweni.config.ConfigurationError
import org.apache.tuweni.config.DocumentPosition
import org.apache.tuweni.config.PropertyValidator
import org.apache.tuweni.config.Schema
import org.apache.tuweni.config.SchemaBuilder
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.genesis.GenesisFile
import java.io.FileNotFoundException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections

/**
 * Configuration of EthereumClient. Can be provided via file or over the wire.
 */
class EthereumClientConfig(private var config: Configuration = Configuration.empty(createSchema())) {

  fun dataStores(): List<DataStoreConfiguration> {
    val storageSections = config.sections("storage")
    if (storageSections == null || storageSections.isEmpty()) {
      return emptyList()
    }
    return storageSections.map { section ->
      val sectionConfig = config.getConfigurationSection("storage.$section")
      DataStoreConfigurationImpl(
        section,
        Paths.get(sectionConfig.getString("path")),
        sectionConfig.getString("genesis")
      )
    }
  }

  fun rlpxServices(): List<RLPxServiceConfiguration> {
    val rlpxSections = config.sections("rlpx")
    if (rlpxSections == null || rlpxSections.isEmpty()) {
      return emptyList()
    }
    return rlpxSections.map { section ->
      val sectionConfig = config.getConfigurationSection("rlpx.$section")
      RLPxServiceConfigurationImpl(
        section,
        sectionConfig.getString("clientName"),
        sectionConfig.getInteger("port"),
        sectionConfig.getString("networkInterface"),
        sectionConfig.getInteger("advertisedPort"),
        sectionConfig.getString("repository"),
        sectionConfig.getString("peerRepository"),
        sectionConfig.getString("key"),
      )
    }
  }

  fun genesisFiles(): List<GenesisFileConfiguration> {
    val genesisSections = config.sections("genesis")
    if (genesisSections == null || genesisSections.isEmpty()) {
      return emptyList()
    }
    return genesisSections.map { section ->
      val sectionConfig = config.getConfigurationSection("genesis.$section")
      GenesisFileConfigurationImpl(
        section,
        URI.create(sectionConfig.getString("path")),
      )
    }
  }

  // TODO read this from toml
  fun peerRepositories(): List<PeerRepositoryConfiguration> =
    listOf(PeerRepositoryConfigurationImpl("default"))

  fun metricsPort(): Int = config.getConfigurationSection("metrics").getInteger("port")

  fun metricsNetworkInterface(): String = config.getConfigurationSection("metrics").getString("networkInterface")

  fun metricsPrometheusEnabled(): Boolean = config.getConfigurationSection("metrics").getBoolean("enablePrometheus")

  fun metricsGrpcPushEnabled(): Boolean = config.getConfigurationSection("metrics").getBoolean("enableGrpcPush")

  fun toToml() = config.toToml()

  fun dnsClients(): List<DNSConfiguration> {
    val dnsSections = config.sections("dns")
    if (dnsSections == null || dnsSections.isEmpty()) {
      return emptyList()
    }
    return dnsSections.map { section ->
      val sectionConfig = config.getConfigurationSection("dns.$section")
      DNSConfigurationImpl(
        section,
        sectionConfig.getString("peerRepository"),
        sectionConfig.getString("enrLink"),
        sectionConfig.getLong("pollingPeriod")
      )
    }
  }

  fun staticPeers(): List<StaticPeersConfiguration> {
    val staticPeersSections = config.sections("static")
    if (staticPeersSections == null || staticPeersSections.isEmpty()) {
      return emptyList()
    }
    return staticPeersSections.map { section ->
      val sectionConfig = config.getConfigurationSection("static.$section")
      StaticPeersConfigurationImpl(
        sectionConfig.getListOfString("enodes"),
        sectionConfig.getString("peerRepository"),
      )
    }
  }

  companion object {
    fun createSchema(): Schema {
      val metricsSection = SchemaBuilder.create()
      metricsSection.addInteger("port", 9090, "Port to expose Prometheus metrics", PropertyValidator.isValidPort())
      metricsSection.addString("networkInterface", "0.0.0.0", "Network interface to expose Prometheus metrics", null)
      metricsSection.addBoolean("enablePrometheus", true, "Enable Prometheus metrics reporting", null)
      metricsSection.addBoolean("enableGrpcPush", true, "Enable GRPC OpenTelemetry metrics reporting", null)

      val storageSection = SchemaBuilder.create()
      storageSection.addString("path", null, "File system path where data is stored", null)
      storageSection.addString("genesis", null, "Reference to a genesis configuration", null)

      val dnsSection = SchemaBuilder.create()
      dnsSection.addString("enrLink", null, "DNS domain to query for records", null)
      dnsSection.addLong("pollingPeriod", 50000, "Polling period to refresh DNS records", null)
      dnsSection.addString("peerRepository", "default", "Peer repository to which records should go", null)

      val staticPeers = SchemaBuilder.create()
      staticPeers.addListOfString("enodes", Collections.emptyList(), "Static enodes to connect to in enode://publickey@host:port format", null)
      staticPeers.addString("peerRepository", "default", "Peer repository to which static nodes should go", null)

      val genesis = SchemaBuilder.create()
      genesis.addString("path", "classpath:/default.json", "Path to the genesis file", PropertyValidator.isURL())

      val rlpx = SchemaBuilder.create()
      rlpx.addString("networkInterface", "127.0.0.1", "Network interface to bind", null)
      rlpx.addString(
        "key", null, "Hex string representation of the private key used to represent the node",
        object : PropertyValidator<String> {
          override fun validate(
            key: String,
            position: DocumentPosition?,
            value: String?,
          ): MutableList<ConfigurationError> {
            try {
              SECP256K1.SecretKey.fromBytes(Bytes32.fromHexString(value ?: ""))
            } catch (e: IllegalArgumentException) {
              return mutableListOf(ConfigurationError("Invalid enode secret key"))
            }
            return mutableListOf()
          }
        }
      )
      rlpx.addInteger("port", 0, "Port to expose the RLPx service on", PropertyValidator.isValidPortOrZero())
      rlpx.addInteger("advertisedPort", 30303, "Port to advertise in communications as the RLPx service port", PropertyValidator.isValidPort())
      rlpx.addString("clientName", "Apache Tuweni", "Name of the Ethereum client", null)
      rlpx.addString("repository", "default", "Name of the blockchain repository", null)
      rlpx.addString("peerRepository", "default", "Peer repository to which records should go", null)

      val builder = SchemaBuilder.create()
      builder.addSection("metrics", metricsSection.toSchema())
      builder.addSection("storage", storageSection.toSchema())
      builder.addSection("dns", dnsSection.toSchema())
      builder.addSection("static", staticPeers.toSchema())
      builder.addSection("rlpx", rlpx.toSchema())
      builder.addSection("genesis", genesis.toSchema())
      return builder.toSchema()
    }

    fun fromFile(path: Path?): EthereumClientConfig {
      if (path == null) {
        return empty()
      }
      try {
        return fromString(path.toFile().readText())
      } catch (e: Exception) {
        when (e) {
          is NoSuchFileException, is FileNotFoundException -> {
            throw IllegalArgumentException("Missing config file: '$path'")
          }
          else -> throw e
        }
      }
    }

    fun fromString(config: String): EthereumClientConfig {
      return EthereumClientConfig(Configuration.fromToml(config, createSchema()))
    }

    fun empty(): EthereumClientConfig =
      fromString(EthereumClientConfig::class.java.getResource("/default.toml").readText())
  }
}

interface GenesisFileConfiguration {
  fun getName(): String
  fun genesisFile(): GenesisFile
}

interface DataStoreConfiguration {
  fun getName(): String
  fun getStoragePath(): Path
  fun getGenesisFile(): String
}

interface RLPxServiceConfiguration {
  fun port(): Int
  fun networkInterface(): String
  fun advertisedPort(): Int
  fun keyPair(): SECP256K1.KeyPair
  fun repository(): String
  fun getName(): String
  fun clientName(): String
  fun peerRepository(): String
}

interface DNSConfiguration {
  fun enrLink(): String
  fun pollingPeriod(): Long
  fun getName(): String
  fun peerRepository(): String
}

interface StaticPeersConfiguration {
  fun enodes(): List<String>
  fun peerRepository(): String
}

interface PeerRepositoryConfiguration {
  fun getName(): String
}

internal class PeerRepositoryConfigurationImpl(private val repoName: String) : PeerRepositoryConfiguration {
  override fun getName(): String = repoName
}

internal data class RLPxServiceConfigurationImpl(
  private val name: String,
  val clientName: String,
  val port: Int,
  val networkInterface: String,
  val advertisedPort: Int,
  val repository: String,
  val peerRepository: String,
  val key: String
) : RLPxServiceConfiguration {

  private val keyPair = SECP256K1.KeyPair.fromSecretKey(SECP256K1.SecretKey.fromBytes(Bytes32.fromHexString(key)))

  override fun port(): Int = port

  override fun networkInterface(): String = networkInterface

  override fun advertisedPort(): Int = advertisedPort

  override fun keyPair(): SECP256K1.KeyPair = keyPair

  override fun repository(): String = repository

  override fun getName(): String = name

  override fun clientName(): String = clientName

  override fun peerRepository(): String = peerRepository
}

internal data class GenesisFileConfigurationImpl(private val name: String, private val genesisFilePath: URI) : GenesisFileConfiguration {
  override fun getName(): String = name

  override fun genesisFile(): GenesisFile =
    GenesisFile.read(
      if (genesisFilePath.scheme == "classpath") {
        GenesisFileConfigurationImpl::class.java.getResource(genesisFilePath.path).readBytes()
      } else {
        Files.readAllBytes(Path.of(genesisFilePath))
      }
    )
}

internal data class DataStoreConfigurationImpl(
  private val name: String,
  private val storagePath: Path,
  private val genesisFile: String
) : DataStoreConfiguration {
  override fun getName(): String = name

  override fun getStoragePath(): Path = storagePath

  override fun getGenesisFile(): String = genesisFile
}

data class DNSConfigurationImpl(
  private val name: String,
  private val peerRepository: String,
  private val enrLink: String,
  private val pollingPeriod: Long
) :
  DNSConfiguration {
  override fun getName() = name

  override fun peerRepository() = peerRepository

  override fun enrLink() = enrLink

  override fun pollingPeriod(): Long = pollingPeriod
}

data class StaticPeersConfigurationImpl(private val enodes: List<String>, private val peerRepository: String) : StaticPeersConfiguration {

  override fun enodes(): List<String> = enodes

  override fun peerRepository() = peerRepository
}
