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
package org.apache.tuweni.devp2p.proxy

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.devp2p.proxy.ProxySubprotocol.Companion.ID
import org.apache.tuweni.rlpx.SubprotocolService
import org.apache.tuweni.rlpx.wire.SubProtocolClient
import org.apache.tuweni.rlpx.wire.WireConnection
import java.lang.RuntimeException
import java.util.UUID

interface ClientHandler {
  suspend fun handleRequest(message: Bytes): Bytes
}

class ProxyError(message: String?) : RuntimeException(message)

class ProxyClient(private val service: SubprotocolService) : SubProtocolClient {

  fun knownSites(): List<String> = service.repository().asIterable().map {
    val peerInfo = proxyPeerRepository.peers[it.uri()]
    peerInfo?.sites
  }.filterNotNull().flatten().distinct()

  suspend fun request(site: String, message: Bytes): Bytes {
    val messageId = UUID.randomUUID().toString()
    var selectedConn: WireConnection? = null
    for (conn in service.repository().asIterable()) {
      val peerInfo = proxyPeerRepository.peers[conn.uri()]
      if (peerInfo?.sites?.contains(site) == true) {
        selectedConn = conn
        break
      }
    }
    if (selectedConn == null) {
      throw ProxyError("No peer with site $site available")
    }
    val result = AsyncResult.incomplete<ResponseMessage>()
    proxyPeerRepository.peers[selectedConn.uri()]?.pendingResponses?.put(messageId, result)
    service.send(ID, REQUEST, selectedConn, RequestMessage(messageId, site, message).toRLP())
    val response = result.await()
    if (!response.success) {
      throw ProxyError(String(response.message.toArrayUnsafe()))
    }
    return response.message
  }

  val registeredSites = mutableMapOf<String, ClientHandler>()
  internal val proxyPeerRepository = ProxyPeerRepository()
}
