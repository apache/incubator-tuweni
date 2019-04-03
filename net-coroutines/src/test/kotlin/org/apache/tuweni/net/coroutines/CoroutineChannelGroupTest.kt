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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.channels.ShutdownChannelGroupException

internal class CoroutineChannelGroupTest {

  @Test
  fun shouldImmediatelyTerminateEmptyGroup() {
    val group = CoroutineChannelGroup.open()
    group.shutdown()
    assertTrue(group.isShutdown)
    assertTrue(group.isTerminated)
  }

  @Test
  fun shouldTerminateGroupWhenAllChannelsClosed() {
    val group = CoroutineChannelGroup.open()
    val channel = CoroutineServerSocketChannel.open(group)
    group.shutdown()
    assertTrue(group.isShutdown)
    assertFalse(group.isTerminated)
    channel.close()
    assertTrue(group.isTerminated)
  }

  @Test
  fun shouldNotAllowNewChannelsAfterShutdown() {
    val group = CoroutineChannelGroup.open()
    CoroutineServerSocketChannel.open(group)
    group.shutdown()
    assertThrows<ShutdownChannelGroupException> { CoroutineServerSocketChannel.open(group) }
  }

  @Test
  fun shouldTerminateWhenAllChannelAreClosed() = runBlocking {
    val group = CoroutineChannelGroup.open()
    val channel = CoroutineServerSocketChannel.open(group)
    var didBlock = false
    val task = async {
      group.awaitTermination()
      assertTrue(didBlock)
    }
    group.shutdown()
    Thread.sleep(100)
    assertFalse(group.isTerminated)
    didBlock = true
    channel.close()
    task.await()
    assertTrue(group.isTerminated)
  }

  @Test
  fun shutdownNowShouldCloseChannels() {
    val group = CoroutineChannelGroup.open()
    val channel = CoroutineServerSocketChannel.open(group)
    assertTrue(channel.isOpen)
    group.shutdownNow()
    assertFalse(channel.isOpen)
    assertTrue(group.isTerminated)
  }

  @Test
  fun shutdownNowShouldResumeCoroutinesAwaitingTermination() = runBlocking {
    val group = CoroutineChannelGroup.open()
    val channel = CoroutineServerSocketChannel.open(group)
    var didBlock = false
    val task = async {
      group.awaitTermination()
      assertTrue(didBlock)
    }
    Thread.sleep(100)
    didBlock = true
    group.shutdownNow()
    assertTrue(group.isTerminated)
    task.await()
    assertFalse(channel.isOpen)
  }

  @Test
  fun awaitTerminationShouldReturnImmediatelyForTerminatedGroup() {
    val group = CoroutineChannelGroup.open()
    group.shutdown()
    runBlocking { group.awaitTermination() }
  }
}
