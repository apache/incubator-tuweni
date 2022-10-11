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

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.discovery.DNSDaemon
import org.apache.tuweni.discovery.DNSDaemonListener
import org.apache.tuweni.kv.KeyValueStore
import org.apache.tuweni.peer.repository.PeerRepository
import org.slf4j.LoggerFactory

/**
 * Wrapper for running a DNS daemon with configuration.
 */
class DNSClient(
  private val vertx: Vertx,
  private val config: DNSConfiguration,
  private val metadataStore: KeyValueStore<String, String>,
  private val peerRepository: PeerRepository
) {

  companion object {
    private val SEQ = "SEQ"

    private val logger = LoggerFactory.getLogger(DNSClient::class.java)
  }

  private var dnsDaemon: DNSDaemon? = null

  /**
   * @return the current sequence number associated with this host
   */
  suspend fun seq(): Long {
    return metadataStore.get(SEQ)?.toLong() ?: 0
  }

  /**
   * Sets the sequence
   */
  suspend fun seq(seq: Long) {
    metadataStore.put(SEQ, seq.toString())
  }

  /**
   * Start the client
   */
  suspend fun start() {
    logger.info("Starting DNSClient with ${config.enrLink()}")
    config.enrLink().let { link ->
      val daemon = DNSDaemon(
        vertx = vertx,
        dnsServer = config.dnsServer(),
        seq = seq(),
        enrLink = link,
        period = config.pollingPeriod(),
        listener = object : DNSDaemonListener {
          override fun newRecords(seq: Long, records: List<EthereumNodeRecord>) {
            runBlocking {
              seq(seq)
              records.map {
                peerRepository.storeIdentity(it.ip().hostAddress, it.tcp()!!, it.publicKey())
              }
            }
          }
        }
      )
      daemon.start()
      dnsDaemon = daemon
    }
  }

  /**
   * Stop the client
   */
  fun stop() {
    dnsDaemon?.close()
  }
}
