// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.relayer

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.hobbits.HobbitsTransport
import org.apache.tuweni.hobbits.Message
import org.apache.tuweni.hobbits.Protocol
import org.apache.tuweni.hobbits.Relayer
import org.apache.tuweni.hobbits.Transport
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
class RelayerAppTest {

  @Test
  fun testRelayerApp(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    RelayerApp.main(arrayOf("-b", "tcp://localhost:21000", "-t", "tcp://localhost:22000"))
    runBlocking {
      client1.createTCPEndpoint("foo", networkInterface = "127.0.0.1", port = 22000, handler = ref::set)
      client1.start()
      client2.start()
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.TCP,
        "localhost",
        21000
      )
    }
    Thread.sleep(1000)
    Assertions.assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    client1.stop()
    client2.stop()
  }

  @Test
  fun testClose(@VertxInstance vertx: Vertx) {
    val relayer = Relayer(vertx, "tcp://localhost:21000", "tcp://localhost:21001", {})
    RelayerApp(vertx, relayer)
  }
}
