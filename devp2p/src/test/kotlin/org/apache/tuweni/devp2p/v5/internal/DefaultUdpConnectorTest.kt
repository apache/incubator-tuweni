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
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
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
class DefaultUdpConnectorTest {

  private val keyPair: SECP256K1.KeyPair = SECP256K1.KeyPair.random()
  private val nodeId: Bytes = Bytes.fromHexString("0x98EB6D611291FA21F6169BFF382B9369C33D997FE4DC93410987E27796360640")
  private val address: InetSocketAddress = InetSocketAddress(9090)
  private val selfEnr: Bytes = EthereumNodeRecord.toRLP(keyPair, ip = address.address)

  private val data: Bytes = UdpMessage.randomData()
  private val message: RandomMessage = RandomMessage(data)

  private var connector: UdpConnector = DefaultUdpConnector(address, keyPair, selfEnr, nodeId)

  @BeforeEach
  fun setUp() {
    connector = DefaultUdpConnector(address, keyPair, selfEnr, nodeId)
  }

  @AfterEach
  fun tearDown() {
    if (connector.started()) {
      connector.terminate()
    }
  }

  @Test
  fun startOpensChannelForMessages() {
    connector.start()

    assert(connector.available())
  }

  @Test
  fun terminateShutdownsConnector() {
    connector.start()

    assert(connector.available())

    connector.terminate()

    assert(!connector.available())
  }

  @Test
  fun sendSendsValidDatagram() {
    connector.start()

    val destNodeId = Bytes.random(32)

    val receiverAddress = InetSocketAddress(InetAddress.getLocalHost(), 9091)
    val socketChannel = CoroutineDatagramChannel.open()
    socketChannel.bind(receiverAddress)

    runBlocking {
      connector.send(receiverAddress, message, destNodeId)
      val buffer = ByteBuffer.allocate(UdpMessage.MAX_UDP_MESSAGE_SIZE)
      socketChannel.receive(buffer) as InetSocketAddress
      buffer.flip()

      val messageContent = Bytes.wrapByteBuffer(buffer).slice(45)
      val message = RandomMessage.create(messageContent)

      assert(message.data == data)
    }
    socketChannel.close()
  }
}
