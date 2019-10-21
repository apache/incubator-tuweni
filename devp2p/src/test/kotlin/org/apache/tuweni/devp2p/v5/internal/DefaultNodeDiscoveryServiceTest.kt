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
package org.apache.tuweni.devp2p.v5.internal

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.NodeDiscoveryService
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.io.Base64URLSafe
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.time.Instant

@ExtendWith(BouncyCastleExtension::class)
class DefaultNodeDiscoveryServiceTest {

  private val recipientKeyPair: SECP256K1.KeyPair = SECP256K1.KeyPair.random()
  private val recipientEnr: Bytes =
    EthereumNodeRecord.toRLP(recipientKeyPair, ip = InetAddress.getLocalHost(), udp = 9091)
  private val encodedEnr: String = "enr:${Base64URLSafe.encode(recipientEnr)}"

  private val keyPair: SECP256K1.KeyPair = SECP256K1.KeyPair.random()
  private val localPort: Int = 9090
  private val bindAddress: InetSocketAddress = InetSocketAddress(localPort)
  private val bootstrapENRList: List<String> = listOf(encodedEnr)
  private val enrSeq: Long = Instant.now().toEpochMilli()
  private val selfENR: Bytes = EthereumNodeRecord.toRLP(
    keyPair,
    enrSeq,
    emptyMap(),
    bindAddress.address,
    null,
    bindAddress.port
  )
  private val connector: UdpConnector = DefaultUdpConnector(bindAddress, keyPair, selfENR)

  private val nodeDiscoveryService: NodeDiscoveryService =
    DefaultNodeDiscoveryService(keyPair, localPort, bindAddress, bootstrapENRList, enrSeq, selfENR, connector)

  @Test
  fun startInitializesConnectorAndBootstraps() {
    val recipientSocket = CoroutineDatagramChannel.open()
    recipientSocket.bind(InetSocketAddress(9091))

    nodeDiscoveryService.start()

    runBlocking {
      val buffer = ByteBuffer.allocate(UdpMessage.MAX_UDP_MESSAGE_SIZE)
      recipientSocket.receive(buffer)
      buffer.flip()
      val receivedBytes = Bytes.wrapByteBuffer(buffer)
      val content = receivedBytes.slice(45)

      val message = RandomMessage.create(content)
      assert(message.data.size() == UdpMessage.RANDOM_DATA_LENGTH)
    }

    assert(connector.started())

    recipientSocket.close()
    nodeDiscoveryService.terminate()
  }

  @Test
  fun terminateShutsDownService() {
    nodeDiscoveryService.start()

    assert(connector.started())

    nodeDiscoveryService.terminate(true)

    assert(!connector.available())
  }
}
