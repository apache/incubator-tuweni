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

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.SocketAddress
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.CompletableAsyncResult
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

@Timeout(10)
@ExtendWith(BouncyCastleExtension::class, VertxExtension::class)
internal class DiscoveryServiceTest {

  @Test
  fun shouldStartAndShutdownService(@VertxInstance vertx: Vertx) = runBlocking {
    val discoveryService = DiscoveryService.open(
      vertx,
      host = "127.0.0.1",
      keyPair = SECP256K1.KeyPair.random()
    )
    discoveryService.awaitBootstrap()
    assertFalse(discoveryService.isShutdown)
    discoveryService.shutdown()
    assertTrue(discoveryService.isShutdown)
  }

  @Test
  fun shouldRespondToPingAndRecordEndpoint(@VertxInstance vertx: Vertx) = runBlocking {
    val peerRepository = EphemeralPeerRepository()
    val discoveryService = DiscoveryService.open(
      vertx,
      host = "127.0.0.1",
      keyPair = SECP256K1.KeyPair.random(),
      peerRepository = peerRepository
    )
    discoveryService.awaitBootstrap()
    val address = SocketAddress.inetSocketAddress(discoveryService.localPort, "127.0.0.1")
    val clientKeyPair = SECP256K1.KeyPair.random()
    val reference = AsyncResult.incomplete<Buffer>()
    val client = vertx.createDatagramSocket().handler { res ->
      reference.complete(res.data())
    }.listen(0, "localhost").await()
    val clientEndpoint = Endpoint("192.168.1.1", 5678, 7654)
    val ping = PingPacket.create(
      clientKeyPair,
      System.currentTimeMillis(),
      clientEndpoint,
      Endpoint(address),
      null
    )
    client.send(Buffer.buffer(ping.encode().toArrayUnsafe()), address.port(), address.host()).await()
    val datagram = reference.await()
    val buffer = ByteBuffer.allocate(datagram.length())
    datagram.byteBuf.readBytes(buffer)
    val pong = Packet.decodeFrom(buffer) as PongPacket
    assertEquals(discoveryService.nodeId, pong.nodeId)
    assertEquals(ping.hash, pong.pingHash)

    val peer = peerRepository.get(URI("enode://" + clientKeyPair.publicKey().toHexString() + "@127.0.0.1:5678"))
    assertNotNull(peer.endpoint)
    assertEquals(clientEndpoint.tcpPort, peer.endpoint.tcpPort)
    discoveryService.shutdown()
    client.close()
  }

  @Test
  fun shouldPingBootstrapNodeAndValidate(@VertxInstance vertx: Vertx) = runBlocking {
    val bootstrapKeyPair = SECP256K1.KeyPair.random()
    val reference = AtomicReference<CompletableAsyncResult<Buffer>>()
    reference.set(AsyncResult.incomplete())
    val bootstrapClient = vertx.createDatagramSocket().handler { res ->
      reference.get().complete(res.data())
    }.listen(0, "127.0.0.1").await()

    val serviceKeyPair = SECP256K1.KeyPair.random()
    val peerRepository = EphemeralPeerRepository()
    val routingTable = DevP2PPeerRoutingTable(serviceKeyPair.publicKey())
    val discoveryService = DiscoveryService.open(
      vertx,
      host = "127.0.0.1",
      keyPair = serviceKeyPair,
      bootstrapURIs = listOf(
        URI(
          "enode://" + bootstrapKeyPair.publicKey().toHexString() + "@127.0.0.1:" + bootstrapClient.localAddress()
            .port()
        )
      ),
      peerRepository = peerRepository,
      routingTable = routingTable
    )

    val datagram = reference.get().await()
    val buffer = ByteBuffer.allocate(datagram.length())
    datagram.byteBuf.readBytes(buffer)
    val ping = Packet.decodeFrom(buffer) as PingPacket
    assertEquals(discoveryService.nodeId, ping.nodeId)
    assertEquals(
      ping.to,
      Endpoint("127.0.0.1", bootstrapClient.localAddress().port(), bootstrapClient.localAddress().port())
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
    reference.set(AsyncResult.incomplete())
    val address = SocketAddress.inetSocketAddress(discoveryService.localPort, "127.0.0.1")
    bootstrapClient.send(Buffer.buffer(pong.encode().toArrayUnsafe()), address.port(), address.host()).await()

    val findNodesDatagram = reference.get().await()

    val findNodes = Packet.decodeFrom(Bytes.wrap(findNodesDatagram.bytes)) as FindNodePacket
    assertEquals(discoveryService.nodeId, findNodes.nodeId)
    assertEquals(discoveryService.nodeId, findNodes.target)

    val bootstrapPeer =
      peerRepository.get(
        URI(
          "enode://" + bootstrapKeyPair.publicKey().toHexString() +
            "@127.0.0.1:" + bootstrapClient.localAddress().port()
        )
      )
    assertNotNull(bootstrapPeer.lastVerified)
    assertNotNull(bootstrapPeer.endpoint)
    assertEquals(bootstrapClient.localAddress().port(), bootstrapPeer.endpoint.tcpPort)

    assertTrue(routingTable.contains(bootstrapPeer))

    discoveryService.shutdown()
    bootstrapClient.close()
  }

  @Test
  fun shouldIgnoreBootstrapNodeRespondingWithDifferentNodeId(@VertxInstance vertx: Vertx) = runBlocking {
    println("foo")
    val bootstrapKeyPair = SECP256K1.KeyPair.random()
    val reference = AsyncResult.incomplete<Buffer>()
    val bootstrapClient = vertx.createDatagramSocket().handler { res ->
      reference.complete(res.data())
    }.listen(0, "localhost").await()

    val serviceKeyPair = SECP256K1.KeyPair.random()
    val peerRepository = EphemeralPeerRepository()
    val routingTable = DevP2PPeerRoutingTable(serviceKeyPair.publicKey())
    val discoveryService = DiscoveryService.open(
      vertx,
      host = "127.0.0.1",
      keyPair = serviceKeyPair,
      bootstrapURIs = listOf(
        URI(
          "enode://" + bootstrapKeyPair.publicKey().toHexString() + "@127.0.0.1:" + bootstrapClient.localAddress()
            .port()
        )
      ),
      peerRepository = peerRepository,
      routingTable = routingTable
    )
    val datagram = reference.await()
    val buffer = ByteBuffer.allocate(datagram.length())
    datagram.byteBuf.readBytes(buffer)
    val ping = Packet.decodeFrom(buffer) as PingPacket
    assertEquals(discoveryService.nodeId, ping.nodeId)
    assertEquals(
      ping.to,
      Endpoint("127.0.0.1", bootstrapClient.localAddress().port(), bootstrapClient.localAddress().port())
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
    val address = SocketAddress.inetSocketAddress(discoveryService.localPort, "127.0.0.1")
    bootstrapClient.send(Buffer.buffer(pong.encode().toArrayUnsafe()), address.port(), address.host()).await()

    delay(1000)
    val bootstrapPeer =
      peerRepository.get(
        URI(
          "enode://" + bootstrapKeyPair.publicKey().toHexString() +
            "@127.0.0.1:" + bootstrapClient.localAddress().port()
        )
      )
    assertNull(bootstrapPeer.lastVerified)
    assertFalse(routingTable.contains(bootstrapPeer))

    discoveryService.shutdown()
    bootstrapClient.close()
  }

  @Test
  fun shouldPingBootstrapNodeWithAdvertisedAddress(@VertxInstance vertx: Vertx) = runBlocking {
    val bootstrapKeyPair = SECP256K1.KeyPair.random()
    val reference = AsyncResult.incomplete<Buffer>()
    val bootstrapClient = vertx.createDatagramSocket().handler { res ->
      reference.complete(res.data())
    }.listen(0, "localhost").await()

    val discoveryService = DiscoveryService.open(
      vertx,
      host = "127.0.0.1",
      keyPair = SECP256K1.KeyPair.random(),
      bootstrapURIs = listOf(
        URI(
          "enode://" + bootstrapKeyPair.publicKey().bytes()
            .toHexString() + "@127.0.0.1:" + bootstrapClient.localAddress().port()
        )
      ),
      advertiseAddress = "192.168.66.55",
      advertiseUdpPort = 3836,
      advertiseTcpPort = 8765
    )

    val datagram = reference.await()
    val buffer = ByteBuffer.allocate(datagram.length())
    datagram.byteBuf.readBytes(buffer)
    val ping = Packet.decodeFrom(buffer) as PingPacket
    assertEquals(discoveryService.nodeId, ping.nodeId)
    assertEquals(
      Endpoint("127.0.0.1", bootstrapClient.localAddress().port(), bootstrapClient.localAddress().port()),
      ping.to
    )
    assertEquals(Endpoint("192.168.66.55", 3836, 8765), ping.from)

    discoveryService.shutdown()
    bootstrapClient.close()
  }

  @Test
  fun shouldRetryPingsToBootstrapNodes(@VertxInstance vertx: Vertx) = runBlocking {
    val bootstrapKeyPair = SECP256K1.KeyPair.random()
    val reference = AtomicReference<CompletableAsyncResult<Buffer>>()
    reference.set(AsyncResult.incomplete())
    val bootstrapClient = vertx.createDatagramSocket().handler { res ->
      reference.get().complete(res.data())
    }.listen(0, "localhost").await()

    val discoveryService = DiscoveryService.open(
      vertx,
      host = "127.0.0.1",
      keyPair = SECP256K1.KeyPair.random(),
      bootstrapURIs = listOf(
        URI(
          "enode://" + bootstrapKeyPair.publicKey().bytes()
            .toHexString() + "@127.0.0.1:" + bootstrapClient.localAddress().port()
        )
      )
    )
    val datagram1 = reference.get().await()
    reference.set(AsyncResult.incomplete())
    val buffer1 = ByteBuffer.allocate(datagram1.length())
    datagram1.byteBuf.readBytes(buffer1)
    val ping1 = Packet.decodeFrom(buffer1) as PingPacket
    assertEquals(discoveryService.nodeId, ping1.nodeId)
    assertEquals(
      Endpoint("127.0.0.1", bootstrapClient.localAddress().port(), bootstrapClient.localAddress().port()),
      ping1.to
    )
    val datagram2 = reference.get().await()
    reference.set(AsyncResult.incomplete())
    val buffer2 = ByteBuffer.allocate(datagram2.length())
    datagram2.byteBuf.readBytes(buffer2)
    val ping2 = Packet.decodeFrom(buffer2) as PingPacket
    assertEquals(discoveryService.nodeId, ping2.nodeId)
    assertEquals(
      Endpoint("127.0.0.1", bootstrapClient.localAddress().port(), bootstrapClient.localAddress().port()),
      ping2.to
    )
    val datagram3 = reference.get().await()
    reference.set(AsyncResult.incomplete())
    val buffer3 = ByteBuffer.allocate(datagram3.length())
    datagram3.byteBuf.readBytes(buffer3)
    val ping3 = Packet.decodeFrom(buffer3) as PingPacket
    assertEquals(discoveryService.nodeId, ping3.nodeId)
    assertEquals(
      Endpoint("127.0.0.1", bootstrapClient.localAddress().port(), bootstrapClient.localAddress().port()),
      ping3.to
    )
    discoveryService.shutdown()
    bootstrapClient.close()
  }

  @Test
  fun shouldRequirePingPongBeforeRespondingToFindNodesFromUnverifiedPeer(@VertxInstance vertx: Vertx) = runBlocking {
    val peerRepository = EphemeralPeerRepository()
    val discoveryService = DiscoveryService.open(
      vertx,
      host = "127.0.0.1",
      keyPair = SECP256K1.KeyPair.random(),
      peerRepository = peerRepository
    )
    discoveryService.awaitBootstrap()
    val address = SocketAddress.inetSocketAddress(discoveryService.localPort, "127.0.0.1")

    val clientKeyPair = SECP256K1.KeyPair.random()
    val reference = AtomicReference<CompletableAsyncResult<Buffer>>()
    reference.set(AsyncResult.incomplete())
    val client = vertx.createDatagramSocket().handler { res ->
      reference.get().complete(res.data())
    }.listen(0, "localhost").await()
    val findNodes =
      FindNodePacket.create(
        clientKeyPair,
        System.currentTimeMillis(),
        SECP256K1.KeyPair.random().publicKey()
      )
    client.send(Buffer.buffer(findNodes.encode().toArrayUnsafe()), address.port(), address.host()).await()

    val datagram = reference.get().await()
    val buffer = ByteBuffer.allocate(datagram.length())
    datagram.byteBuf.readBytes(buffer)
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

    reference.set(AsyncResult.incomplete())
    client.send(Buffer.buffer(pong.encode().toArrayUnsafe()), address.port(), address.host()).await()

    val datagram2 = reference.get().await()
    val buffer2 = ByteBuffer.allocate(datagram2.length())
    datagram2.byteBuf.readBytes(buffer2)
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

    discoveryService.shutdown()
    client.close()
  }

  @Disabled
  @Test
  fun shouldConnectToNetworkAndDoALookup(@VertxInstance vertx: Vertx) {
    /* ktlint-disable */
    val boostrapNodes = listOf(
      "enode://6332792c4a00e3e4ee0926ed89e0d27ef985424d97b6a45bf0f23e51f0dcb5e66b875777506458aea7af6f9e4ffb69f43f3778ee73c81ed9d34c51c4b16b0b0f@52.232.243.152:30303"
//      "enode://94c15d1b9e2fe7ce56e458b9a3b672ef11894ddedd0c6f247e0f1d3487f52b66208fb4aeb8179fce6e3a749ea93ed147c37976d67af557508d199d9594c35f09@192.81.208.223:30303"
    ).map { s -> URI.create(s) }
    /* ktlint-enable */
    val discoveryService = DiscoveryService.open(
      vertx,
      host = "127.0.0.1",
      keyPair = SECP256K1.KeyPair.random(),
      bootstrapURIs = boostrapNodes
    )

    runBlocking {
      discoveryService.awaitBootstrap()
      val result = discoveryService.lookup(SECP256K1.KeyPair.random().publicKey())
      assertTrue(result.isNotEmpty())
      discoveryService.shutdown()
    }
  }
}
