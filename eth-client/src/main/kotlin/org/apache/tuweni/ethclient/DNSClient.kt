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

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.discovery.DNSDaemon
import org.apache.tuweni.kv.KeyValueStore
import org.apache.tuweni.peer.repository.PeerRepository

/**
 * Wrapper for running a DNS daemon with configuration.
 */
class DNSClient(
  private val config: DNSConfiguration,
  private val metadataStore: KeyValueStore<String, String>,
  private val peerRepository: PeerRepository
) {

  companion object {
    private val SEQ = "SEQ"
  }

  private var dnsDaemon: DNSDaemon? = null

  suspend fun seq(): Long {
    return metadataStore.get(SEQ)?.toLong() ?: 0
  }

  suspend fun seq(seq: Long) {
    metadataStore.put(SEQ, seq.toString())
  }

  suspend fun start() {
    config.domain().let { domain ->
      dnsDaemon = DNSDaemon(
        seq = seq(),
        enrLink = domain,
        period = config.pollingPeriod(),
        listeners = setOf { seq, enrs ->
          runBlocking {
            seq(seq)
            enrs.map {
              peerRepository.storeIdentity(it.ip().hostAddress, it.tcp(), it.publicKey())
            }
          }
        }
      )
    }
  }

  fun stop() {
    dnsDaemon?.close()
  }
}
