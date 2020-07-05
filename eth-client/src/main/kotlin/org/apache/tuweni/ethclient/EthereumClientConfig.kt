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

  companion object {
    fun createSchema(): Schema {
      val storageSection = SchemaBuilder.create()
      storageSection.addString("path", null, "File system path where data is stored", null)
      storageSection.addString("genesis", null, "Reference to a genesis configuration", null)

      val builder = SchemaBuilder.create()
      builder.addSection("storage", storageSection.toSchema())

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
  fun domain(): String
  fun pollingPeriod(): Long
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

data class DNSConfigurationImpl(private val domain: String, private val pollingPeriod: Long) : DNSConfiguration {
  override fun domain(): String = domain

  override fun pollingPeriod(): Long = pollingPeriod
}
