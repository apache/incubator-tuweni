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
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey

/**
 * A co-routine based datagram-oriented network channel.
 *
 */
class CoroutineDatagramChannel private constructor(
  private val channel: DatagramChannel,
  private val group: CoroutineChannelGroup
) : CoroutineByteChannel,
  ScatteringCoroutineByteChannel by ScatteringCoroutineByteChannelMixin(channel, group),
  GatheringCoroutineByteChannel by GatheringCoroutineByteChannelMixin(channel, group),
  CoroutineNetworkChannel by CoroutineNetworkChannelMixin(channel) {

  companion object {
    /**
     * Opens a datagram channel.
     *
     * @param selector The selector to use with this channel (defaults to `CommonCoroutineSelector`).
     * @return A new channel.
     * @throws IOException If an I/O error occurs.
     */
    fun open(group: CoroutineChannelGroup = CommonCoroutineGroup): CoroutineDatagramChannel {
      val channel = DatagramChannel.open()
      channel.configureBlocking(false)
      return CoroutineDatagramChannel(channel, group)
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

  override fun bind(local: SocketAddress?): CoroutineDatagramChannel {
    channel.bind(local)
    return this
  }

  /**
   * Connect this channel.
   *
   * The channel will be configured so that it only receives datagrams from, and sends datagrams to, the given
   * remote peer address. Once connected, datagrams may not be received from or sent to any other address. A datagram
   * channel remains connected until it is disconnected or closed.
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
  fun connect(remote: SocketAddress): CoroutineDatagramChannel {
    channel.connect(remote)
    return this
  }

  /**
   * Disconnects this channel.
   */
  fun disconnect(): CoroutineDatagramChannel {
    channel.disconnect()
    return this
  }

  /**
   * Receives a datagram via this channel.
   *
   * @param dst The buffer into which the datagram is to be transferred.
   * @return The datagram's source address.
   * @throws ClosedChannelException If the channel is closed.
   * @throws AsynchronousCloseException If another thread closes this channel while the receive operation is in
   *   progress.
   * @throws ClosedByInterruptException If another thread interrupts the current thread while the receive operation is
   *   in progress, thereby closing the channel and setting the current thread's interrupt status.
   * @throws IOException If some other I/O error occurs.
   */
  suspend fun receive(dst: ByteBuffer): SocketAddress {
    while (true) {
      val address = channel.receive(dst)
      if (address != null) {
        return address
      }
      // slow path
      group.select(channel, SelectionKey.OP_READ)
    }
  }

  /**
   * Receives a datagram via this channel, if one is immediately available.
   *
   * @param dst The buffer into which the datagram is to be transferred.
   * @return The datagram's source address, or `null` if no datagram was available to be received.
   * @throws ClosedChannelException If the channel is closed.
   * @throws AsynchronousCloseException If another thread closes this channel while the receive operation is in
   *   progress.
   * @throws ClosedByInterruptException If another thread interrupts the current thread while the receive operation is
   *   in progress, thereby closing the channel and setting the current thread's interrupt status.
   * @throws IOException If some other I/O error occurs.
   */
  fun tryReceive(dst: ByteBuffer): SocketAddress? = channel.receive(dst)

  /**
   * Sends a datagram via this channel.
   *
   * @param src The buffer containing the datagram to be sent.
   * @param target The address to which the datagram is to be sent.
   * @return The number of bytes sent.
   * @throws ClosedChannelException If the channel is closed.
   * @throws AsynchronousCloseException If another thread closes this channel while the send operation is in progress.
   * @throws ClosedByInterruptException If another thread interrupts the current thread while the send operation is
   *   in progress, thereby closing the channel and setting the current thread's interrupt status.
   * @throws IOException If some other I/O error occurs.
   */
  suspend fun send(src: ByteBuffer, target: SocketAddress): Int {
    while (true) {
      val n = channel.send(src, target)
      if (n != 0 || src.remaining() == 0) {
        return n
      }
      // slow path
      group.select(channel, SelectionKey.OP_WRITE)
    }
  }

  /**
   * Sends a datagram via this channel, if it can be sent immediately.
   *
   * @param src The buffer containing the datagram to be sent.
   * @param target The address to which the datagram is to be sent.
   * @return The number of bytes sent, which will be zero if the channel was not ready to send.
   * @throws ClosedChannelException If the channel is closed.
   * @throws AsynchronousCloseException If another thread closes this channel while the send operation is in progress.
   * @throws ClosedByInterruptException If another thread interrupts the current thread while the send operation is
   *   in progress, thereby closing the channel and setting the current thread's interrupt status.
   * @throws IOException If some other I/O error occurs.
   */
  fun trySend(src: ByteBuffer, target: SocketAddress): Int = channel.send(src, target)
}
