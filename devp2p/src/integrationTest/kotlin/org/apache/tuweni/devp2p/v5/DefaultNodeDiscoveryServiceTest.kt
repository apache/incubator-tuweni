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
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.internal.DefaultUdpConnector
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.io.Base64URLSafe
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.time.Instant

@Timeout(10)
@ExtendWith(BouncyCastleExtension::class)
class DefaultNodeDiscoveryServiceTest {

  private val recipientKeyPair: SECP256K1.KeyPair = SECP256K1.KeyPair.random()
  private val recipientEnr: Bytes =
    EthereumNodeRecord.toRLP(recipientKeyPair, ip = InetAddress.getLoopbackAddress(), udp = 9001)
  private val encodedEnr: String = "enr:${Base64URLSafe.encode(recipientEnr)}"
  private val keyPair: SECP256K1.KeyPair = SECP256K1.KeyPair.random()
  private val localPort: Int = 9000
  private val bindAddress: InetSocketAddress = InetSocketAddress("localhost", localPort)
  private val bootstrapENRList: List<String> = listOf(encodedEnr)
  private val enrSeq: Long = Instant.now().toEpochMilli()
  private val selfENR: Bytes = EthereumNodeRecord.toRLP(
    keyPair,
    enrSeq,
    emptyMap(),
    emptyMap(),
    bindAddress.address,
    null,
    bindAddress.port
  )
  private val connector: UdpConnector =
    DefaultUdpConnector(bindAddress, keyPair, selfENR)

  private val nodeDiscoveryService: NodeDiscoveryService =
    DefaultNodeDiscoveryService.open(
      bootstrapENRList,
      connector = connector
    )

  @Test
  fun startInitializesConnectorAndBootstraps() = runBlocking {
    val recipientSocket = CoroutineDatagramChannel.open()
    recipientSocket.bind(InetSocketAddress("localhost", 9001))

    nodeDiscoveryService.start()

    val buffer = ByteBuffer.allocate(UdpMessage.MAX_UDP_MESSAGE_SIZE)
    recipientSocket.receive(buffer)
    buffer.flip()
    val receivedBytes = Bytes.wrapByteBuffer(buffer)
    val content = receivedBytes.slice(45)

    val message = RandomMessage.create(UdpMessage.authTag(), content)
    assertTrue(message.data.size() == UdpMessage.RANDOM_DATA_LENGTH)

    assertTrue(connector.started())

    recipientSocket.close()
    nodeDiscoveryService.terminate()

    assertTrue(!connector.started())
  }
}
