package org.apache.tuweni.devp2p.v5

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.devp2p.v5.packet.PingMessage
import org.apache.tuweni.devp2p.v5.packet.PongMessage
import org.junit.jupiter.api.Test

class HandshakeTest: AbstractIntegrationTest() {

  @Test
  fun testHandshake() {
    val node1 = createNode(9090)
    val node2 = createNode(9091)

    val result = handshake(node1, node2)

    assert(result)

    node1.service.terminate(true)
    node2.service.terminate(true)
  }

  @Test
  fun testPing() {
    val node1 = createNode(9090)
    val node2 = createNode(9091)

    handshake(node1, node2)
    val pong = sendAndAwait<PongMessage>(node1, node2, PingMessage())

    assert(node1.port == pong.recipientPort)

    node1.service.terminate(true)
    node2.service.terminate(true)
  }

  @Test
  fun testTableMaintenance() {
    val node1 = createNode(9090)
    val node2 = createNode(9091)

    handshake(node1, node2)
    runBlocking {
      assert(!node1.routingTable.isEmpty())

      node2.service.terminate( true)

      delay(3000)

      assert(node1.routingTable.isEmpty())
    }

    node1.service.terminate(true)
  }

}
