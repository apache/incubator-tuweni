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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.v5.MessageHandler
import org.apache.tuweni.devp2p.v5.PacketCodec
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.dht.RoutingTable
import org.apache.tuweni.devp2p.v5.internal.handler.FindNodeMessageHandler
import org.apache.tuweni.devp2p.v5.internal.handler.NodesMessageHandler
import org.apache.tuweni.devp2p.v5.internal.handler.RandomMessageHandler
import org.apache.tuweni.devp2p.v5.internal.handler.WhoAreYouMessageHandler
import org.apache.tuweni.devp2p.v5.packet.FindNodeMessage
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import org.apache.tuweni.devp2p.v5.misc.HandshakeInitParameters
import org.apache.tuweni.devp2p.v5.packet.NodesMessage
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class DefaultUdpConnector(
  private val bindAddress: InetSocketAddress,
  private val keyPair: SECP256K1.KeyPair,
  private val selfEnr: Bytes,
  private val nodeId: Bytes = Hash.sha2_256(selfEnr),
  private val receiveChannel: CoroutineDatagramChannel = CoroutineDatagramChannel.open(),
  private val sendChannel: CoroutineDatagramChannel = CoroutineDatagramChannel.open(),
  private val packetCodec: PacketCodec = DefaultPacketCodec(keyPair, selfEnr),
  override val coroutineContext: CoroutineContext = Dispatchers.IO
) : UdpConnector, CoroutineScope {

  private val log: Logger = Logger.getLogger(this.javaClass.simpleName)

  private val randomMessageHandler: MessageHandler<RandomMessage> = RandomMessageHandler()
  private val whoAreYouMessageHandler: MessageHandler<WhoAreYouMessage> = WhoAreYouMessageHandler(nodeId)
  private val findNodeMessageHandler: MessageHandler<FindNodeMessage> = FindNodeMessageHandler()
  private val nodesMessageHandler: MessageHandler<NodesMessage> = NodesMessageHandler()

  private val authenticatingPeers: MutableMap<InetSocketAddress, Bytes> = mutableMapOf()

  private val nodesTable: RoutingTable = RoutingTable(selfEnr)

  private lateinit var receiveJob: Job

  override fun start() {
    receiveChannel.bind(bindAddress)

    receiveJob = launch {
      val datagram = ByteBuffer.allocate(UdpMessage.MAX_UDP_MESSAGE_SIZE)
      while (receiveChannel.isOpen) {
        datagram.clear()
        val address = receiveChannel.receive(datagram) as InetSocketAddress
        datagram.flip()
        try {
          processDatagram(datagram, address)
        } catch (ex: Exception) {
          log.warning(ex.message)
        }
      }
    }
  }

  override fun send(
    address: InetSocketAddress,
    message: UdpMessage,
    destNodeId: Bytes,
    handshakeParams: HandshakeInitParameters?
  ) {
    launch {
      val buffer = packetCodec.encode(message, destNodeId, handshakeParams)
      sendChannel.send(ByteBuffer.wrap(buffer.toArray()), address)
    }
  }

  override fun terminate() {
    receiveJob.cancel()
    receiveChannel.close()
    sendChannel.close()
  }

  override fun available(): Boolean = receiveChannel.isOpen

  override fun started(): Boolean = ::receiveJob.isInitialized && available()

  override fun getEnr(): Bytes = selfEnr

  override fun addPendingNodeId(address: InetSocketAddress, nodeId: Bytes) {
    authenticatingPeers[address] = nodeId
  }

  override fun getNodeKeyPair(): SECP256K1.KeyPair = keyPair

  override fun getPendingNodeIdByAddress(address: InetSocketAddress): Bytes {
    val result = authenticatingPeers[address]
      ?: throw IllegalArgumentException("Authenticated peer not found with address ${address.hostName}:${address.port}")
    authenticatingPeers.remove(address)
    return result
  }

  override fun getNodesTable(): RoutingTable = nodesTable

  private fun processDatagram(datagram: ByteBuffer, address: InetSocketAddress) {
    val messageBytes = Bytes.wrapByteBuffer(datagram)
    val decodeResult = packetCodec.decode(messageBytes)
    val message = decodeResult.message
    when (message) {
      is RandomMessage -> randomMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      is WhoAreYouMessage -> whoAreYouMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      is FindNodeMessage -> findNodeMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      is NodesMessage -> nodesMessageHandler.handle(message, address, decodeResult.srcNodeId, this)
      else -> throw IllegalArgumentException("Unexpected message has been received - ${message::class.java.simpleName}")
    }
  }
}
