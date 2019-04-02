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
package org.apache.tuweni.net.coroutines

import java.io.IOException
import java.net.SocketAddress
import java.nio.channels.AlreadyBoundException
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.NotYetBoundException
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.UnsupportedAddressTypeException

/**
 * A co-routine based network channel for stream-oriented connection listening.
 *
 * @author Chris Leishman - https://cleishm.github.io/
 */
class CoroutineServerSocketChannel private constructor(
  private val channel: ServerSocketChannel,
  private val group: CoroutineChannelGroup
) : CoroutineNetworkChannel by CoroutineNetworkChannelMixin(channel) {

  companion object {
    /**
     * Opens a server-socket channel.
     *
     * @param selector The selector to use with this channel (defaults to `CommonCoroutineSelector`).
     * @return A new channel.
     * @throws IOException If an I/O error occurs.
     */
    fun open(group: CoroutineChannelGroup = CommonCoroutineGroup): CoroutineServerSocketChannel {
      val channel = ServerSocketChannel.open()
      channel.configureBlocking(false)
      return CoroutineServerSocketChannel(channel, group)
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
   * Binds the channel's socket to a local address and configures the socket to listen for connections.
   *
   * @param local The local address to bind the socket, or null to bind to an automatically assigned socket address.
   * @return This channel
   * @throws AlreadyBoundException If the socket is already bound.
   * @throws UnsupportedAddressTypeException If the type of the given address is not supported.
   * @throws ClosedChannelException If the channel is closed.
   * @throws IOException If an I/O error occurs.
   */
  override fun bind(local: SocketAddress?): CoroutineServerSocketChannel = bind(local, 0)

  /**
   * Binds the channel's socket to a local address and configures the socket to listen for connections.
   *
   * @param local The local address to bind the socket, or null to bind to an automatically assigned socket address.
   * @param backlog The maximum number of pending connections.
   * @return This channel
   * @throws AlreadyBoundException If the socket is already bound.
   * @throws UnsupportedAddressTypeException If the type of the given address is not supported.
   * @throws ClosedChannelException If the channel is closed.
   * @throws IOException If an I/O error occurs.
   */
  fun bind(local: SocketAddress?, backlog: Int): CoroutineServerSocketChannel {
    channel.bind(local, backlog)
    return this
  }

  /**
   * Accepts a connection made to this channel's socket.
   *
   * @return The socket channel for the new connection.
   * @throws ClosedChannelException If the channel is closed.
   * @throws AsynchronousCloseException If another thread closes this channel while the accept operation is in
   *   progress.
   * @throws ClosedByInterruptException If another thread interrupts the current thread while the accept operation is
   *   in progress, thereby closing the channel and setting the current thread's interrupt status.
   * @throws NotYetBoundException If this channel's socket has not yet been bound.
   * @throws IOException If an I/O error occurs.
   */
  suspend fun accept(): CoroutineSocketChannel {
    while (true) {
      val s = channel.accept()
      if (s != null) {
        s.configureBlocking(false)
        return CoroutineSocketChannel(s, group)
      }
      // slow path
      group.select(channel, SelectionKey.OP_ACCEPT)
    }
  }
}
