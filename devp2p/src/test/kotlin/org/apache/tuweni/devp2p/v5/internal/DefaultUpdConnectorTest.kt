package org.apache.tuweni.devp2p.v5.internal

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class DefaultUpdConnectorTest {

  private val nodeId: Bytes = Bytes.fromHexString("0x98EB6D611291FA21F6169BFF382B9369C33D997FE4DC93410987E27796360640")
  private val address: InetSocketAddress = InetSocketAddress(9090)

  private val srcId: Bytes = Bytes.fromHexString("0xA5CFE10E0EFC543CBE023560B2900E2243D798FAFD0EA46267DDD20D283CE13C")
  private val authTag: Bytes = UdpMessage.authTag()
  private val message: RandomMessage = RandomMessage(nodeId, srcId, authTag)

  private var connector: UdpConnector = DefaultUdpConnector(nodeId, address)

  @BeforeEach
  fun setUp() {
    connector = DefaultUdpConnector(nodeId, address)
  }

  @AfterEach
  fun tearDown() {
    if (connector.started()) {
      connector.terminate()
    }
  }

  @Test
  fun startOpensChannelForMessages() {
    connector.start()

    assert(connector.available())
  }

  @Test
  fun terminateShutdownsConnector() {
    connector.start()

    assert(connector.available())

    connector.terminate()

    assert(!connector.available())
  }

  @Test
  fun sendSendsValidDatagram() {
    connector.start()

    val receiverAddress = InetSocketAddress(InetAddress.getLocalHost(), 9091)
    val socketChannel = CoroutineDatagramChannel.open()
    socketChannel.bind(receiverAddress)

    runBlocking {
      connector.send(receiverAddress, message)
      val buffer = ByteBuffer.allocate(UdpMessage.MAX_UDP_MESSAGE_SIZE)
      socketChannel.receive(buffer) as InetSocketAddress

      val messageContent = Bytes.wrapByteBuffer(buffer).slice(UdpMessage.TAG_LENGTH)
      val message = RandomMessage.create(messageContent, nodeId, srcId)

      assert(message.authTag == authTag)
    }
  }

}
