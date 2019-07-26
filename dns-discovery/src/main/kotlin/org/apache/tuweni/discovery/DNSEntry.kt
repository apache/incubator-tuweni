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

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.io.Base32
import org.apache.tuweni.io.Base64URLSafe

/**
 * Intermediate format to write DNS entries
 */
internal interface DNSEntry {

  companion object {

    fun readDNSEntry(serialized: String): DNSEntry {
      val attrs = serialized.split(" ").map {
        val equalseparator = it.indexOf("=")
        if (equalseparator == -1) {
          throw InvalidEntryException("Invalid record entry $serialized")
        }
        val key = it.substring(0, equalseparator)
        val value = it.substring(equalseparator + 1)
        Pair(key, value)
      }.toMap()
      if (attrs.containsKey("enrtree-root")) {
        return ENRTreeRoot(attrs)
      } else if (attrs.containsKey("enrtree")) {
        return ENRTree(attrs)
      } else if (attrs.containsKey("enr")) {
        return ENRNode(attrs)
      } else if (attrs.containsKey("enrtree-link")) {
        return ENRTreeLink(attrs)
      } else {
        throw InvalidEntryException("$serialized should contain enrtree, enr, enrtree-root or enrtree-link")
      }
    }
  }
}

class ENRNode(attrs: Map<String, String>) : DNSEntry {

  val nodeRecord: EthereumNodeRecord

  init {
    if (attrs["enr"] == null) {
      throw InvalidEntryException("Missing attributes on enr entry")
    }
    nodeRecord = EthereumNodeRecord.fromRLP(Base64URLSafe.decode(attrs["enr"]!!))
  }
}

class ENRTreeRoot(attrs: Map<String, String>) : DNSEntry {

  val version: String
  val seq: Int
  val sig: SECP256K1.Signature
  val hash: Bytes
  init {
    if (attrs["enrtree-root"] == null || attrs["seq"] == null || attrs["sig"] == null || attrs["hash"] == null) {
      throw InvalidEntryException("Missing attributes on root entry")
    }

    version = attrs["enrtree-root"]!!
    seq = attrs["seq"]!!.toInt()
    sig = SECP256K1.Signature.fromBytes(Base64URLSafe.decode(attrs["sig"]!!))
    hash = Base32.decode(attrs["hash"]!!)
  }

  override fun toString(): String {
    val encodedHash = Base32.encode(hash)
    return "enrtree-root=$version hash=${encodedHash.subSequence(0, encodedHash.indexOf("="))} " +
      "seq=$seq sig=${Base64URLSafe.encode(sig.bytes())}"
  }
}

class ENRTree(attrs: Map<String, String>) : DNSEntry {

  val entries: List<String>
  init {
    val attr = attrs["enrtree"] ?: throw InvalidEntryException("Missing attributes on enrtree entry")
    entries = attr.split(",")
  }

  override fun toString(): String {
    return "enrtree=${entries.joinToString(",")}"
  }
}

class ENRTreeLink(attrs: Map<String, String>) : DNSEntry {

  val domainName: String
  init {
    val attr = attrs["enrtree-link"] ?: throw InvalidEntryException("Missing attributes on enrtree-link entry")
    domainName = attr
  }

  override fun toString(): String {
    return "enrtree-link=$domainName"
  }
}

class InvalidEntryException(message: String?) : RuntimeException(message)
