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
import org.xbill.DNS.Resolver
import org.xbill.DNS.SimpleResolver
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicReference

/**
 * Resolves DNS records over time, refreshing records.
 *
 * @param dnsServer the DNS server to use for DNS query. If null, the default DNS server will be used.
 * @param seq the sequence number of the root record. If the root record seq is higher, proceed with visit.
 * @param enrLink the ENR link to start with, of the form enrtree://PUBKEY@domain
 * @param period the period at which to poll DNS records
 * @param resolver
 */
public class DNSDaemon @JvmOverloads constructor(
  private val dnsServer: String? = null,
  private val seq: Long = 0,
  private val enrLink: String,
  private val period: Long = 60000L,
  private val resolver: Resolver = SimpleResolver(dnsServer)
) {

  /**
   * Listeners notified when records are read and whenever they are updated.
   */
  val listeners = HashSet<(List<EthereumNodeRecord>) -> Unit>()

  private val timer: Timer = Timer(false)
  private val records = AtomicReference<EthereumNodeRecord>()

  init {
    timer.scheduleAtFixedRate(DNSTimerTask(resolver, seq, enrLink, this::updateRecords), 0, period)
  }

  private fun updateRecords(records: List<EthereumNodeRecord>) {
    listeners.forEach { it(records) }
  }

  /**
   * Close the daemon.
   */
  public fun close() {
    timer.cancel()
  }
}

class DNSTimerTask(
  private val resolver: Resolver,
  private val seq: Long,
  private val enrLink: String,
  private val records: (List<EthereumNodeRecord>) -> Unit,
  private val dnsResolver: DNSResolver = DNSResolver(null, seq, resolver)
) : TimerTask() {

  override fun run() {
    records(dnsResolver.collectAll(enrLink))
  }
}
