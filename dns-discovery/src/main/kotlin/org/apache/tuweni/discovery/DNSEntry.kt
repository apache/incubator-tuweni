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
import org.apache.tuweni.rlp.InvalidRLPEncodingException
import java.net.URI

/**
 * Intermediate format to write DNS entries
 */
public interface DNSEntry {

  companion object {

    /**
     * Read a DNS entry from a String.
     * @param serialized the serialized form of a DNS entry
     * @return DNS entry if found
     * @throws InvalidEntryException if the record cannot be read
     */
    fun readDNSEntry(serialized: String): DNSEntry {
      var record = serialized
      if (record[0] == '"') {
        record = record.substring(1, record.length - 1)
      }
      if (record.startsWith("enrtree-root:")) {
        return ENRTreeRoot(readKV(record))
      } else if (record.startsWith("enrtree-branch:")) {
        return ENRTree(record.substring("enrtree-branch:".length))
      } else if (record.startsWith("enr:")) {
        return ENRNode(readKV(record))
      } else if (record.startsWith("enrtree:")) {
        return ENRTreeLink(record)
      } else {
        throw InvalidEntryException("$serialized should contain enrtree-branch, enr, enrtree-root or enrtree")
      }
    }

    private fun readKV(record: String): Map<String, String> {
      return record.split(" ").map {
        val equalseparator = it.indexOf("=")
        if (equalseparator == -1) {
          val colonseparator = it.indexOf(":")
          if (colonseparator == -1) {
            throw InvalidEntryException("$it could not be read")
          }
          val key = it.substring(0, colonseparator)
          val value = it.substring(colonseparator + 1)
          Pair(key, value)
        } else {
          val key = it.substring(0, equalseparator)
          val value = it.substring(equalseparator + 1)
          Pair(key, value)
        }
      }.toMap()
    }
  }
}

internal class ENRNode(attrs: Map<String, String>) : DNSEntry {

  val nodeRecord: EthereumNodeRecord

  init {
    if (attrs["enr"] == null) {
      throw InvalidEntryException("Missing attributes on enr entry")
    }
    try {
      nodeRecord = EthereumNodeRecord.fromRLP(Base64URLSafe.decode(attrs["enr"]!!))
    } catch (e: InvalidRLPEncodingException) {
      throw InvalidEntryException(e.message)
    }
  }

  override fun toString(): String {
    return nodeRecord.toString()
  }
}

internal class ENRTreeRoot(attrs: Map<String, String>) : DNSEntry {

  val version: String
  val seq: Long
  val sig: SECP256K1.Signature
  val enrRoot: String
  val linkRoot: String
  init {
    if (attrs["enrtree-root"] == null || attrs["seq"] == null || attrs["sig"] == null || attrs["e"] == null ||
      attrs["l"] == null
    ) {
      throw InvalidEntryException("Missing attributes on root entry")
    }

    version = attrs["enrtree-root"]!!
    seq = attrs["seq"]!!.toLong()
    val sigBytes = Base64URLSafe.decode(attrs["sig"]!!)
    sig = SECP256K1.Signature.fromBytes(
      Bytes.concatenate(
        sigBytes,
        Bytes.wrap(ByteArray(Math.max(0, 65 - sigBytes.size())))
      )
    )
    enrRoot = attrs["e"]!!
    linkRoot = attrs["l"]!!
  }

  override fun toString(): String {
    return "enrtree-root:$version e=$enrRoot " +
      "l=$linkRoot seq=$seq sig=${Base64URLSafe.encode(sig.bytes())}"
  }

  fun signedContent(): String {
    return "enrtree-root:$version e=$enrRoot l=$linkRoot seq=$seq"
  }
}

internal class ENRTree(entriesAsString: String) : DNSEntry {

  val entries: List<String>
  init {
    entries = entriesAsString.split(",", "\"").filter { it.length > 4 }
  }

  override fun toString(): String {
    return "enrtree-branch:${entries.joinToString(",")}"
  }
}

internal class ENRTreeLink(url: String) : DNSEntry {

  val domainName: String
  val pubKey: String
  init {
    val uri = URI.create(url)
    domainName = uri.host
    pubKey = uri.userInfo
  }

  fun publicKey(): SECP256K1.PublicKey {
    val keyBytes = Base32.decodeBytes(pubKey)
    val ecPoint = SECP256K1.Parameters.CURVE.getCurve().decodePoint(keyBytes)
    return SECP256K1.PublicKey.fromBytes(Bytes.wrap(ecPoint.getEncoded(false)).slice(1))
  }

  override fun toString(): String {
    return "enrtree://$pubKey@$domainName"
  }
}

internal class InvalidEntryException(message: String?) : RuntimeException(message)
