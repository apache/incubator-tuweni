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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.CompletableAsyncCompletion
import org.apache.tuweni.concurrent.CompletableAsyncResult
import org.apache.tuweni.concurrent.coroutines.asyncCompletion
import org.apache.tuweni.devp2p.proxy.ProxySubprotocol.Companion.ID
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlpx.SubprotocolService
import org.apache.tuweni.rlpx.wire.DisconnectReason
import org.apache.tuweni.rlpx.wire.SubProtocolHandler
import org.apache.tuweni.rlpx.wire.WireConnection
import org.slf4j.LoggerFactory
import java.util.WeakHashMap
import kotlin.coroutines.CoroutineContext

const val STATUS = 0
const val REQUEST = 1
const val RESPONSE = 2

class ProxyHandler(
  override val coroutineContext: CoroutineContext = Dispatchers.Default,
  val service: SubprotocolService,
  val client: ProxyClient
) : CoroutineScope, SubProtocolHandler {

  companion object {
    val logger = LoggerFactory.getLogger(ProxyHandler::class.java)
  }

  private val pendingStatus = WeakHashMap<String, PeerInfo>()

  override fun handle(connection: WireConnection, messageType: Int, message: Bytes) = asyncCompletion {
    when (messageType) {
      STATUS -> handleStatus(message, connection)
      REQUEST -> handleRequest(message, connection)
      RESPONSE -> handleResponse(message, connection)
      else -> {
        logger.warn("Unknown message type {}", messageType)
        service.disconnect(connection, DisconnectReason.SUBPROTOCOL_REASON)
      }
    }
  }

  private fun handleResponse(message: Bytes, connection: WireConnection) {
    val response = ResponseMessage.decode(message)
    client.proxyPeerRepository.peers[connection.uri()]?.pendingResponses?.remove(response.id)?.complete(response)
  }

  private suspend fun handleRequest(message: Bytes, connection: WireConnection) {
    val request = RequestMessage.decode(message)

    val siteClient = client.registeredSites[request.site]
    if (siteClient == null) {
      val response =
        ResponseMessage(request.id, request.site, false, Bytes.wrap("No site available ${request.site}".toByteArray()))
      service.send(ID, RESPONSE, connection, response.toRLP())
    } else {
      try {
        val responseContent = siteClient.handleRequest(request.message)
        val response = ResponseMessage(request.id, request.site, true, responseContent)
        service.send(ID, RESPONSE, connection, response.toRLP())
      } catch (t: Throwable) {
        val response =
          ResponseMessage(request.id, request.site, false, Bytes.wrap("Internal server error".toByteArray()))
        service.send(ID, RESPONSE, connection, response.toRLP())
      }
    }
  }

  private fun handleStatus(message: Bytes, connection: WireConnection) {
    val status = StatusMessage.decode(message)
    var peer = pendingStatus.remove(connection.uri())
    if (peer == null) {
      service.send(ID, STATUS, connection, StatusMessage(client.registeredSites.keys.toList()).toRLP())
      peer = PeerInfo()
    }
    client.proxyPeerRepository.addPeer(connection.uri(), peer)
    peer.connect(status.sites)
  }

  override fun handleNewPeerConnection(connection: WireConnection): AsyncCompletion {
    val newPeer = PeerInfo()
    pendingStatus[connection.uri()] = newPeer
    service.send(ID, STATUS, connection, StatusMessage(client.registeredSites.keys.toList()).toRLP())

    return newPeer.ready
  }

  override fun stop(): AsyncCompletion = AsyncCompletion.COMPLETED
}

internal class PeerInfo {

  var sites: List<String>? = null
  val ready: CompletableAsyncCompletion = AsyncCompletion.incomplete()
  val pendingResponses = WeakHashMap<String, CompletableAsyncResult<ResponseMessage>>()

  fun connect(sites: List<String>) {
    this.sites = sites
    ready.complete()
  }

  fun cancel() {
    ready.cancel()
  }
}

data class StatusMessage(val sites: List<String>) {

  companion object {
    fun decode(message: Bytes): StatusMessage = RLP.decodeList(message) {
      val list = mutableListOf<String>()
      while (!it.isComplete) {
        list.add(it.readString())
      }
      StatusMessage(list)
    }
  }

  fun toRLP(): Bytes = RLP.encodeList {
    for (site in sites) {
      it.writeString(site)
    }
  }
}

data class RequestMessage(val id: String, val site: String, val message: Bytes) {

  companion object {
    fun decode(message: Bytes): RequestMessage = RLP.decodeList(message) {
      val id = it.readString()
      val site = it.readString()
      val request = it.readValue()
      RequestMessage(id, site, request)
    }
  }

  fun toRLP(): Bytes = RLP.encodeList {
    it.writeString(id)
    it.writeString(site)
    it.writeValue(message)
  }
}

data class ResponseMessage(val id: String, val site: String, val success: Boolean, val message: Bytes) {

  companion object {
    fun decode(message: Bytes): ResponseMessage = RLP.decodeList(message) {
      val id = it.readString()
      val site = it.readString()
      val success = it.readByte() == 1.toByte()
      val response = it.readValue()
      ResponseMessage(id, site, success, response)
    }
  }

  fun toRLP(): Bytes = RLP.encodeList {
    it.writeString(id)
    it.writeString(site)
    it.writeByte(if (success) 1.toByte() else 0.toByte())
    it.writeValue(message)
  }
}
