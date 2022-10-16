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
import org.apache.tuweni.devp2p.parseEnodeUri
import org.apache.tuweni.eth.genesis.GenesisFile
import org.apache.tuweni.units.bigints.UInt256
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections
import java.util.stream.Stream

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
        sectionConfig.getString("key")
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
        URI.create(sectionConfig.getString("path"))
      )
    }
  }

  fun peerRepositories(): List<PeerRepositoryConfiguration> {
    val peerRepositories = config.sections("peerRepository")
    if (peerRepositories == null || peerRepositories.isEmpty()) {
      return emptyList()
    }
    return peerRepositories.map { section ->
      val sectionConfig = config.getConfigurationSection("peerRepository.$section")
      PeerRepositoryConfigurationImpl(
        section,
        sectionConfig.getString("type")
      )
    }
  }

  fun metricsEnabled(): Boolean = config.getConfigurationSection("metrics").getBoolean("enabled")

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
        sectionConfig.getLong("pollingPeriod"),
        if (sectionConfig.contains("dnsServer")) sectionConfig.getString("dnsServer") else null
      )
    }
  }

  fun discoveryServices(): List<DiscoveryConfiguration> {
    val discoverySections = config.sections("discovery")
    if (discoverySections == null || discoverySections.isEmpty()) {
      return emptyList()
    }
    return discoverySections.map { section ->
      val sectionConfig = config.getConfigurationSection("discovery.$section")

      val secretKey = sectionConfig.getString("identity")
      val keypair = if (secretKey == "") {
        SECP256K1.KeyPair.random()
      } else {
        SECP256K1.KeyPair.fromSecretKey(SECP256K1.SecretKey.fromBytes(Bytes32.fromHexString(secretKey)))
      }
      logger.info("Using () for discovery ()", keypair.publicKey().toHexString(), section)

      DiscoveryConfigurationImpl(
        section,
        sectionConfig.getString("peerRepository"),
        sectionConfig.getInteger("port"),
        sectionConfig.getString("networkInterface"),
        keypair
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
        sectionConfig.getString("peerRepository")
      )
    }
  }

  fun proxies(): List<ProxyConfiguration> {
    val proxySections = config.sections("proxy")
    if (proxySections == null || proxySections.isEmpty()) {
      return emptyList()
    }
    return proxySections.map { section ->
      val sectionConfig = config.getConfigurationSection("proxy.$section")
      ProxyConfigurationImpl(
        sectionConfig.getString("name"),
        sectionConfig.getString("upstream"),
        sectionConfig.getString("downstream")
      )
    }
  }

  fun synchronizers(): List<SynchronizerConfiguration> {
    val synchronizers = config.sections("synchronizer")
    if (synchronizers == null || synchronizers.isEmpty()) {
      return emptyList()
    }
    return synchronizers.map { section ->
      val sectionConfig = config.getConfigurationSection("synchronizer.$section")
      SynchronizerConfigurationImpl(
        section,
        SynchronizerType.valueOf(sectionConfig.getString("type")),
        sectionConfig.getString("repository"),
        sectionConfig.getString("peerRepository"),
        sectionConfig.getString("rlpxService"),
        UInt256.valueOf(sectionConfig.getLong("from")),
        UInt256.valueOf(sectionConfig.getLong("to")),
        sectionConfig.getString("fromRepository")
      )
    }
  }

  fun validators(): List<ValidatorConfiguration> {
    val validators = config.sections("validator")
    if (validators == null || validators.isEmpty()) {
      return emptyList()
    }
    return validators.map { section ->
      val sectionConfig = config.getConfigurationSection("validator.$section")
      ValidatorConfigurationImpl(
        section,
        ValidatorType.valueOf(sectionConfig.getString("type")),
        UInt256.valueOf(sectionConfig.getLong("from")),
        UInt256.valueOf(sectionConfig.getLong("to")),
        sectionConfig.getInteger("chainId")
      )
    }
  }

  fun validate(): Stream<ConfigurationError> {
    val schema = createSchema()
    var errors = schema.validate(this.config)

    errors = Stream.concat(errors, validateSubsection("metrics", schema))
    errors = Stream.concat(errors, validateSubsection("storage", schema))
    errors = Stream.concat(errors, validateSubsection("dns", schema))
    errors = Stream.concat(errors, validateSubsection("static", schema))
    errors = Stream.concat(errors, validateSubsection("discovery", schema))
    errors = Stream.concat(errors, validateSubsection("rlpx", schema))
    errors = Stream.concat(errors, validateSubsection("proxy", schema))
    errors = Stream.concat(errors, validateSubsection("peerRepository", schema))

    return errors
  }

  private fun validateSubsection(name: String, schema: Schema): Stream<ConfigurationError> {
    var errors = listOf<ConfigurationError>().stream()
    for (subSection in this.config.sections(name)) {
      errors = Stream.concat(
        errors,
        schema.getSubSection(name).validate(this.config.getConfigurationSection("$name.$subSection"))
      )
    }
    return errors
  }

  companion object {

    val logger = LoggerFactory.getLogger(EthereumClientConfig::class.java)

    fun createSchema(): Schema {
      val metricsSection = SchemaBuilder.create()
      metricsSection.addBoolean("enabled", false, "Enable telemetry", null)
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
      dnsSection.addString("dnsServer", null, "DNS Server address to use, will use system default if null", null)

      val staticPeers = SchemaBuilder.create()
      staticPeers.addListOfString(
        "enodes",
        Collections.emptyList(),
        "Static enodes to connect to in enode://publickey@host:port format"
      ) { _, position, value ->
        val errors = mutableListOf<ConfigurationError>()
        for (enode in value!!) {
          try {
            parseEnodeUri(URI.create(enode))
          } catch (e: IllegalArgumentException) {
            errors.add(ConfigurationError(position, e.message ?: "error validating enode"))
          }
        }
        errors
      }
      staticPeers.addString("peerRepository", "default", "Peer repository to which static nodes should go", null)

      val discoverySection = SchemaBuilder.create()
      discoverySection.addString("identity", "", "Node identity", null)
      discoverySection.addString("networkInterface", "127.0.0.1", "Network interface to bind", null)
      discoverySection.addInteger(
        "port",
        0,
        "Port to expose the discovery service on",
        PropertyValidator.isValidPortOrZero()
      )
      discoverySection.addString("peerRepository", "default", "Peer repository to which records should go", null)

      val genesis = SchemaBuilder.create()
      genesis.addString("path", "classpath:/default.json", "Path to the genesis file", PropertyValidator.isURL())

      val rlpx = SchemaBuilder.create()
      rlpx.addString("networkInterface", "127.0.0.1", "Network interface to bind", null)
      rlpx.addString(
        "key",
        null,
        "Hex string representation of the private key used to represent the node",
        object : PropertyValidator<String> {
          override fun validate(
            key: String,
            position: DocumentPosition?,
            value: String?
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
      rlpx.addInteger(
        "advertisedPort",
        30303,
        "Port to advertise in communications as the RLPx service port",
        PropertyValidator.isValidPort()
      )
      rlpx.addString("clientName", "Apache Tuweni", "Name of the Ethereum client", null)
      rlpx.addString("repository", "default", "Name of the blockchain repository", null)
      rlpx.addString("peerRepository", "default", "Peer repository to which records should go", null)
      val proxiesSection = SchemaBuilder.create()
      proxiesSection.addString("name", null, "Name of the site", null)
      proxiesSection.addString("upstream", "", "Server and port to send data to, such as localhost:1234", null)
      proxiesSection.addString("downstream", "", "Server and port to expose data on, such as localhost:1234", null)

      val peerRepositoriesSection = SchemaBuilder.create()
      peerRepositoriesSection.addString("type", "memory", "Peer repository type", PropertyValidator.anyOf("memory"))

      val synchronizersSection = SchemaBuilder.create()
      synchronizersSection.addString(
        "type",
        "status",
        "Synchronizer type",
        PropertyValidator.anyOf("status", "parent", "best", "canonical")
      )
      synchronizersSection.addLong("from", 0L, "Start block to sync from", PropertyValidator.isGreaterOrEqual(0L))
      synchronizersSection.addLong("to", 0L, "End block to sync to", PropertyValidator.isGreaterOrEqual(0L))
      synchronizersSection.addString("repository", "default", "Blockchain repository to use", null)
      synchronizersSection.addString("rlpxService", "default", "RLPx service to use for requests with this synchronizer", null)
      synchronizersSection.addString("peerRepository", "default", "Peer repository to use for requests with this synchronizer", null)
      synchronizersSection.addString("fromRepository", null, "(only for canonical) Repository to sync from", null)

      val validatorsSection = SchemaBuilder.create()
      validatorsSection.addString(
        "type",
        "evm",
        "Validator type",
        PropertyValidator.anyOf("header", "difficulty", "evm")
      )
      validatorsSection.addInteger("chainId", 1, "chain id to validate with", null)
      val builder = SchemaBuilder.create()
      builder.addSection("metrics", metricsSection.toSchema())
      builder.addSection("storage", storageSection.toSchema())
      builder.addSection("dns", dnsSection.toSchema())
      builder.addSection("static", staticPeers.toSchema())
      builder.addSection("discovery", discoverySection.toSchema())
      builder.addSection("rlpx", rlpx.toSchema())
      builder.addSection("genesis", genesis.toSchema())
      builder.addSection("proxy", proxiesSection.toSchema())
      builder.addSection("peerRepository", peerRepositoriesSection.toSchema())
      builder.addSection("synchronizer", synchronizersSection.toSchema())
      builder.addSection("validators", validatorsSection.toSchema())

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
  fun dnsServer(): String?
  fun getName(): String
  fun peerRepository(): String
}

interface DiscoveryConfiguration {
  fun getName(): String
  fun getIdentity(): SECP256K1.KeyPair
  fun getNetworkInterface(): String
  fun getPort(): Int
  fun getPeerRepository(): String
}

interface StaticPeersConfiguration {
  fun enodes(): List<String>
  fun peerRepository(): String
}

interface ProxyConfiguration {
  fun name(): String
  fun upstream(): String
  fun downstream(): String
}

interface PeerRepositoryConfiguration {
  fun getName(): String
  fun getType(): String
}

enum class SynchronizerType {
  BEST, STATUS, PARENT, CANONICAL
}

interface SynchronizerConfiguration {
  fun getName(): String
  fun getRepository(): String
  fun getPeerRepository(): String
  fun getRlpxService(): String
  fun getType(): SynchronizerType
  fun getFrom(): UInt256?
  fun getTo(): UInt256?
  fun getFromRepository(): String?
}

enum class ValidatorType {
  CHAINID, TRANSACTIONHASH
}

interface ValidatorConfiguration {
  fun getName(): String
  fun getType(): ValidatorType
  fun getFrom(): UInt256?
  fun getTo(): UInt256?
  fun getChainId(): Int?
}

internal class PeerRepositoryConfigurationImpl(private val repoName: String, private val type: String) :
  PeerRepositoryConfiguration {
  override fun getName(): String = repoName
  override fun getType(): String = type
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

internal data class GenesisFileConfigurationImpl(private val name: String, private val genesisFilePath: URI) :
  GenesisFileConfiguration {
  override fun getName(): String = name

  override fun genesisFile(): GenesisFile =
    GenesisFile.read(
      if (genesisFilePath.scheme == "classpath") {
        val resource = GenesisFileConfigurationImpl::class.java.getResource(genesisFilePath.path)
          ?: throw IllegalArgumentException("No such classpath resource: ${genesisFilePath.path}")
        resource.readBytes()
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
  private val pollingPeriod: Long,
  private val dnsServer: String?
) :
  DNSConfiguration {
  override fun getName() = name

  override fun peerRepository() = peerRepository

  override fun enrLink() = enrLink

  override fun pollingPeriod(): Long = pollingPeriod

  override fun dnsServer(): String? = dnsServer
}

data class DiscoveryConfigurationImpl(
  private val name: String,
  private val peerRepository: String,
  private val port: Int,
  private val networkInterface: String,
  private val identity: SECP256K1.KeyPair
) :
  DiscoveryConfiguration {
  override fun getName() = name
  override fun getIdentity() = identity
  override fun getNetworkInterface() = networkInterface
  override fun getPeerRepository() = peerRepository
  override fun getPort() = port
}

data class StaticPeersConfigurationImpl(private val enodes: List<String>, private val peerRepository: String) :
  StaticPeersConfiguration {

  override fun enodes(): List<String> = enodes

  override fun peerRepository() = peerRepository
}

data class ProxyConfigurationImpl(
  private val name: String,
  private val upstream: String,
  private val downstream: String
) : ProxyConfiguration {
  override fun name() = name

  override fun upstream() = upstream
  override fun downstream() = downstream
}

data class SynchronizerConfigurationImpl(
  private val name: String,
  private val type: SynchronizerType,
  private val repository: String,
  private val peerRepository: String,
  private val rlpxService: String,
  private val from: UInt256?,
  private val to: UInt256?,
  private val fromRepository: String?
) : SynchronizerConfiguration {
  override fun getName() = name
  override fun getRepository() = repository
  override fun getPeerRepository() = peerRepository
  override fun getRlpxService() = rlpxService
  override fun getType() = type
  override fun getFrom(): UInt256? = from
  override fun getTo(): UInt256? = to
  override fun getFromRepository() = fromRepository
}

data class ValidatorConfigurationImpl(
  private val name: String,
  private val type: ValidatorType,
  private val from: UInt256?,
  private val to: UInt256?,
  private val chainId: Int?
) : ValidatorConfiguration {
  override fun getName() = name
  override fun getType() = type
  override fun getFrom(): UInt256? = from
  override fun getTo(): UInt256? = to
  override fun getChainId() = chainId
}
