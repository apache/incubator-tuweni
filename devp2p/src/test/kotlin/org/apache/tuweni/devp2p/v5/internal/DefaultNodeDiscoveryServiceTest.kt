package org.apache.tuweni.devp2p.v5.internal

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import org.apache.tuweni.io.Base64URLSafe
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.time.Instant

@ExtendWith(BouncyCastleExtension::class)
class DefaultNodeDiscoveryServiceTest {

  @Test
  fun pingTest() {
    val senderAddress = InetSocketAddress(InetAddress.getLocalHost(), 9091)
    val senderKey = SECP256K1.KeyPair.random()
    val senderEnrSeq = Instant.now().toEpochMilli()
    val senderEnr = EthereumNodeRecord.toRLP(
      senderKey,
      senderEnrSeq,
      emptyMap(),
      senderAddress.address,
      null,
      senderAddress.port
    )
    val senderNodeId = Hash.sha2_256(senderEnr)

    val receiverAddress = InetSocketAddress(InetAddress.getLocalHost(), 9090)
    val receiverKey = SECP256K1.KeyPair.random()
    val receiverEnrSeq = Instant.now().toEpochMilli()
    val receiverEnr = EthereumNodeRecord.toRLP(
      receiverKey,
      receiverEnrSeq,
      emptyMap(),
      receiverAddress.address,
      null,
      receiverAddress.port
    )
    val receiverNodeId = Hash.sha2_256(receiverEnr)

    val discoveryService = DefaultNodeDiscoveryService(receiverKey, 9090, enrSeq = receiverEnrSeq, selfENR = receiverEnr)
    discoveryService.start()

    val channel = CoroutineDatagramChannel.open()
    channel.bind(senderAddress)
    runBlocking {
      val message = RandomMessage(src = senderNodeId, dest = receiverNodeId)
      println("Sending ${message.authTag}")
      val encoded = message.encode()
      channel.send(encoded, receiverAddress)

      val buffer = ByteBuffer.allocate(1280)
      channel.receive(buffer)
      buffer.flip()
      val bytes = Bytes.wrapByteBuffer(buffer)
      val content = bytes.slice(UdpMessage.TAG_LENGTH)
      val whoAreYou = WhoAreYouMessage.create(content, senderNodeId, senderNodeId)
      println("Received ${whoAreYou.idNonce} message")

      discoveryService.terminate()
    }

  }

  @Test
  fun whoAreYouTest() {
    val senderAddress = InetSocketAddress(InetAddress.getLocalHost(), 9091)
    val senderKey = SECP256K1.KeyPair.random()
    val senderEnrSeq = Instant.now().toEpochMilli()
    val senderEnr = EthereumNodeRecord.toRLP(
      senderKey,
      senderEnrSeq,
      emptyMap(),
      senderAddress.address,
      udp = senderAddress.port
    )
    val senderNodeId = Hash.sha2_256(senderEnr)

    val receiverAddress = InetSocketAddress(InetAddress.getLocalHost(), 9090)
    val receiverKey = SECP256K1.KeyPair.random()
    val receiverEnrSeq = Instant.now().toEpochMilli()
    val receiverEnr = EthereumNodeRecord.toRLP(
      receiverKey,
      receiverEnrSeq,
      emptyMap(),
      receiverAddress.address,
      udp = receiverAddress.port
    )
    val receiverNodeId = Hash.sha2_256(receiverEnr)

    val channel = CoroutineDatagramChannel.open()
    channel.bind(senderAddress)

    val bootList = listOf("enr:${Base64URLSafe.encode(senderEnr)}")
    val discoveryService = DefaultNodeDiscoveryService(receiverKey, 9090, enrSeq = receiverEnrSeq, selfENR = receiverEnr, bootstrapENRList = bootList)
    discoveryService.start()

    runBlocking {
      val buffer = ByteBuffer.allocate(1280)
      channel.receive(buffer)
      val message = WhoAreYouMessage(senderNodeId, receiverNodeId, UdpMessage.authTag())
      channel.send(message.encode(), receiverAddress)
      buffer.clear()
      channel.receive(buffer)
    }
  }

}
