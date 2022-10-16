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
package org.apache.tuweni.discovery

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.slf4j.LoggerFactory

/**
 * Resolves DNS records over time, refreshing records.
 *
 * @param enrLink the ENR link to start with, of the form enrtree://PUBKEY@domain
 * @param listener Listener notified when records are read and whenever they are updated.
 * @param dnsServer the DNS server to use for DNS query. If null, the default DNS server will be used.
 * @param seq the sequence number of the root record. If the root record seq is higher, proceed with visit.
 * @param period the period at which to poll DNS records
 * @param resolver
 */
class DNSDaemon @JvmOverloads constructor(
  private val enrLink: String,
  private val listener: DNSDaemonListener?,
  private val seq: Long = 0,
  private val period: Long = 60000L,
  private val dnsServer: String? = null,
  private val vertx: Vertx
) {
  companion object {
    val logger = LoggerFactory.getLogger(DNSDaemon::class.java)
  }

  private var periodicHandle: Long = 0

  val listeners = HashSet<DNSDaemonListener>()

  init {
    listener?.let {
      listeners.add(listener)
    }
  }

  private fun updateRecords(records: List<EthereumNodeRecord>) {
    listeners.forEach { it.newRecords(seq, records) }
  }

  fun start() {
    logger.trace("Starting DNSDaemon for $enrLink")
    val task = DNSTimerTask(vertx, seq, enrLink, this::updateRecords)
    this.periodicHandle = vertx.setPeriodic(period, task::run)
  }

  /**
   * Close the daemon.
   */
  public fun close() {
    vertx.cancelTimer(this.periodicHandle)
  }
}

internal class DNSTimerTask(
  private val vertx: Vertx,
  private val seq: Long,
  private val enrLink: String,
  private val records: (List<EthereumNodeRecord>) -> Unit,
  private val dnsServer: String? = null,
  private val dnsResolver: DNSResolver = DNSResolver(dnsServer, seq, vertx)
) {

  companion object {
    val logger = LoggerFactory.getLogger(DNSTimerTask::class.java)
  }

  fun run(@Suppress("UNUSED_PARAMETER") timerId: Long) {
    logger.debug("Refreshing DNS records with $enrLink")
    runBlocking {
      records(dnsResolver.collectAll(enrLink))
    }
  }
}
