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
package org.apache.tuweni.devp2p.v5

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.internal.DefaultAuthenticationProvider
import org.apache.tuweni.devp2p.v5.internal.DefaultNodeDiscoveryService
import org.apache.tuweni.devp2p.v5.internal.DefaultPacketCodec
import org.apache.tuweni.devp2p.v5.internal.DefaultUdpConnector
import org.apache.tuweni.devp2p.v5.packet.FindNodeMessage
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import org.apache.tuweni.io.Base64URLSafe
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer

@ExtendWith(BouncyCastleExtension::class)
class HandshakeIntegrationTest {

  private val keyPair: SECP256K1.KeyPair = SECP256K1.KeyPair.random()
  private val enr: Bytes = EthereumNodeRecord.toRLP(keyPair, ip = InetAddress.getLocalHost(), udp = SERVICE_PORT)
  private val address: InetSocketAddress = InetSocketAddress(InetAddress.getLocalHost(), SERVICE_PORT)
  private val connector: UdpConnector = DefaultUdpConnector(address, keyPair, enr)

  private val clientKeyPair = SECP256K1.KeyPair.random()
  private val clientEnr = EthereumNodeRecord.toRLP(clientKeyPair, ip = InetAddress.getLocalHost(), udp = CLIENT_PORT)
  private val authProvider = DefaultAuthenticationProvider(clientKeyPair, clientEnr)
  private val clientCodec = DefaultPacketCodec(clientKeyPair, clientEnr, authenticationProvider = authProvider)
  private val socket = CoroutineDatagramChannel.open()

  private val clientNodeId: Bytes = Hash.sha2_256(clientEnr)

  private val bootList = listOf("enr:${Base64URLSafe.encode(clientEnr)}")
  private val service: NodeDiscoveryService =
    DefaultNodeDiscoveryService(keyPair, SERVICE_PORT, bootstrapENRList = bootList, connector = connector)

  @BeforeEach
  fun init() {
    socket.bind(InetSocketAddress(9091))
    service.start()
  }

  @Test
  fun discv5HandshakeTest() {
    runBlocking {
      val buffer = ByteBuffer.allocate(UdpMessage.MAX_UDP_MESSAGE_SIZE)
      socket.receive(buffer)
      buffer.flip()

      var content = Bytes.wrapByteBuffer(buffer)
      var decodingResult = clientCodec.decode(content)
      assert(decodingResult.message is RandomMessage)
      buffer.clear()

      sendWhoAreYou()

      socket.receive(buffer)
      buffer.flip()
      content = Bytes.wrapByteBuffer(buffer)
      decodingResult = clientCodec.decode(content)
      assert(decodingResult.message is FindNodeMessage)
      assert(null != authProvider.findSessionKey(Hash.sha2_256(enr).toHexString()))

      val message = decodingResult.message as FindNodeMessage

      assert(message.distance == 0L)
      assert(message.requestId.size() == UdpMessage.REQUEST_ID_LENGTH)
    }
  }

  @AfterEach
  fun tearDown() {
    service.terminate(true)
    socket.close()
  }

  private suspend fun sendWhoAreYou() {
    val message = WhoAreYouMessage()
    val bytes = clientCodec.encode(message, clientNodeId)
    val buffer = ByteBuffer.wrap(bytes.toArray())
    socket.send(buffer, address)
  }

  companion object {
    private const val SERVICE_PORT: Int = 9090
    private const val CLIENT_PORT: Int = 9091
  }
}
