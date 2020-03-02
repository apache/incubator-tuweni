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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.MessageObserver
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.devp2p.v5.storage.RoutingTable
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer

@ObsoleteCoroutinesApi
@ExtendWith(BouncyCastleExtension::class)
@Execution(ExecutionMode.SAME_THREAD)
class DefaultUdpConnectorTest {

  companion object {
    private var counter = 0
  }

  private var connector: DefaultUdpConnector? = null

  @BeforeEach
  fun setUp() {
    val address = InetSocketAddress(9090 + counter)
    val keyPair = SECP256K1.KeyPair.random()
    val selfEnr = EthereumNodeRecord.toRLP(keyPair, ip = address.address)
    connector = DefaultUdpConnector(address, keyPair, selfEnr)
    counter += 1
  }

  @AfterEach
  fun tearDown() {
    runBlocking {
      connector!!.terminate()
    }
  }

  @Test
  fun startOpensChannelForMessages() {
    assertTrue(!connector!!.started())
    runBlocking {
      connector!!.start()
    }

    assertTrue(connector!!.started())
  }

  @Test
  fun terminateShutdownsConnector() = runBlocking {

    connector!!.start()

    assertTrue(connector!!.started())

    connector!!.terminate()

    assertTrue(!connector!!.started())
  }

  @Test
  fun sendSendsValidDatagram() = runBlocking {
    connector!!.start()

    val destNodeId = Bytes.random(32)

    val receiverAddress = InetSocketAddress(InetAddress.getLocalHost(), 5000)
    val socketChannel = CoroutineDatagramChannel.open()
    socketChannel.bind(receiverAddress)

    val data = RandomMessage.randomData()
    val randomMessage = RandomMessage(UdpMessage.authTag(), data)
    connector!!.send(receiverAddress, randomMessage, destNodeId)
    val buffer = ByteBuffer.allocate(UdpMessage.MAX_UDP_MESSAGE_SIZE)
    socketChannel.receive(buffer) as InetSocketAddress
    buffer.flip()

    val messageContent = Bytes.wrapByteBuffer(buffer).slice(45)
    val message = RandomMessage.create(UdpMessage.authTag(), messageContent)

    assertEquals(message.data, data)

    socketChannel.close()
  }

  @ExperimentalCoroutinesApi
  @Test
  fun attachObserverRegistersListener() = runBlocking {
    val observer = object : MessageObserver {
      var result: Channel<RandomMessage> = Channel()
      override fun observe(message: UdpMessage) {
        if (message is RandomMessage) {
          result.offer(message)
        }
      }
    }
    connector!!.attachObserver(observer)
    connector!!.start()
    assertTrue(observer.result.isEmpty)
    val codec = DefaultPacketCodec(SECP256K1.KeyPair.random(), RoutingTable(Bytes.random(32)))
    val socketChannel = CoroutineDatagramChannel.open()
    val message = RandomMessage()
    val encodedRandomMessage = codec.encode(message, Hash.sha2_256(connector!!.getEnrBytes()))
    val buffer = ByteBuffer.wrap(encodedRandomMessage.content.toArray())
    socketChannel.send(buffer, InetSocketAddress(InetAddress.getLocalHost(), 9090))
    val expectedResult = observer.result.receive()
    assertEquals(expectedResult.data, message.data)
  }

  @Test
  @UseExperimental(ExperimentalCoroutinesApi::class)
  fun detachObserverRemovesListener() = runBlocking {
    val observer = object : MessageObserver {
      var result: Channel<RandomMessage> = Channel()
      override fun observe(message: UdpMessage) {
        if (message is RandomMessage) {
          result.offer(message)
        }
      }
    }
    connector!!.attachObserver(observer)
    connector!!.detachObserver(observer)
    connector!!.start()
    val codec = DefaultPacketCodec(SECP256K1.KeyPair.random(), RoutingTable(Bytes.random(32)))
    val socketChannel = CoroutineDatagramChannel.open()

    val message = RandomMessage()
    val encodedRandomMessage = codec.encode(message, Hash.sha2_256(connector!!.getEnrBytes()))
    val buffer = ByteBuffer.wrap(encodedRandomMessage.content.toArray())
    socketChannel.send(buffer, InetSocketAddress(InetAddress.getLocalHost(), 9090))
    assertTrue(observer.result.isEmpty)
  }
}
