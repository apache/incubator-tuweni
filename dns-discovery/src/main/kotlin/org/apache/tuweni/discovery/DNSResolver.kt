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

import io.vertx.core.Vertx
import io.vertx.core.dns.DnsClient
import io.vertx.core.dns.DnsClientOptions
import io.vertx.core.dns.DnsException
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.coroutines.asyncCompletion
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Resolves a set of ENR nodes from a host name.
 *
 * @param dnsServer the DNS server to use for DNS query. If null, the default DNS server will be used.
 * @param seq the sequence number of the root record. If the root record seq is higher, proceed with visit.
 */
class DNSResolver @JvmOverloads constructor(
  private val dnsServer: String? = null,
  var seq: Long = 0,
  val vertx: Vertx,
  private val dnsClient: DnsClient = vertx.createDnsClient(
    DnsClientOptions().apply {
      if (dnsServer != null) {
        this.host = dnsServer
      }
    }
  )
) {

  companion object {
    internal val logger = LoggerFactory.getLogger(DNSResolver::class.java)
  }

  /**
   * Resolves one DNS record associated with the given domain name.
   *
   * @param domainName the domain name to query
   * @return the DNS entry read from the domain
   */
  suspend fun resolveRecord(domainName: String): DNSEntry? {
    return resolveRecordRaw(domainName)?.let { DNSEntry.readDNSEntry(it) }
  }

  private fun checkSignature(root: ENRTreeRoot, pubKey: SECP256K1.PublicKey, sig: SECP256K1.Signature): Boolean {
    val hash = Hash.keccak256(root.signedContent().toByteArray())
    return SECP256K1.verifyHashed(hash, sig, pubKey)
  }

  /**
   * Convenience method to read all ENRs, from a top-level record.
   *
   * @param enrLink the ENR link to start with, of the form enrtree://PUBKEY@domain
   * @return all ENRs collected
   */
  suspend fun collectAll(enrLink: String): List<EthereumNodeRecord> {
    val nodes = mutableListOf<EthereumNodeRecord>()
    val visitor = object : DNSVisitor {
      override fun visit(enr: EthereumNodeRecord): Boolean {
        nodes.add(enr)
        return true
      }
    }
    visitTree(enrLink, visitor)
    if (nodes.size > 0) {
      logger.info("Resolved ${nodes.size} nodes")
    } else {
      logger.debug("No nodes resolved")
    }
    return nodes
  }

  /**
   * Reads a complete tree of record, starting with the top-level record.
   * @param enrLink the ENR link to start with
   * @param visitor the visitor that will look at each record
   */
  suspend fun visitTree(enrLink: String, visitor: DNSVisitor) {
    val link = ENRTreeLink(enrLink)
    visitTree(link, visitor)
  }

  fun visitTreeAsync(enrLink: String, visitor: DNSVisitor): AsyncCompletion {
    return runBlocking {
      return@runBlocking asyncCompletion {
        visitTree(enrLink, visitor)
      }
    }
  }

  /**
   * Reads a complete tree of record, starting with the top-level record.
   * @param link the ENR link to start with
   * @param visitor the visitor that will look at each record
   */
  private suspend fun visitTree(link: ENRTreeLink, visitor: DNSVisitor) {
    val entry = resolveRecord(link.domainName)
    if (entry !is ENRTreeRoot) {
      logger.debug("Root entry $entry is not an ENR tree root")
      return
    }
    if (!checkSignature(entry, link.publicKey(), entry.sig)) {
      logger.debug("ENR tree root ${link.domainName} failed signature check")
      return
    }
    if (entry.seq <= seq) {
      logger.debug("ENR tree root seq $entry.seq is not higher than $seq, aborting")
      return
    }
    seq = entry.seq

    internalVisit(entry.enrRoot, link.domainName, visitor)
    internalVisit(entry.linkRoot, link.domainName, visitor)
  }

  /**
   * Resolves the first TXT record associated with a domain name,
   * and returns it, or null if no such record exists or the record cannot be read.
   *
   * @param domainName the name of the DNS domain to query
   * @return the first TXT entry of the DNS record
   */
  suspend fun resolveRecordRaw(domainName: String): String? {
    try {
      val records = dnsClient.resolveTXT(domainName).await()
      if (records.isNotEmpty()) {
        return records[0]
      } else {
        logger.debug("No TXT record for $domainName")
        return null
      }
    } catch (e: DnsException) {
      logger.warn("DNS query error with $domainName", e)
      return null
    } catch (e: IOException) {
      logger.warn("I/O exception contacting remote DNS server when resolving $domainName", e)
      return null
    }
  }

  private suspend fun internalVisit(entryName: String, domainName: String, visitor: DNSVisitor): Boolean {
    try {
      val entry = resolveRecord("$entryName.$domainName") ?: return true

      if (entry is ENRNode) {
        return visitor.visit(entry.nodeRecord)
      } else if (entry is ENRTree) {
        for (e in entry.entries) {
          val keepGoing = internalVisit(e, domainName, visitor)
          if (!keepGoing) {
            return false
          }
        }
      } else if (entry is ENRTreeLink) {
        visitTree(entry, visitor)
      } else {
        logger.debug("Unsupported type of node $entry")
      }
      return true
    } catch (e: InvalidEntryException) {
      logger.warn(e.message, e)
      return true
    } catch (e: IllegalArgumentException) {
      logger.warn(e.message, e)
      return true
    }
  }
}
