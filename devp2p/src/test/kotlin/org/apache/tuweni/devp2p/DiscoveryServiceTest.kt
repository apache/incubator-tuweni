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
package org.apache.tuweni.devp2p

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.aSocket
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.network.port
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readAvailable
import io.ktor.utils.io.core.readFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.nio.ByteBuffer

@Timeout(10)
@ExtendWith(BouncyCastleExtension::class)
@OptIn(KtorExperimentalAPI::class)
internal class DiscoveryServiceTest {

  @Test
  fun shouldStartAndShutdownService() {
    val discoveryService = DiscoveryService.open(
      host = "127.0.0.1",
      keyPair = SECP256K1.KeyPair.random()
    )
    assertFalse(discoveryService.isShutdown)
    assertFalse(discoveryService.isTerminated)
    discoveryService.shutdown()
    assertTrue(discoveryService.isShutdown)

    runBlocking {
      discoveryService.awaitTermination()
    }
    assertTrue(discoveryService.isTerminated)
  }

  @Test
  fun shouldRespondToPingAndRecordEndpoint() = runBlocking {
    val peerRepository = EphemeralPeerRepository()
    val discoveryService = DiscoveryService.open(
      host = "127.0.0.1",
      keyPair = SECP256K1.KeyPair.random(),
      peerRepository = peerRepository
    )
    val address = InetSocketAddress(InetAddress.getLoopbackAddress(), discoveryService.localPort)
    val clientKeyPair = SECP256K1.KeyPair.random()
    val client = aSocket(ActorSelectorManager(Dispatchers.IO)).udp().bind(InetSocketAddress(0))
    val clientEndpoint = Endpoint("192.168.1.1", 5678, 7654)
    val ping = PingPacket.create(
      clientKeyPair,
      System.currentTimeMillis(),
      clientEndpoint,
      Endpoint(address),
      null
    )
    val bytes = ByteBuffer.allocate(Packet.MAX_SIZE)
    ping.encodeTo(bytes)
    bytes.flip()
    client.send(Datagram(ByteReadPacket(bytes), address))
    val datagram = client.receive()
    val buffer = ByteBuffer.allocate(datagram.packet.remaining.toInt())
    datagram.packet.readFully(buffer)
    val pong = Packet.decodeFrom(buffer) as PongPacket
    assertEquals(discoveryService.nodeId, pong.nodeId)
    assertEquals(ping.hash, pong.pingHash)

    val peer = peerRepository.get(URI("enode://" + clientKeyPair.publicKey().toHexString() + "@127.0.0.1:5678"))
    assertNotNull(peer.endpoint)
    assertEquals(clientEndpoint.tcpPort, peer.endpoint.tcpPort)
    discoveryService.shutdownNow()
    client.close()
  }

  @Test
  fun shouldPingBootstrapNodeAndValidate() = runBlocking {
    val bootstrapKeyPair = SECP256K1.KeyPair.random()
    val bootstrapClient =
      aSocket(ActorSelectorManager(Dispatchers.Default)).udp().bind(InetSocketAddress("127.0.0.1", 0))

    val serviceKeyPair = SECP256K1.KeyPair.random()
    val peerRepository = EphemeralPeerRepository()
    val routingTable = DevP2PPeerRoutingTable(serviceKeyPair.publicKey())
    val discoveryService = DiscoveryService.open(
      host = "127.0.0.1",
      keyPair = serviceKeyPair,
      bootstrapURIs = listOf(
        URI("enode://" + bootstrapKeyPair.publicKey().toHexString() + "@127.0.0.1:" + bootstrapClient.localAddress.port)
      ),
      peerRepository = peerRepository,
      routingTable = routingTable
    )
    val address = InetSocketAddress(InetAddress.getLoopbackAddress(), discoveryService.localPort)

    val datagram = bootstrapClient.receive()
    val buffer = ByteBuffer.allocate(datagram.packet.remaining.toInt())
    datagram.packet.readFully(buffer)
    val ping = Packet.decodeFrom(buffer) as PingPacket
    assertEquals(discoveryService.nodeId, ping.nodeId)
    assertEquals(
      ping.to,
      Endpoint("127.0.0.1", bootstrapClient.localAddress.port, bootstrapClient.localAddress.port)
    )
    assertEquals(discoveryService.localPort, ping.from.udpPort)
    assertNull(ping.from.tcpPort)

    val pong = PongPacket.create(
      bootstrapKeyPair,
      System.currentTimeMillis(),
      ping.from,
      ping.hash,
      null
    )
    val bytes = ByteBuffer.allocate(Packet.MAX_SIZE)
    pong.encodeTo(bytes)
    bytes.flip()
    bootstrapClient.send(Datagram(ByteReadPacket(bytes), address))

    val findNodesDatagram = bootstrapClient.receive()
    val b = ByteBuffer.allocate(Packet.MAX_SIZE)
    findNodesDatagram.packet.readAvailable(b)
    b.flip()
    val findNodes = Packet.decodeFrom(b) as FindNodePacket
    assertEquals(discoveryService.nodeId, findNodes.nodeId)
    assertEquals(discoveryService.nodeId, findNodes.target)

    val bootstrapPeer =
      peerRepository.get(
        URI(
          "enode://" + bootstrapKeyPair.publicKey().toHexString() +
            "@127.0.0.1:" + bootstrapClient.localAddress.port
        )
      )
    assertNotNull(bootstrapPeer.lastVerified)
    assertNotNull(bootstrapPeer.endpoint)
    assertEquals(bootstrapClient.localAddress.port, bootstrapPeer.endpoint.tcpPort)

    assertTrue(routingTable.contains(bootstrapPeer))

    discoveryService.shutdownNow()
  }

  @Test
  fun shouldIgnoreBootstrapNodeRespondingWithDifferentNodeId() = runBlocking {
    val bootstrapKeyPair = SECP256K1.KeyPair.random()
    val bootstrapClient =
      aSocket(ActorSelectorManager(Dispatchers.Default)).udp().bind(InetSocketAddress("127.0.0.1", 0))

    val serviceKeyPair = SECP256K1.KeyPair.random()
    val peerRepository = EphemeralPeerRepository()
    val routingTable = DevP2PPeerRoutingTable(serviceKeyPair.publicKey())
    val discoveryService = DiscoveryService.open(
      host = "127.0.0.1",
      keyPair = serviceKeyPair,
      bootstrapURIs = listOf(
        URI("enode://" + bootstrapKeyPair.publicKey().toHexString() + "@127.0.0.1:" + bootstrapClient.localAddress.port)
      ),
      peerRepository = peerRepository,
      routingTable = routingTable
    )
    val address = InetSocketAddress(InetAddress.getLoopbackAddress(), discoveryService.localPort)

    val datagram = bootstrapClient.receive()
    val buffer = ByteBuffer.allocate(datagram.packet.remaining.toInt())
    datagram.packet.readFully(buffer)
    val ping = Packet.decodeFrom(buffer) as PingPacket
    assertEquals(discoveryService.nodeId, ping.nodeId)
    assertEquals(
      ping.to,
      Endpoint("127.0.0.1", bootstrapClient.localAddress.port, bootstrapClient.localAddress.port)
    )
    assertEquals(discoveryService.localPort, ping.from.udpPort)
    assertNull(ping.from.tcpPort)

    val pong = PongPacket.create(
      SECP256K1.KeyPair.random(),
      System.currentTimeMillis(),
      ping.from,
      ping.hash,
      null
    )

    val bytes = ByteBuffer.allocate(Packet.MAX_SIZE)
    pong.encodeTo(bytes)
    bytes.flip()
    bootstrapClient.send(Datagram(ByteReadPacket(bytes), address))

    delay(1000)
    val bootstrapPeer =
      peerRepository.get(
        URI(
          "enode://" + bootstrapKeyPair.publicKey().toHexString() +
            "@127.0.0.1:" + bootstrapClient.localAddress.port
        )
      )
    assertNull(bootstrapPeer.lastVerified)
    assertFalse(routingTable.contains(bootstrapPeer))

    discoveryService.shutdownNow()
  }

  @Test
  fun shouldPingBootstrapNodeWithAdvertisedAddress() = runBlocking {
    val bootstrapKeyPair = SECP256K1.KeyPair.random()
    val bootstrapClient =
      aSocket(ActorSelectorManager(Dispatchers.Default)).udp().bind(InetSocketAddress("localhost", 0))

    val discoveryService = DiscoveryService.open(
      host = "127.0.0.1",
      keyPair = SECP256K1.KeyPair.random(),
      bootstrapURIs = listOf(
        URI(
          "enode://" + bootstrapKeyPair.publicKey().bytes()
            .toHexString() + "@127.0.0.1:" + bootstrapClient.localAddress.port
        )
      ),
      advertiseAddress = InetAddress.getByName("192.168.66.55"),
      advertiseUdpPort = 3836,
      advertiseTcpPort = 8765
    )

    val datagram = bootstrapClient.receive()
    val buffer = ByteBuffer.allocate(datagram.packet.remaining.toInt())
    datagram.packet.readFully(buffer)
    val ping = Packet.decodeFrom(buffer) as PingPacket
    assertEquals(discoveryService.nodeId, ping.nodeId)
    assertEquals(Endpoint("127.0.0.1", bootstrapClient.localAddress.port, bootstrapClient.localAddress.port), ping.to)
    assertEquals(Endpoint("192.168.66.55", 3836, 8765), ping.from)

    discoveryService.shutdownNow()
  }

  @Test
  fun shouldRetryPingsToBootstrapNodes() = runBlocking {
    val bootstrapKeyPair = SECP256K1.KeyPair.random()
    val bootstrapClient =
      aSocket(ActorSelectorManager(Dispatchers.Default)).udp().bind(InetSocketAddress("localhost", 0))

    val discoveryService = DiscoveryService.open(
      host = "127.0.0.1",
      keyPair = SECP256K1.KeyPair.random(),
      bootstrapURIs = listOf(
        URI(
          "enode://" + bootstrapKeyPair.publicKey().bytes()
            .toHexString() + "@127.0.0.1:" + bootstrapClient.localAddress.port
        )
      )
    )
    val datagram1 = bootstrapClient.receive()
    val buffer1 = ByteBuffer.allocate(datagram1.packet.remaining.toInt())
    datagram1.packet.readFully(buffer1)
    val ping1 = Packet.decodeFrom(buffer1) as PingPacket
    assertEquals(discoveryService.nodeId, ping1.nodeId)
    assertEquals(Endpoint("127.0.0.1", bootstrapClient.localAddress.port, bootstrapClient.localAddress.port), ping1.to)
    val datagram2 = bootstrapClient.receive()
    val buffer2 = ByteBuffer.allocate(datagram2.packet.remaining.toInt())
    datagram2.packet.readFully(buffer2)
    val ping2 = Packet.decodeFrom(buffer2) as PingPacket
    assertEquals(discoveryService.nodeId, ping2.nodeId)
    assertEquals(Endpoint("127.0.0.1", bootstrapClient.localAddress.port, bootstrapClient.localAddress.port), ping2.to)
    val datagram3 = bootstrapClient.receive()
    val buffer3 = ByteBuffer.allocate(datagram3.packet.remaining.toInt())
    datagram3.packet.readFully(buffer3)
    val ping3 = Packet.decodeFrom(buffer3) as PingPacket
    assertEquals(discoveryService.nodeId, ping3.nodeId)
    assertEquals(Endpoint("127.0.0.1", bootstrapClient.localAddress.port, bootstrapClient.localAddress.port), ping3.to)
    discoveryService.shutdownNow()
    discoveryService.awaitTermination()
    bootstrapClient.close()
  }

  @Test
  fun shouldRequirePingPongBeforeRespondingToFindNodesFromUnverifiedPeer() = runBlocking {
    val peerRepository = EphemeralPeerRepository()
    val discoveryService = DiscoveryService.open(
      host = "127.0.0.1",
      keyPair = SECP256K1.KeyPair.random(),
      peerRepository = peerRepository
    )
    val address = InetSocketAddress(InetAddress.getLoopbackAddress(), discoveryService.localPort)

    val clientKeyPair = SECP256K1.KeyPair.random()
    val client = aSocket(ActorSelectorManager(Dispatchers.IO)).udp().bind(InetSocketAddress(0))
    val findNodes =
      FindNodePacket.create(
        clientKeyPair,
        System.currentTimeMillis(),
        SECP256K1.KeyPair.random().publicKey()
      )
    val bytes = ByteBuffer.allocate(Packet.MAX_SIZE)
    findNodes.encodeTo(bytes)
    bytes.flip()
    client.send(Datagram(ByteReadPacket(bytes), address))

    val datagram = client.receive()
    val buffer = ByteBuffer.allocate(datagram.packet.remaining.toInt())
    datagram.packet.readFully(buffer)
    val ping = Packet.decodeFrom(buffer) as PingPacket
    assertEquals(discoveryService.nodeId, ping.nodeId)

    // check it didn't immediately send neighbors
    delay(500)

    val pong = PongPacket.create(
      clientKeyPair,
      System.currentTimeMillis(),
      ping.from,
      ping.hash,
      null
    )
    val bytes2 = ByteBuffer.allocate(Packet.MAX_SIZE)
    pong.encodeTo(bytes2)
    bytes2.flip()
    client.send(Datagram(ByteReadPacket(bytes2), address))

    val datagram2 = client.receive()
    val buffer2 = ByteBuffer.allocate(datagram2.packet.remaining.toInt())
    datagram2.packet.readFully(buffer2)
    val neighbors = Packet.decodeFrom(buffer2) as NeighborsPacket
    assertEquals(discoveryService.nodeId, neighbors.nodeId)

    val peer =
      peerRepository.get(
        URI(
          "enode://" + clientKeyPair.publicKey().toHexString() +
            "@127.0.0.1:" + discoveryService.localPort
        )
      )
    assertNotNull(peer.lastVerified)
    assertNotNull(peer.endpoint)

    discoveryService.shutdownNow()
  }

  @Disabled
  @Test
  fun shouldConnectToNetworkAndDoALookup() {
    /* ktlint-disable */
    val boostrapNodes = listOf(
      "enode://6332792c4a00e3e4ee0926ed89e0d27ef985424d97b6a45bf0f23e51f0dcb5e66b875777506458aea7af6f9e4ffb69f43f3778ee73c81ed9d34c51c4b16b0b0f@52.232.243.152:30303"
//      "enode://94c15d1b9e2fe7ce56e458b9a3b672ef11894ddedd0c6f247e0f1d3487f52b66208fb4aeb8179fce6e3a749ea93ed147c37976d67af557508d199d9594c35f09@192.81.208.223:30303"
    ).map { s -> URI.create(s) }
    /* ktlint-enable */
    val discoveryService = DiscoveryService.open(
      host = "127.0.0.1",
      keyPair = SECP256K1.KeyPair.random(),
      bootstrapURIs = boostrapNodes
    )

    runBlocking {
      discoveryService.awaitBootstrap()
      val result = discoveryService.lookup(SECP256K1.KeyPair.random().publicKey())
      assertTrue(result.isNotEmpty())
      discoveryService.shutdown()
      discoveryService.awaitTermination()
    }
  }
}
