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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.ClosedSelectorException
import java.nio.channels.Pipe
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

internal class CoroutineSelectorTest {

  @Test
  fun shouldRequireNonBlockingChannel() {
    val pipe = Pipe.open()
    val selector = CoroutineSelector.open()

    assertThrows<IllegalArgumentException> {
      runBlocking {
        selector.select(pipe.source(), SelectionKey.OP_READ)
      }
    }
  }

  @Test
  fun shouldThrowWhenAccessingClosedSelector() {
    val pipe = Pipe.open()
    pipe.source().configureBlocking(false)
    val selector = CoroutineSelector.open()
    selector.close()

    assertThrows<ClosedSelectorException> {
      runBlocking {
        selector.select(pipe.source(), SelectionKey.OP_READ)
      }
    }
    assertThrows<ClosedSelectorException> {
      runBlocking {
        selector.cancelSelections(pipe.source())
      }
    }
    assertThrows<ClosedSelectorException> {
      selector.wakeup()
    }
  }

  @Test
  fun closeNowOnEmptySelectorShouldReturnImmediately() {
    val selector = CoroutineSelector.open()
    runBlocking { selector.closeNow() }
  }

  @Test
  fun shouldSuspendUntilReady() = runBlocking {
    val pipe1 = Pipe.open()
    pipe1.source().configureBlocking(false)
    val pipe2 = Pipe.open()
    pipe2.source().configureBlocking(false)
    val selector = CoroutineSelector.open()

    var ok1 = false
    var ok2 = false

    val job1 = async {
      selector.select(pipe1.source(), SelectionKey.OP_READ)
      assertTrue(ok1, "failed to suspend")
      assertFalse(ok2)
    }

    Thread.sleep(100)

    val job2 = async {
      selector.select(pipe2.source(), SelectionKey.OP_READ)
      assertTrue(ok2, "failed to suspend")
    }

    ok1 = true
    pipe1.sink().write(ByteBuffer.wrap(byteArrayOf(1)))
    job1.await()
    ok2 = true
    pipe2.sink().write(ByteBuffer.wrap(byteArrayOf(1)))
    job2.await()
  }

  @Test
  fun shouldAwakenMultiple() = runBlocking {
    val server = ServerSocketChannel.open()
    server.bind(InetSocketAddress(0))
    val client = SocketChannel.open()
    client.connect(server.localAddress)
    client.configureBlocking(false)
    val selector = CoroutineSelector.open()

    val job1 = async {
      selector.select(client, SelectionKey.OP_READ)
    }
    val job2 = async {
      selector.select(client, SelectionKey.OP_WRITE)
    }

    Thread.sleep(100)
    server.accept().write(ByteBuffer.wrap(byteArrayOf(1)))

    job2.await()
    job1.await()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun shouldCancelOutstanding() = runBlocking {
    val server = ServerSocketChannel.open()
    server.bind(InetSocketAddress(0))
    val client = SocketChannel.open()
    client.connect(server.localAddress)
    server.accept()

    client.configureBlocking(false)
    server.configureBlocking(false)
    val selector = CoroutineSelector.open()

    assertFalse(selector.cancelSelections(client))

    val job1 = async(start = CoroutineStart.UNDISPATCHED) {
      selector.select(client, SelectionKey.OP_READ)
    }
    val job2 = async(start = CoroutineStart.UNDISPATCHED) {
      selector.select(client, SelectionKey.OP_WRITE)
    }
    val job3 = async(start = CoroutineStart.UNDISPATCHED) {
      selector.select(server, SelectionKey.OP_ACCEPT)
    }

    job2.await()
    Thread.sleep(100)

    selector.cancelSelections(client)

    assertThrows<CancellationException> { runBlocking { job1.await() } }
    assertFalse(job3.isCompleted)
    SocketChannel.open().connect(server.localAddress)
    job3.await()
  }

  @Test
  fun shouldThrowWhenSelectingClosedChannel() {
    val pipe = Pipe.open()
    pipe.source().configureBlocking(false)
    val selector = CoroutineSelector.open()

    pipe.source().close()
    assertThrows<ClosedChannelException> {
      runBlocking {
        selector.select(pipe.source(), SelectionKey.OP_READ)
      }
    }
  }

  @Test
  fun shouldAwakenOnChannelClose() = runBlocking {
    val pipe1 = Pipe.open()
    pipe1.source().configureBlocking(false)
    val pipe2 = Pipe.open()
    pipe2.source().configureBlocking(false)
    val selector = CoroutineSelector.open()

    val job1 = async {
      selector.select(pipe1.source(), SelectionKey.OP_READ)
      fail<Unit>("should not be reached")
    }

    val job2 = async {
      selector.select(pipe2.source(), SelectionKey.OP_READ)
      fail<Unit>("should not be reached")
    }

    Thread.sleep(100)
    pipe1.source().close()
    assertThrows<ClosedChannelException> { runBlocking { job1.await() } }

    Thread.sleep(100)
    pipe2.source().close()
    assertThrows<ClosedChannelException> { runBlocking { job2.await() } }
  }

  @Test
  fun shouldAwakenOnSelectorClose() = runBlocking {
    val pipe1 = Pipe.open()
    pipe1.source().configureBlocking(false)
    val pipe2 = Pipe.open()
    pipe2.source().configureBlocking(false)
    val selector = CoroutineSelector.open()

    val job1 = async {
      selector.select(pipe1.source(), SelectionKey.OP_READ)
      fail<Unit>("should not be reached")
    }

    val job2 = async {
      selector.select(pipe2.source(), SelectionKey.OP_READ)
      fail<Unit>("should not be reached")
    }

    Thread.sleep(100)
    selector.close()
    assertThrows<ClosedSelectorException> { runBlocking { job1.await() } }
    assertThrows<ClosedSelectorException> { runBlocking { job2.await() } }
  }
}
