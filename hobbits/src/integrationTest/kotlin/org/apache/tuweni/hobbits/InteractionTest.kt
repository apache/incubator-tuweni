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
package org.apache.tuweni.hobbits

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
class TCPPersistentTest {

  @Test
  fun testTwoTCPConnections(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val newPort = AtomicInteger()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    runBlocking {
      client1.createTCPEndpoint(
        "foo",
        networkInterface = "127.0.0.1",
        port = 0,
        handler = ref::set,
        portUpdateListener = newPort::set
      )
      client1.start()
      client2.start()
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.TCP,
        "127.0.0.1",
        newPort.get()
      )
    }
    Thread.sleep(200)
    assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    client1.stop()
    client2.stop()
  }

  @Disabled
  @Test
  fun testTwoTCPConnectionsWithTLS(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val newPort = AtomicInteger()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    runBlocking {
      client1.createTCPEndpoint(
        "foo",
        networkInterface = "127.0.0.1",
        port = 0,
        handler = ref::set,
        tls = true,
        portUpdateListener = newPort::set
      )
      client1.start()
      client2.start()
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.TCP,
        "127.0.0.1",
        newPort.get()
      )
    }
    Thread.sleep(200)
    assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    client1.stop()
    client2.stop()
  }

  @Test
  fun testTwoEndpoints(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val ref2 = AtomicReference<Message>()
    val newPort = AtomicInteger()
    val newPort2 = AtomicInteger()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    runBlocking {
      client1.createTCPEndpoint(
        "foo",
        networkInterface = "127.0.0.1",
        port = 0,
        handler = ref::set,
        portUpdateListener = newPort::set
      )
      client1.createTCPEndpoint(
        "bar",
        networkInterface = "127.0.0.1",
        port = 0,
        handler = ref2::set,
        portUpdateListener = newPort2::set
      )
      client1.start()
      client2.start()
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.TCP,
        "127.0.0.1",
        newPort.get()
      )
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.TCP,
        "127.0.0.1",
        newPort2.get()
      )
    }
    Thread.sleep(200)
    assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    assertEquals(Bytes.fromHexString("deadbeef"), ref2.get().body)
    client1.stop()
    client2.stop()
  }
}

@ExtendWith(VertxExtension::class)
class HTTPTest {
  @Test
  fun testTwoHTTPConnections(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    val newPort = AtomicInteger()

    runBlocking {
      client1.createHTTPEndpoint(
        "foo",
        networkInterface = "127.0.0.1",
        port = 0,
        handler = ref::set,
        portUpdateListener = newPort::set
      )
      client1.start()
      client2.start()
      client2.sendMessage(
        Message(
          protocol = Protocol.PING,
          body = Bytes.fromHexString("deadbeef"),
          headers = Bytes.random(16)
        ),
        Transport.HTTP,
        "127.0.0.1",
        newPort.get()
      )
    }
    Thread.sleep(200)
    assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    client1.stop()
    client2.stop()
  }

  @Test
  fun testTwoEndpoints(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val ref2 = AtomicReference<Message>()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    val newPort = AtomicInteger()
    val newPort2 = AtomicInteger()
    runBlocking {
      client1.createHTTPEndpoint(
        "foo",
        networkInterface = "127.0.0.1",
        port = 0,
        handler = ref::set,
        portUpdateListener = newPort::set
      )
      client1.createHTTPEndpoint(
        "bar",
        networkInterface = "127.0.0.1",
        port = 0,
        handler = ref2::set,
        portUpdateListener = newPort2::set
      )
      client1.start()
      client2.start()
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.HTTP,
        "127.0.0.1",
        newPort.get()
      )
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.HTTP,
        "127.0.0.1",
        newPort2.get()
      )
    }
    Thread.sleep(200)
    assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    assertEquals(Bytes.fromHexString("deadbeef"), ref2.get().body)
    client1.stop()
    client2.stop()
  }
}

@ExtendWith(VertxExtension::class)
class UDPTest {
  @Test
  fun testTwoUDPConnections(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)

    runBlocking {
      client1.createUDPEndpoint("foo", "localhost", 15000, ref::set)
      client1.start()
      client2.start()
      client2.sendMessage(
        Message(
          protocol = Protocol.PING,
          body = Bytes.fromHexString("deadbeef"),
          headers = Bytes.random(16)
        ),
        Transport.UDP,
        "localhost",
        15000
      )
    }
    Thread.sleep(200)
    assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    client1.stop()
    client2.stop()
  }

  @Test
  fun testTwoEndpoints(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val ref2 = AtomicReference<Message>()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    runBlocking {
      client1.createUDPEndpoint("foo", "localhost", 16000, ref::set)
      client1.createUDPEndpoint("bar", "localhost", 16001, ref2::set)
      client1.start()
      client2.start()
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.UDP,
        "localhost",
        16000
      )
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.UDP,
        "localhost",
        16001
      )
    }
    Thread.sleep(200)
    assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    assertEquals(Bytes.fromHexString("deadbeef"), ref2.get().body)
    client1.stop()
    client2.stop()
  }
}

@ExtendWith(VertxExtension::class)
class WebSocketTest {
  @Test
  fun testTwoWSConnections(@VertxInstance vertx: Vertx) {
    vertx.exceptionHandler { it.printStackTrace() }
    val ref = AtomicReference<Message>()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    val newPort = AtomicInteger()

    runBlocking {
      client1.createWSEndpoint(
        "foo",
        networkInterface = "127.0.0.1",
        port = 0,
        handler = ref::set,
        portUpdateListener = newPort::set
      )
      client1.start()
      client2.start()
      client2.sendMessage(
        Message(
          protocol = Protocol.PING,
          body = Bytes.fromHexString("deadbeef"),
          headers = Bytes.random(16)
        ),
        Transport.WS,
        "127.0.0.1",
        newPort.get()
      )
    }
    Thread.sleep(200)
    assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    client1.stop()
    client2.stop()
  }

  @Test
  fun testTwoEndpoints(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val ref2 = AtomicReference<Message>()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    val newPort = AtomicInteger()
    val newPort2 = AtomicInteger()
    runBlocking {
      client1.exceptionHandler { it.printStackTrace() }
      client2.exceptionHandler { it.printStackTrace() }
      client1.createWSEndpoint(
        "foo",
        networkInterface = "127.0.0.1",
        port = 0,
        handler = ref::set,
        portUpdateListener = newPort::set
      )
      client1.createWSEndpoint(
        "bar",
        networkInterface = "127.0.0.1",
        port = 0,
        handler = ref2::set,
        portUpdateListener = newPort2::set
      )
      client1.start()
      client2.start()
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.WS,
        "127.0.0.1",
        newPort.get()
      )
      client2.sendMessage(
        Message(protocol = Protocol.PING, body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.WS,
        "127.0.0.1",
        newPort2.get()
      )
    }
    Thread.sleep(200)
    assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    assertEquals(Bytes.fromHexString("deadbeef"), ref2.get().body)
    client1.stop()
    client2.stop()
  }
}
