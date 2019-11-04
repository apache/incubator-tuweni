package org.apache.tuweni.devp2p.v5

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.internal.DefaultAuthenticationProvider
import org.apache.tuweni.devp2p.v5.internal.DefaultPacketCodec
import org.apache.tuweni.devp2p.v5.internal.DefaultUdpConnector
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import org.apache.tuweni.devp2p.v5.storage.DefaultENRStorage
import org.apache.tuweni.devp2p.v5.storage.RoutingTable
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress
import java.net.InetSocketAddress

@ExtendWith(BouncyCastleExtension::class)
abstract class AbstractIntegrationTest {

  protected fun createNode(
    port: Int = 9090,
    bootList: List<String> = emptyList(),
    enrStorage: ENRStorage = DefaultENRStorage(),
    keyPair: SECP256K1.KeyPair = SECP256K1.KeyPair.random(),
    enr: Bytes = EthereumNodeRecord.toRLP(keyPair, ip = InetAddress.getLocalHost(), udp = port),
    routingTable: RoutingTable = RoutingTable(enr),
    address: InetSocketAddress = InetSocketAddress(InetAddress.getLocalHost(), port),
    authenticationProvider: AuthenticationProvider = DefaultAuthenticationProvider(keyPair, routingTable),
    packetCodec: PacketCodec = DefaultPacketCodec(
      keyPair,
      routingTable,
      authenticationProvider = authenticationProvider
    ),
    connector: UdpConnector = DefaultUdpConnector(
      address,
      keyPair,
      enr,
      enrStorage,
      nodesTable = routingTable,
      packetCodec = packetCodec
    ),
    service: NodeDiscoveryService =
      DefaultNodeDiscoveryService(
        keyPair,
        port,
        enrStorage = enrStorage,
        bootstrapENRList = bootList,
        connector = connector
      )
  ): TestNode {
    service.start()
    return TestNode(
      bootList,
      port,
      enrStorage,
      keyPair,
      enr,
      address,
      routingTable,
      authenticationProvider,
      packetCodec,
      connector,
      service
    )
  }

  protected fun handshake(initiator: TestNode, recipient: TestNode): Boolean {
    initiator.enrStorage.set(recipient.enr)
    initiator.routingTable.add(recipient.enr)
    val message = RandomMessage()
    initiator.connector.send(recipient.address, message, recipient.nodeId)
    while (true) {
      if (null != recipient.authenticationProvider.findSessionKey(initiator.nodeId.toHexString())) {
        return true
      }
    }
  }

  protected fun send(initiator: TestNode, recipient: TestNode, message: UdpMessage) {
    if (message is RandomMessage || message is WhoAreYouMessage) {
      throw IllegalArgumentException("Can't send handshake initiation message")
    }
    initiator.connector.send(recipient.address, message, recipient.nodeId)
  }

  protected inline fun <reified T: UdpMessage>sendAndAwait(initiator: TestNode, recipient: TestNode, message: UdpMessage): T {
    val listener = object : MessageObserver {
      var result: Channel<T> = Channel()

      override fun observe(message: UdpMessage) {
        if (message is T) {
          result.offer(message)
        }
      }
    }

    return runBlocking {
      initiator.connector.attachObserver(listener)
      send(initiator, recipient, message)
      val result = listener.result.receive()
      initiator.connector.detachObserver(listener)
      result
    }
  }
}

class TestNode(
  val bootList: List<String>,
  val port: Int,
  val enrStorage: ENRStorage,
  val keyPair: SECP256K1.KeyPair,
  val enr: Bytes,
  val address: InetSocketAddress,
  val routingTable: RoutingTable,
  val authenticationProvider: AuthenticationProvider,
  val packetCodec: PacketCodec,
  val connector: UdpConnector,
  val service: NodeDiscoveryService,
  val nodeId: Bytes = Hash.sha2_256(enr)
)
