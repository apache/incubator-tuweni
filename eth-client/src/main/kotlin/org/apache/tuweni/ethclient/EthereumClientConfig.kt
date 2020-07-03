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
import org.apache.tuweni.config.ConfigurationError
import org.apache.tuweni.config.Schema
import org.apache.tuweni.config.SchemaBuilder
import org.apache.tuweni.eth.genesis.GenesisFile
import org.apache.tuweni.peer.repository.PeerRepository
import picocli.CommandLine
import java.io.FileNotFoundException
import java.lang.IllegalArgumentException
import java.nio.file.Path

/**
 * Configuration of EthereumClient. Can be provided via file or over the wire.
 */
class EthereumClientConfig(private var config: Configuration? = null) {
  @CommandLine.Option(names = ["-c", "--config"], description = ["Configuration file."], defaultValue = "config.toml")
  var configPath: Path? = null

  @CommandLine.Option(names = ["-h", "--help"], description = ["Prints usage prompt"])
  var help: Boolean = false

  fun validate(): List<ConfigurationError> {
    if (config == null) {
      config = if (configPath != null) {
        Configuration.fromToml(configPath!!, createSchema())
      } else {
        Configuration.empty(createSchema())
      }
    }
    return config!!.errors()
  }

  fun dataStores(): List<DataStoreConfiguration> {
    TODO()
  }

  fun getGenesisFile(): GenesisFile {
    val contents = EthereumClient::class.java.getResourceAsStream("/mainnet.json").readAllBytes()
    val genesisFile = GenesisFile.read(contents)
return genesisFile
  }

  fun rlpxServices(): List<RLPxServiceConfiguration> {
    TODO()
  }

  fun genesisFiles(): List<GenesisFileConfiguration> {
    TODO()
  }

  fun peerRepositories(): List<PeerRepositoryConfiguration> {
    TODO()
  }

  companion object {
    fun createSchema(): Schema {
      val builder = SchemaBuilder.create()

      return builder.toSchema()
    }

    fun fromFile(path: Path): EthereumClientConfig {
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
  abstract fun port(): Int
  abstract fun networkInterface(): String
  abstract fun advertisedPort(): Int
  abstract fun repository(): String
  abstract fun getName(): String
  abstract fun clientName(): String
  abstract fun peerRepository(): String
}

interface PeerRepositoryConfiguration {
  abstract fun getName(): String
  abstract fun peerRepository(): PeerRepository
}
