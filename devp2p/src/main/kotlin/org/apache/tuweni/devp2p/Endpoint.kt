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
package org.apache.tuweni.devp2p

import io.vertx.core.net.SocketAddress
import org.apache.tuweni.rlp.RLPException
import org.apache.tuweni.rlp.RLPReader
import org.apache.tuweni.rlp.RLPWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException

private fun parseInetAddress(address: String): InetAddress {
  require(Character.digit(address[0], 16) != -1 || address[0] == ':') {
    "address should be a literal IP address, got $address"
  }
  return InetAddress.getByName(address)
}

/**
 * An Ethereum node endpoint.
 *
 * @constructor Create a new endpoint.
 * @param address the InetAddress
 * @param udpPort the UDP port for the endpoint
 * @param tcpPort the TCP port for the endpoint or `null` if no TCP port is known
 * @throws IllegalArgumentException if either port is out of range
 */
data class Endpoint(
  val address: String,
  val udpPort: Int = DEFAULT_PORT,
  val tcpPort: Int? = null
) {

  /**
   * Create a new endpoint.
   *
   * @param address a SocketAddress, containing the IP address the UDP port
   */
  constructor(address: SocketAddress, tcpPort: Int? = null) : this(address.host(), address.port(), tcpPort)

  companion object {

    /**
     * The default port used by Ethereum DevP2P.
     */
    const val DEFAULT_PORT = 30303

    /**
     * Create an Endpoint by reading fields from the RLP input stream.
     *
     * If the fields are wrapped into an RLP list, use `reader.readList` to unwrap before calling this method.
     *
     * @param reader the RLP input stream from which to read
     * @return the decoded endpoint
     * @throws RLPException if the RLP source does not decode to a valid endpoint
     */
    fun readFrom(reader: RLPReader): Endpoint {
      val addr: InetAddress
      try {
        addr = InetAddress.getByAddress(reader.readValue().toArrayUnsafe())
      } catch (e: UnknownHostException) {
        throw RLPException(e)
      }

      var udpPort = reader.readInt()
      if (udpPort == 0) { // this is an invalid port number we see in the wild. Use DEFAULT_PORT instead.
        udpPort = DEFAULT_PORT
      }
      // Some implementations seem to send packets that either do not have the TCP port field, or to have an
      // RLP NULL value for it.
      var tcpPort: Int? = null
      if (!reader.isComplete) {
        tcpPort = reader.readInt()
        if (tcpPort == 0) {
          tcpPort = null
        }
      }

      return Endpoint(addr.hostAddress, udpPort, tcpPort)
    }
  }

  init {
    require(udpPort in 1..65535) { "udpPort should be between 1 and 65535, got $udpPort" }
    require(tcpPort == null || tcpPort in 1..65535) { "tcpPort should be between 1 and 65535, got $tcpPort" }
  }

  /**
   * UDP socket address of the endpoint
   */
  val udpSocketAddress: SocketAddress = SocketAddress.inetSocketAddress(udpPort, address)

  /**
   * TCP socket address of the endpoint, if set
   */
  val tcpSocketAddress: InetSocketAddress? = if (tcpPort != null) InetSocketAddress(address, tcpPort) else null

  /**
   * Write this endpoint to an RLP output.
   *
   * @param writer the RLP writer
   */
  internal fun writeTo(writer: RLPWriter) {
    writer.writeByteArray(InetAddress.getByName(address).address)
    writer.writeInt(udpPort)
    writer.writeInt(tcpPort ?: 0)
  }

  // rough over-estimate, assuming maximum size encoding for the port numbers
  internal fun rlpSize(): Int = 1 + InetAddress.getByName(address).address.size + 2 * (1 + 2)
}
