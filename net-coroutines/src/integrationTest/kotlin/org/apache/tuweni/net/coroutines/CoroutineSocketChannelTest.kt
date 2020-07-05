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
package org.apache.tuweni.net.coroutines

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8

internal class CoroutineSocketChannelTest {

  @Test
  fun shouldSuspendServerSocketChannelWhileAccepting() = runBlocking {
    val listenChannel = CoroutineServerSocketChannel.open()
    Assertions.assertNull(listenChannel.localAddress)
    assertEquals(0, listenChannel.localPort)

    listenChannel.bind(null)
    assertNotNull(listenChannel.localAddress)
    assertTrue(listenChannel.localPort > 0)
    val addr = InetSocketAddress(InetAddress.getLoopbackAddress(), listenChannel.localPort)

    var didBlock = false
    val job = async {
      val serverChannel = listenChannel.accept()
      assertNotNull(serverChannel)
      assertTrue(didBlock)
    }

    Thread.sleep(100)
    didBlock = true

    val clientChannel = CoroutineSocketChannel.open()
    clientChannel.connect(addr)
    job.await()
  }

  @Test
  fun shouldBlockSocketChannelWhileReading() = runBlocking {
    val listenChannel = CoroutineServerSocketChannel.open()
    listenChannel.bind(null)
    val addr =
      InetSocketAddress(InetAddress.getLoopbackAddress(), (listenChannel.localAddress as InetSocketAddress).port)

    val serverJob = async {
      val serverChannel = listenChannel.accept()
      assertNotNull(serverChannel)

      assertTrue(serverChannel.isConnected)
      val dst = ByteBuffer.allocate(1024)
      serverChannel.read(dst)

      dst.flip()
      val chars = ByteArray(dst.limit())
      dst.get(chars, 0, dst.limit())
      assertEquals("testing123456", String(chars, UTF_8))

      serverChannel.write(ByteBuffer.wrap("654321abcdefg".toByteArray(UTF_8)))

      serverChannel.close()
    }

    val clientJob = async {
      val clientChannel = CoroutineSocketChannel.open()
      clientChannel.connect(addr)

      clientChannel.write(ByteBuffer.wrap("testing123456".toByteArray(UTF_8)))

      val dst = ByteBuffer.allocate(1024)
      clientChannel.read(dst)

      dst.flip()
      val chars = ByteArray(dst.limit())
      dst.get(chars, 0, dst.limit())
      assertEquals("654321abcdefg", String(chars, UTF_8))

      clientChannel.close()
    }

    serverJob.await()
    clientJob.await()
  }

  @Test
  fun shouldCloseSocketChannelWhenRemoteClosed() = runBlocking {
    val listenChannel = CoroutineServerSocketChannel.open()
    listenChannel.bind(null)
    val addr =
      InetSocketAddress(InetAddress.getLoopbackAddress(), (listenChannel.localAddress as InetSocketAddress).port)

    val serverJob = async {
      val serverChannel = listenChannel.accept()
      assertNotNull(serverChannel)
      assertTrue(serverChannel.isConnected)

      val dst = ByteBuffer.allocate(1024)
      serverChannel.read(dst)

      dst.flip()
      val chars = ByteArray(dst.limit())
      dst.get(chars, 0, dst.limit())
      assertEquals("testing123456", String(chars, UTF_8))

      serverChannel.close()
    }

    serverJob.start()

    val clientJob = async {
      val clientChannel = CoroutineSocketChannel.open()
      clientChannel.connect(addr)

      clientChannel.write(ByteBuffer.wrap("testing123456".toByteArray(UTF_8)))

      val dst = ByteBuffer.allocate(1024)
      assertTrue(clientChannel.read(dst) < 0)

      clientChannel.close()
    }

    serverJob.await()
    clientJob.await()
  }
}
