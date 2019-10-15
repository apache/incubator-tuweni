package org.apache.tuweni.devp2p.v5.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.MessageHandler
import org.apache.tuweni.devp2p.v5.PacketCodec
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.internal.handler.RandomMessageHandler
import org.apache.tuweni.devp2p.v5.internal.handler.WhoAreYouMessageHandler
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

class DefaultUdpConnector(
  private val nodeId: Bytes,
  private val bindAddress: InetSocketAddress,
  private val receiveChannel: CoroutineDatagramChannel = CoroutineDatagramChannel.open(),
  private val sendChannel: CoroutineDatagramChannel = CoroutineDatagramChannel.open(),
  private val packetCodec: PacketCodec = DefaultPacketCodec(nodeId),
  override val coroutineContext: CoroutineContext = Dispatchers.IO
): UdpConnector, CoroutineScope {

  private val randomMessageHandler: MessageHandler<RandomMessage> = RandomMessageHandler()
  private val whoAreYouMessageHandler: MessageHandler<WhoAreYouMessage> = WhoAreYouMessageHandler()

  private val authenticatingPeers: MutableMap<InetSocketAddress, Bytes> = mutableMapOf()

  private lateinit var receiveJob: Job

  override fun start(): Job {
    receiveChannel.bind(bindAddress)

    receiveJob = launch {
      val datagram = ByteBuffer.allocate(MAX_PACKET_SIZE)
      while (receiveChannel.isOpen) {
        datagram.clear()
        val address = receiveChannel.receive(datagram) as InetSocketAddress
        datagram.flip()
        processDatagram(datagram, address)
      }
    }
    return receiveJob
  }

  override fun send(address: InetSocketAddress, message: UdpMessage) {
    launch {
      val buffer = packetCodec.encode(message)
      sendChannel.send(buffer, address)
    }
  }

  override fun terminate() {
    receiveJob.cancel()
    receiveChannel.close()
    sendChannel.close()
  }

  override fun available(): Boolean = receiveChannel.isOpen

  override fun started(): Boolean = ::receiveJob.isInitialized && available()

  override fun addPendingNodeId(address: InetSocketAddress, nodeId: Bytes) {
    authenticatingPeers[address] = nodeId
  }

  override fun getPendingNodeIdByAddress(address: InetSocketAddress): Bytes = authenticatingPeers[address]
    ?: throw IllegalArgumentException("Authenticated peer not found with address ${address.hostName}:${address.port}")

  private fun processDatagram(datagram: ByteBuffer, address: InetSocketAddress) {
    val message = packetCodec.decode(datagram)
    when(message) {
      is RandomMessage -> randomMessageHandler.handle(message, address, this)
      is WhoAreYouMessage -> whoAreYouMessageHandler.handle(message, address, this)
    }
  }

  companion object {
      /** Maximum packet size in network discovery protocol (https://eips.ethereum.org/EIPS/eip-8) */
      private const val MAX_PACKET_SIZE = 1280
  }

}
