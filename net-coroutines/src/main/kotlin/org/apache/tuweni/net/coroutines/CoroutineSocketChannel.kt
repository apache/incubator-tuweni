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

import java.io.IOException
import java.net.SocketAddress
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.NotYetConnectedException
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

/**
 * A co-routine based stream-oriented network channel.
 *
 * @author Chris Leishman - https://cleishm.github.io/
 */
class CoroutineSocketChannel internal constructor(
  private val channel: SocketChannel,
  private val group: CoroutineChannelGroup
) : CoroutineByteChannel,
  ScatteringCoroutineByteChannel by ScatteringCoroutineByteChannelMixin(channel, group),
  GatheringCoroutineByteChannel by GatheringCoroutineByteChannelMixin(channel, group),
  CoroutineNetworkChannel by CoroutineNetworkChannelMixin(channel) {

  companion object {
    /**
     * Opens a socket channel.
     *
     * @param selector The selector to use with this channel (defaults to `CommonCoroutineSelector`).
     * @return A new channel.
     * @throws IOException If an I/O error occurs.
     */
    fun open(group: CoroutineChannelGroup = CommonCoroutineGroup): CoroutineSocketChannel {
      val channel = SocketChannel.open()
      channel.configureBlocking(false)
      return CoroutineSocketChannel(channel, group)
    }
  }

  init {
    group.register(this)
  }

  override fun close() {
    group.deRegister(this)
    channel.close()
  }

  /**
   * Indicates whether this channel is connected.
   */
  val isConnected: Boolean
    get() = channel.isConnected

  /**
   * Get the remote address to which this channel is connected.
   */
  val remoteAddress: SocketAddress
    get() = channel.remoteAddress

  override fun bind(local: SocketAddress?): CoroutineSocketChannel {
    channel.bind(local)
    return this
  }

  /**
   * Connect this channel.
   *
   * @param remote The remote address to which this channel is to be connected.
   * @return This channel.
   * @throws ClosedChannelException If the channel is closed.
   * @throws AsynchronousCloseException If another thread closes this channel while the connect operation is in
   *   progress.
   * @throws ClosedByInterruptException If another thread interrupts the current thread while the connect operation is
   *   in progress, thereby closing the channel and setting the current thread's interrupt status.
   * @throws IOException If some other I/O error occurs.
   */
  suspend fun connect(remote: SocketAddress): CoroutineSocketChannel {
    if (!channel.connect(remote)) {
      // slow path
      do {
        group.select(channel, SelectionKey.OP_CONNECT)
      } while (!channel.finishConnect())
    }
    return this
  }

  /**
   * Shutdown the connection for reading without closing the channel.
   *
   * @return This channel.
   * @throws NotYetConnectedException If this channel is not yet connected.
   * @throws ClosedChannelException If the channel is closed.
   * @throws IOException If some other I/O error occurs.
   */
  fun shutdownInput(): CoroutineSocketChannel {
    channel.shutdownInput()
    return this
  }

  /**
   * Shutdown the connection for writing without closing the channel.
   *
   * @return This channel.
   * @throws NotYetConnectedException If this channel is not yet connected.
   * @throws ClosedChannelException If the channel is closed.
   * @throws IOException If some other I/O error occurs.
   */
  fun shutdownOutput(): CoroutineSocketChannel {
    channel.shutdownOutput()
    return this
  }
}
