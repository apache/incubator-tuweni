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

import org.apache.tuweni.config.Configuration
import org.apache.tuweni.config.Schema
import org.apache.tuweni.config.SchemaBuilder
import org.apache.tuweni.eth.genesis.GenesisFile
import java.io.FileNotFoundException
import java.nio.file.Path
import java.nio.file.Paths

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

  fun rlpxServices(): List<RLPxServiceConfiguration> =
    listOf(RLPxServiceConfigurationImpl())

  fun genesisFiles(): List<GenesisFileConfiguration> = listOf(GenesisFileConfigurationImpl())

  fun peerRepositories(): List<PeerRepositoryConfiguration> =
    listOf(PeerRepositoryConfigurationImpl("default"))

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

  companion object {
    fun createSchema(): Schema {
      val storageSection = SchemaBuilder.create()
      storageSection.addString("path", null, "File system path where data is stored", null)
      storageSection.addString("genesis", null, "Reference to a genesis configuration", null)

      val dnsSection = SchemaBuilder.create()
      dnsSection.addString("enrLink", null, "DNS domain to query for records", null)
      dnsSection.addLong("pollingPeriod", 50000, "Polling period to refresh DNS records", null)
      dnsSection.addString("peerRepository", "default", "Peer repository to which records should go", null)

      val proxiesSection = SchemaBuilder.create()
      proxiesSection.addString("name", null, "Name of the site", null)
      proxiesSection.addString("upstream", null, "Server and port to send data to, such as localhost:1234", null)
      proxiesSection.addString("downstream", null, "Server and port to expose data on, such as localhost:1234", null)

      val builder = SchemaBuilder.create()
      builder.addSection("storage", storageSection.toSchema())
      builder.addSection("dns", dnsSection.toSchema())
      builder.addSection("proxy", proxiesSection.toSchema())

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

interface ProxyConfiguration {
  fun name(): String
  fun upstream(): String
  fun downstream(): String
}

interface PeerRepositoryConfiguration {
  fun getName(): String
}

internal class PeerRepositoryConfigurationImpl(private val repoName: String) : PeerRepositoryConfiguration {
  override fun getName(): String = repoName
}

internal class RLPxServiceConfigurationImpl() : RLPxServiceConfiguration {
  override fun port(): Int = 0

  override fun networkInterface(): String = "127.0.0.1"

  override fun advertisedPort(): Int = 0

  override fun repository(): String = "default"

  override fun getName(): String = "default"

  override fun clientName(): String = "Apache Tuweni 1.1"

  override fun peerRepository(): String = "default"
}

internal class GenesisFileConfigurationImpl() : GenesisFileConfiguration {
  override fun getName(): String = "default"

  override fun genesisFile(): GenesisFile =
    GenesisFile.read(GenesisFileConfigurationImpl::class.java.getResource("/default.json").readBytes())
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

data class ProxyConfigurationImpl(private val name: String, private val upstream: String, private val downstream: String) : ProxyConfiguration {
  override fun name() = name

  override fun upstream() = upstream
  override fun downstream() = upstream
}
