/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.net.coroutines

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer

internal class CoroutineDatagramChannelTest {

  @Test
  fun shouldSuspendDatagramChannelWhileReading() = runBlocking {
    val channel = CoroutineDatagramChannel.open()
    assertNull(channel.localAddress)
    assertEquals(0, channel.localPort)

    channel.bind(null)
    assertNotNull(channel.localAddress)
    assertTrue(channel.localPort > 0)

    var didBlock = false
    val dst = ByteBuffer.allocate(10)
    val job = async {
      channel.receive(dst)
      assertTrue(didBlock)
    }

    Thread.sleep(100)
    didBlock = true

    val socket = DatagramSocket()
    socket.connect(InetAddress.getLocalHost(), (channel.localAddress as InetSocketAddress).port)

    val testData = byteArrayOf(1, 2, 3, 4, 5)
    socket.send(DatagramPacket(testData, 5))
    job.await()

    dst.flip()
    assertEquals(5, dst.remaining())
    assertEquals(testData[0], dst.get(0))
    assertEquals(testData[1], dst.get(1))
    assertEquals(testData[2], dst.get(2))
    assertEquals(testData[3], dst.get(3))
    assertEquals(testData[4], dst.get(4))
  }

  @Test
  fun shouldSuspendDatagramChannelWhileWriting() = runBlocking {
    val socket = DatagramSocket()

    val channel = CoroutineDatagramChannel.open()
    val address = InetSocketAddress(InetAddress.getLocalHost(), socket.localPort)

    val testData = byteArrayOf(1, 2, 3, 4, 5)
    val src = ByteBuffer.wrap(testData)
    val job = async {
      channel.send(src, address)
    }

    val resultData = ByteArray(5)
    val packet = DatagramPacket(resultData, resultData.size)
    socket.receive(packet)

    assertEquals(5, packet.length)
    assertEquals(testData[0], resultData[0])
    assertEquals(testData[1], resultData[1])
    assertEquals(testData[2], resultData[2])
    assertEquals(testData[3], resultData[3])
    assertEquals(testData[4], resultData[4])

    job.await()
  }
}
