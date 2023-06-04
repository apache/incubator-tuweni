// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.hobbits

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
class RelayerTest {

  @Test
  fun testTCPRelay(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    val relayer = Relayer(vertx, "tcp://localhost:22000", "tcp://localhost:20000", { })
    runBlocking {
      client1.createTCPEndpoint("foo", networkInterface = "127.0.0.1", port = 20000, handler = ref::set)
      client1.start()
      client2.start()
      relayer.start()
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.TCP,
        "localhost",
        22000
      )
    }
    Thread.sleep(1000)
    Assertions.assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    client1.stop()
    client2.stop()
    relayer.stop()
  }

  @Test
  fun testHTTPRelay(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    val relayer = Relayer(vertx, "http://localhost:13000", "http://localhost:11000", { })
    runBlocking {
      client1.createHTTPEndpoint("foo", networkInterface = "127.0.0.1", port = 11000, handler = ref::set)
      client1.start()
      client2.start()
      relayer.start()
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.HTTP,
        "localhost",
        13000
      )
    }
    Thread.sleep(1000)
    Assertions.assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    client1.stop()
    client2.stop()
    relayer.stop()
  }

  @Test
  fun testUDPRelay(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    val relayer = Relayer(vertx, "udp://localhost:12000", "udp://localhost:10000", { })
    runBlocking {
      client1.createUDPEndpoint("foo", port = 10000, handler = ref::set)
      client1.start()
      client2.start()
      relayer.start()
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.UDP,
        "localhost",
        12000
      )
    }
    Thread.sleep(1000)
    Assertions.assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    client1.stop()
    client2.stop()
    relayer.stop()
  }

  @Test
  fun testWSRelay(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    val relayer = Relayer(vertx, "ws://localhost:32000", "ws://localhost:30000", { })
    runBlocking {
      client1.createWSEndpoint("foo", port = 30000, handler = ref::set)
      client1.start()
      client2.start()
      relayer.start()
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.WS,
        "localhost",
        32000
      )
    }
    Thread.sleep(1000)
    Assertions.assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    client1.stop()
    client2.stop()
    relayer.stop()
  }
}
