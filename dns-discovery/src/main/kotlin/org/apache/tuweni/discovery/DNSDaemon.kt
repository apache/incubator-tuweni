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

import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.slf4j.LoggerFactory
import org.xbill.DNS.ExtendedResolver
import org.xbill.DNS.Resolver
import java.util.Timer
import java.util.TimerTask

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
  private val resolver: Resolver = if (dnsServer != null) ExtendedResolver(arrayOf(dnsServer)) else ExtendedResolver()
) {
  companion object {
    val logger = LoggerFactory.getLogger(DNSDaemon::class.java)
  }

  val listeners = HashSet<DNSDaemonListener>()

  private val timer: Timer = Timer(false)

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
    timer.scheduleAtFixedRate(DNSTimerTask(resolver, seq, enrLink, this::updateRecords), 0, period)
  }

  /**
   * Close the daemon.
   */
  public fun close() {
    timer.cancel()
  }
}

internal class DNSTimerTask(
  private val resolver: Resolver,
  private val seq: Long,
  private val enrLink: String,
  private val records: (List<EthereumNodeRecord>) -> Unit,
  private val dnsResolver: DNSResolver = DNSResolver(null, seq, resolver)
) : TimerTask() {

  companion object {
    val logger = LoggerFactory.getLogger(DNSTimerTask::class.java)
  }

  override fun run() {
    logger.info("Refreshing DNS records with $enrLink")
    records(dnsResolver.collectAll(enrLink))
  }
}
