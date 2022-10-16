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
package org.apache.tuweni.scuttlebutt.discovery

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.scuttlebutt.Identity
import java.net.InetSocketAddress
import java.util.Objects
import java.util.regex.Pattern

/**
 * Representation of an identity associated with an IP and port, used for Scuttlebutt local discovery.
 *
 *
 * See https://ssbc.github.io/scuttlebutt-protocol-guide/ for a detailed description of this identity.
 *
 * @param addr the address associated with this local identity
 * @param id the identity
 * @throws NumberFormatException if the port does not represent a number
 * @throws IllegalArgumentException if the port is not in the valid port range 0-65536
 */
class LocalIdentity(val addr: InetSocketAddress, val id: Identity) {

  companion object {
    private val regexpPattern = Pattern.compile("^net:(.*):(.*)~shs:(.*)$")

    /**
     * Create a local identity from a String of the form net:IP address:port~shs:base64 of public key
     *
     * @param str the String to interpret
     * @return the identity or null if the string doesn't match the format.
     */
    @JvmStatic
    fun fromString(str: String?): LocalIdentity? {
      val result = regexpPattern.matcher(str)
      return if (!result.matches()) {
        null
      } else {
        LocalIdentity(
          result.group(1),
          result.group(2),
          Identity.fromPublicKey(Signature.PublicKey.fromBytes(Bytes.fromBase64String(result.group(3))))
        )
      }
    }
  }

  /**
   * Constructor for a local identity
   *
   * @param ip the IP address associated with this local identity
   * @param port the port associated with this local identity
   * @param id the identity
   * @throws NumberFormatException if the port does not represent a number
   * @throws IllegalArgumentException if the port is not in the valid port range 0-65536
   */
  constructor(ip: String, port: String, id: Identity) : this(ip, Integer.valueOf(port), id)

  /**
   * Constructor for a local identity
   *
   * @param ip the IP address associated with this local identity
   * @param port the port associated with this local identity
   * @param id the identity
   * @throws IllegalArgumentException if the port is not in the valid port range 0-65536
   */
  constructor(ip: String, port: Int, id: Identity) : this(InetSocketAddress(ip, port), id)

  /**
   * The canonical form of an invite
   *
   * @return the local identity in canonical form according to the Scuttlebutt protocol guide.
   */
  fun toCanonicalForm(): String {
    return "net:" + addr.hostString + ":" + addr.port + "~shs:" + id.publicKeyAsBase64String()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val that = other as LocalIdentity
    return toCanonicalForm() == that.toCanonicalForm()
  }

  override fun hashCode(): Int {
    return Objects.hash(id, addr)
  }

  override fun toString(): String {
    return toCanonicalForm()
  }
}
