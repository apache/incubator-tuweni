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

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.logl.Level
import org.logl.logl.SimpleLogger
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.URI
import java.nio.ByteBuffer

private suspend fun CoroutineDatagramChannel.send(packet: Packet, address: SocketAddress): Int {
  val buffer = packet.encodeTo(ByteBuffer.allocate(2048))
  buffer.flip()
  return send(buffer, address)
}

private suspend fun CoroutineDatagramChannel.receivePacket(): Packet {
  val buffer = ByteBuffer.allocate(2048)
  receive(buffer)
  buffer.flip()
  return Packet.decodeFrom(buffer)
}

@ExtendWith(BouncyCastleExtension::class)
internal class DiscoveryServiceTest {

  @Test
  fun shouldStartAndShutdownService() {
    val discoveryService = DiscoveryService.open(SECP256K1.KeyPair.random())
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
      loggerProvider = SimpleLogger.withLogLevel(Level.ERROR).toOutputStream(System.err),
      keyPair = SECP256K1.KeyPair.random(),
      peerRepository = peerRepository
    )
    val address = InetSocketAddress(InetAddress.getLocalHost(), discoveryService.localPort)

    val clientKeyPair = SECP256K1.KeyPair.random()
    val client = CoroutineDatagramChannel.open()
    val clientEndpoint = Endpoint("192.168.1.1", 5678, 7654)
    val ping = PingPacket.create(clientKeyPair, System.currentTimeMillis(), clientEndpoint, Endpoint(address), null)
    client.send(ping, address)

    val pong = client.receivePacket() as PongPacket
    assertEquals(discoveryService.nodeId, pong.nodeId)
    assertEquals(ping.hash, pong.pingHash)

    val peer = peerRepository.get(clientKeyPair.publicKey())
    assertNotNull(peer.endpoint)
    assertEquals(clientEndpoint.tcpPort, peer.endpoint?.tcpPort)

    discoveryService.shutdownNow()
  }

  @Test
  fun shouldPingBootstrapNodeAndValidate() = runBlocking {
    val bootstrapKeyPair = SECP256K1.KeyPair.random()
    val bootstrapClient = CoroutineDatagramChannel.open().bind(InetSocketAddress(0))

    val serviceKeyPair = SECP256K1.KeyPair.random()
    val peerRepository = EphemeralPeerRepository()
    val routingTable = DevP2PPeerRoutingTable(serviceKeyPair.publicKey())
    val discoveryService = DiscoveryService.open(
      loggerProvider = SimpleLogger.withLogLevel(Level.ERROR).toOutputStream(System.err),
      keyPair = serviceKeyPair,
      bootstrapURIs = listOf(
        URI("enode://" + bootstrapKeyPair.publicKey().toHexString() + "@127.0.0.1:" + bootstrapClient.localPort)
      ),
      peerRepository = peerRepository,
      routingTable = routingTable
    )
    val address = InetSocketAddress(InetAddress.getLocalHost(), discoveryService.localPort)

    val ping = bootstrapClient.receivePacket() as PingPacket
    assertEquals(discoveryService.nodeId, ping.nodeId)
    assertEquals(ping.to, Endpoint("127.0.0.1", bootstrapClient.localPort, bootstrapClient.localPort))
    assertEquals(discoveryService.localPort, ping.from.udpPort)
    assertNull(ping.from.tcpPort)

    val pong = PongPacket.create(bootstrapKeyPair, System.currentTimeMillis(), ping.from, ping.hash, null)
    bootstrapClient.send(pong, address)

    val findNodes = bootstrapClient.receivePacket() as FindNodePacket
    assertEquals(discoveryService.nodeId, findNodes.nodeId)
    assertEquals(discoveryService.nodeId, findNodes.target)

    val bootstrapPeer = peerRepository.get(bootstrapKeyPair.publicKey())
    assertNotNull(bootstrapPeer.lastVerified)
    assertNotNull(bootstrapPeer.endpoint)
    assertEquals(bootstrapClient.localPort, bootstrapPeer.endpoint?.tcpPort)

    assertTrue(routingTable.contains(bootstrapPeer))

    discoveryService.shutdownNow()
  }

  @Test
  fun shouldIgnoreBootstrapNodeRespondingWithDifferentNodeId() = runBlocking {
    val bootstrapKeyPair = SECP256K1.KeyPair.random()
    val bootstrapClient = CoroutineDatagramChannel.open().bind(InetSocketAddress(0))

    val serviceKeyPair = SECP256K1.KeyPair.random()
    val peerRepository = EphemeralPeerRepository()
    val routingTable = DevP2PPeerRoutingTable(serviceKeyPair.publicKey())
    val discoveryService = DiscoveryService.open(
      loggerProvider = SimpleLogger.withLogLevel(Level.ERROR).toOutputStream(System.err),
      keyPair = serviceKeyPair,
      bootstrapURIs = listOf(
        URI("enode://" + bootstrapKeyPair.publicKey().toHexString() + "@127.0.0.1:" + bootstrapClient.localPort)
      ),
      peerRepository = peerRepository,
      routingTable = routingTable
    )
    val address = InetSocketAddress(InetAddress.getLocalHost(), discoveryService.localPort)

    val ping = bootstrapClient.receivePacket() as PingPacket
    assertEquals(discoveryService.nodeId, ping.nodeId)
    assertEquals(ping.to, Endpoint("127.0.0.1", bootstrapClient.localPort, bootstrapClient.localPort))
    assertEquals(discoveryService.localPort, ping.from.udpPort)
    assertNull(ping.from.tcpPort)

    val pong = PongPacket.create(SECP256K1.KeyPair.random(), System.currentTimeMillis(), ping.from, ping.hash, null)
    bootstrapClient.send(pong, address)

    delay(1000)
    val bootstrapPeer = peerRepository.get(bootstrapKeyPair.publicKey())
    assertNull(bootstrapPeer.lastVerified)
    assertFalse(routingTable.contains(bootstrapPeer))

    discoveryService.shutdownNow()
  }

  @Test
  fun shouldPingBootstrapNodeWithAdvertisedAddress() = runBlocking {
    val bootstrapKeyPair = SECP256K1.KeyPair.random()
    val boostrapClient = CoroutineDatagramChannel.open().bind(InetSocketAddress(0))

    val discoveryService = DiscoveryService.open(
      loggerProvider = SimpleLogger.withLogLevel(Level.ERROR).toOutputStream(System.err),
      keyPair = SECP256K1.KeyPair.random(),
      bootstrapURIs = listOf(
        URI("enode://" + bootstrapKeyPair.publicKey().bytes().toHexString() + "@127.0.0.1:" + boostrapClient.localPort)
      ),
      advertiseAddress = InetAddress.getByName("192.168.66.55"),
      advertiseUdpPort = 3836,
      advertiseTcpPort = 8765
    )

    val ping = boostrapClient.receivePacket() as PingPacket
    assertEquals(discoveryService.nodeId, ping.nodeId)
    assertEquals(Endpoint("127.0.0.1", boostrapClient.localPort, boostrapClient.localPort), ping.to)
    assertEquals(Endpoint("192.168.66.55", 3836, 8765), ping.from)

    discoveryService.shutdownNow()
  }

  @Test
  fun shouldRetryPingsToBootstrapNodes() = runBlocking {
    val bootstrapKeyPair = SECP256K1.KeyPair.random()
    val boostrapClient = CoroutineDatagramChannel.open().bind(InetSocketAddress(0))

    val discoveryService = DiscoveryService.open(
      loggerProvider = SimpleLogger.withLogLevel(Level.ERROR).toOutputStream(System.err),
      keyPair = SECP256K1.KeyPair.random(),
      bootstrapURIs = listOf(
        URI("enode://" + bootstrapKeyPair.publicKey().bytes().toHexString() + "@127.0.0.1:" + boostrapClient.localPort)
      )
    )

    val ping1 = boostrapClient.receivePacket() as PingPacket
    assertEquals(discoveryService.nodeId, ping1.nodeId)
    assertEquals(Endpoint("127.0.0.1", boostrapClient.localPort, boostrapClient.localPort), ping1.to)

    val ping2 = boostrapClient.receivePacket() as PingPacket
    assertEquals(discoveryService.nodeId, ping2.nodeId)
    assertEquals(Endpoint("127.0.0.1", boostrapClient.localPort, boostrapClient.localPort), ping2.to)

    val ping3 = boostrapClient.receivePacket() as PingPacket
    assertEquals(discoveryService.nodeId, ping3.nodeId)
    assertEquals(Endpoint("127.0.0.1", boostrapClient.localPort, boostrapClient.localPort), ping3.to)

    discoveryService.shutdownNow()
  }

  @Test
  fun shouldRequirePingPongBeforeRespondingToFindNodesFromUnverifiedPeer() = runBlocking {
    val peerRepository = EphemeralPeerRepository()
    val discoveryService = DiscoveryService.open(
      loggerProvider = SimpleLogger.withLogLevel(Level.ERROR).toOutputStream(System.err),
      keyPair = SECP256K1.KeyPair.random(),
      peerRepository = peerRepository
    )
    val address = InetSocketAddress(InetAddress.getLocalHost(), discoveryService.localPort)

    val clientKeyPair = SECP256K1.KeyPair.random()
    val client = CoroutineDatagramChannel.open()
    val findNodes =
      FindNodePacket.create(clientKeyPair, System.currentTimeMillis(), SECP256K1.KeyPair.random().publicKey())
    client.send(findNodes, address)

    val ping = client.receivePacket() as PingPacket
    assertEquals(discoveryService.nodeId, ping.nodeId)

    // check it didn't immediately send neighbors
    delay(500)
    assertNull(client.tryReceive(ByteBuffer.allocate(2048)))

    val pong = PongPacket.create(clientKeyPair, System.currentTimeMillis(), ping.from, ping.hash, null)
    client.send(pong, address)

    val neighbors = client.receivePacket() as NeighborsPacket
    assertEquals(discoveryService.nodeId, neighbors.nodeId)

    val peer = peerRepository.get(clientKeyPair.publicKey())
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
      SECP256K1.KeyPair.random(),
      bootstrapURIs = boostrapNodes,
      loggerProvider = SimpleLogger.withLogLevel(Level.DEBUG).toOutputStream(System.out)
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
