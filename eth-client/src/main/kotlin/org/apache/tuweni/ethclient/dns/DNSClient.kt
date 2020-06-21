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
package org.apache.tuweni.ethclient.dns

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.devp2p.DiscoveryService
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.discovery.DNSDaemon
import org.apache.tuweni.ethclient.config.DNSConfig
import org.apache.tuweni.kv.KeyValueStore
import java.net.URI

class DNSClient(
  private val config: DNSConfig,
  private val metadataStore: KeyValueStore<String, String>,
  private val store: KeyValueStore<String, EthereumNodeRecord>
) {

  companion object {
    private val SEQ = "SEQ"
  }

  private var dnsDaemon: DNSDaemon? = null
  private var discoveryService: DiscoveryService? = null

  suspend fun seq(): Long {
    return metadataStore.get(SEQ)?.toLong() ?: 0
  }

  suspend fun seq(seq: Long) {
    metadataStore.put(SEQ, seq.toString())
  }

  suspend fun start() {
    config.dnsDomain()?.let { domain ->
      dnsDaemon = DNSDaemon(
        seq = seq(),
        enrLink = domain,
        period = config.dnsPollingPeriod(),
        listeners = setOf { seq, enrs ->
          runBlocking {
            seq(seq)
            enrs.map {
              val url = URI.create("enode://${it.publicKey().toHexString()}@${it.ip()}:${it.tcp()}").toString()
              store.put(url, it)
            }
          }
        }
      )
    }
    config.discv4BootNodes()?.let { bootNodes ->
      discoveryService = DiscoveryService.open(keyPair = config.discKeyPair(), bootstrapURIs = bootNodes)
    }
  }

  suspend fun stop() {
    dnsDaemon?.close()
    discoveryService?.shutdown()
    discoveryService?.awaitTermination()
  }
}
