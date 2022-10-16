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

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.rlpx.MemoryWireConnectionsRepository
import org.apache.tuweni.rlpx.SubprotocolService
import org.apache.tuweni.rlpx.WireConnectionRepository
import org.apache.tuweni.rlpx.wire.DisconnectReason
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier
import org.apache.tuweni.rlpx.wire.WireConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

internal class SubProtocolServiceImpl() : SubprotocolService {

  var handler: ProxyHandler? = null
  private val connectionRepository = MemoryWireConnectionsRepository()

  override fun send(
    subProtocolIdentifier: SubProtocolIdentifier,
    messageType: Int,
    connection: WireConnection,
    message: Bytes
  ) {
    handler?.handle(connection, messageType, message)
  }

  override fun disconnect(connection: WireConnection, reason: DisconnectReason) {
    TODO("Not yet implemented")
  }

  override fun repository(): WireConnectionRepository = connectionRepository
}

class SimpleClientHandler() : ClientHandler {
  override suspend fun handleRequest(message: Bytes): Bytes {
    return Bytes.wrap(" World".toByteArray())
  }
}

class ProxyTest {

  @Test
  fun testTwoProxies() = runBlocking {
    val subProtocolService = SubProtocolServiceImpl()
    val subProtocolService2 = SubProtocolServiceImpl()
    val proxyClient = ProxyClient(subProtocolService)
    val proxyClient2 = ProxyClient(subProtocolService2)

    val handler = ProxyHandler(service = subProtocolService, client = proxyClient)
    val handler2 = ProxyHandler(service = subProtocolService2, client = proxyClient2)
    subProtocolService.handler = handler2
    subProtocolService2.handler = handler

    val fooPeerInfo = PeerInfo()
    fooPeerInfo.sites = listOf("web")
    proxyClient2.proxyPeerRepository.addPeer("foo", fooPeerInfo)
    proxyClient.registeredSites["web"] = SimpleClientHandler()

    val wireConnection = mock<WireConnection>()
    `when`(wireConnection.uri()).thenReturn("foo")

    subProtocolService2.repository().add(wireConnection)
    val response = proxyClient2.request("web", Bytes.wrap("Hello".toByteArray()))
    assertEquals(" World", String(response.toArrayUnsafe()))
  }
}
