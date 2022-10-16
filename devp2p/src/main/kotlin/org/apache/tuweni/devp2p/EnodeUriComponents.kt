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

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import java.net.URI
import java.util.regex.Pattern

private val DISCPORT_QUERY_STRING_REGEX = Pattern.compile(".*discport=([^&]+).*")

/**
 * The components of an enode URI.
 * @param nodeId the public key of the node
 * @param endpoint the ndoe endpoint
 */
data class EnodeUriComponents(val nodeId: SECP256K1.PublicKey, val endpoint: Endpoint)

/**
 * Parse an enode URI.
 *
 * @param uri the URI to parse
 * @return the node id and the endpoint
 * @throws IllegalArgumentException if the uri is not a valid enode URI
 */
fun parseEnodeUri(uri: URI): EnodeUriComponents {
  require("enode" == uri.scheme) { "URI must be an enode:// uri" }
  require(uri.userInfo != null) { "URI must have a node id" }
  val nodeId = SECP256K1.PublicKey.fromBytes(Bytes.fromHexString(uri.userInfo))

  var tcpPort = Endpoint.DEFAULT_PORT
  if (uri.port >= 0) {
    tcpPort = uri.port
  }

  // If TCP and UDP ports differ, expect a query param 'discport' with the UDP port.
  // See https://github.com/ethereum/wiki/wiki/enode-url-format
  var udpPort = tcpPort
  val query = uri.query
  if (query != null) {
    val matcher = DISCPORT_QUERY_STRING_REGEX.matcher(query)
    if (matcher.matches()) {
      try {
        udpPort = Integer.parseInt(matcher.group(1))
      } catch (e: NumberFormatException) {
        throw IllegalArgumentException("Invalid discport query parameter")
      }
    }
  }

  return EnodeUriComponents(
    nodeId,
    Endpoint(uri.host, udpPort, tcpPort)
  )
}
