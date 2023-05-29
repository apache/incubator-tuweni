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
package org.apache.tuweni.stratum.server

import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.units.bigints.UInt256
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class StratumProtocolTest {

  @Test
  fun testStratum1CanHandle() {
    val protocol = Stratum1Protocol(
      "",
      submitCallback = { true },
      seedSupplier = Bytes32::random,
      coroutineContext = Dispatchers.Default,
      subscriptionIdCreator = { "1234" }
    )
    val conn = StratumConnection(emptyArray(), {}, {}, "foo")
    assertFalse(protocol.canHandle("", conn))
    assertFalse(protocol.canHandle("\"mining.subscribe", conn))
    assertFalse(protocol.canHandle("{\"method\": \"mining.subscribe\"}", conn))
    assertTrue(protocol.canHandle("""{"method": "mining.subscribe", "id": 1, "params": []}""", conn))
  }

  @Test
  fun testSendNewWorkStratum1() {
    val protocol = Stratum1Protocol(
      "",
      submitCallback = { true },
      seedSupplier = { Bytes32.repeat(1) },
      coroutineContext = Dispatchers.Default,
      subscriptionIdCreator = { "1234" },
      jobIdSupplier = { "5678" }
    )
    val ref = AtomicReference<String>()
    val conn = StratumConnection(emptyArray(), {}, ref::set, "foo")
    // subscribe
    assertTrue(protocol.canHandle("""{"method": "mining.subscribe", "id": 1, "params": []}""", conn))
    protocol.setCurrentWorkTask(PoWInput(UInt256.ZERO, Bytes32.repeat(2), 0L))
    assertEquals(
      """{"id":1,"jsonrpc":"2.0","result":[["mining.notify","1234","EthereumStratum/1.0.0"],""]}
""",
      ref.get()
    )
    // authorize:
    protocol.handle(conn, """{"method": "mining.authorize", "id": 2, "params": []}""")
    assertEquals(
      """{"jsonrpc":"2.0","method":"mining.notify","params":["5678","0x0202020202020202020202020202020202020202020202020202020202020202","0x0101010101010101010101010101010101010101010101010101010101010101","0x0000000000000000000000000000000000000000000000000000000000000000",true],"id":32}
""",
      ref.get()
    )
    // set new work
    protocol.setCurrentWorkTask(PoWInput(UInt256.ZERO, Bytes32.ZERO, 0L))
    assertEquals(
      """{"jsonrpc":"2.0","method":"mining.notify","params":["5678","0x0000000000000000000000000000000000000000000000000000000000000000","0x0101010101010101010101010101010101010101010101010101010101010101","0x0000000000000000000000000000000000000000000000000000000000000000",true],"id":32}
""",
      ref.get()
    )
  }

  @Test
  fun testGetWorkStratum1NoWork() {
    val protocol = Stratum1Protocol(
      "",
      submitCallback = { true },
      seedSupplier = { Bytes32.repeat(1) },
      coroutineContext = Dispatchers.Default,
      subscriptionIdCreator = { "1234" },
      jobIdSupplier = { "5678" }
    )
    val ref = AtomicReference<String>()
    val conn = StratumConnection(emptyArray(), {}, ref::set, "foo")
    // subscribe
    assertTrue(protocol.canHandle("""{"method": "mining.subscribe", "id": 1, "params": []}""", conn))
    protocol.setCurrentWorkTask(PoWInput(UInt256.ZERO, Bytes32.repeat(2), 0L))
    assertEquals(
      """{"id":1,"jsonrpc":"2.0","result":[["mining.notify","1234","EthereumStratum/1.0.0"],""]}
""",
      ref.get()
    )
    // authorize:
    protocol.handle(conn, """{"method": "mining.authorize", "id": 2, "params": []}""")
    assertEquals(
      """{"jsonrpc":"2.0","method":"mining.notify","params":["5678","0x0202020202020202020202020202020202020202020202020202020202020202","0x0101010101010101010101010101010101010101010101010101010101010101","0x0000000000000000000000000000000000000000000000000000000000000000",true],"id":32}
""",
      ref.get()
    )
  }

  @Test
  fun testGetWorkStratum1() {
    val protocol = Stratum1Protocol(
      "",
      submitCallback = { true },
      seedSupplier = Bytes32::random,
      coroutineContext = Dispatchers.Default,
      subscriptionIdCreator = { "1234" }
    )
    val ref = AtomicReference<String>()
    protocol.setCurrentWorkTask(PoWInput(UInt256.ZERO, Bytes32.ZERO, 0L))
    val conn = StratumConnection(emptyArray(), {}, ref::set, "foo")
    assertTrue(protocol.canHandle("""{"method": "mining.subscribe", "id": 1, "params": []}""", conn))
    assertEquals(
      """{"id":1,"jsonrpc":"2.0","result":[["mining.notify","1234","EthereumStratum/1.0.0"],""]}
""",
      ref.get()
    )
  }

  @Test
  fun testEthProxyCanHandle() {
    val protocol = Stratum1EthProxyProtocol(
      submitCallback = { true },
      seedSupplier = Bytes32::random,
      Dispatchers.Default
    )
    val conn = StratumConnection(emptyArray(), {}, {}, "foo")
    assertFalse(protocol.canHandle("", conn))
    assertFalse(protocol.canHandle("\"eth_submitLogin", conn))
    assertFalse(protocol.canHandle("{\"method\": \"eth_submitLogin\"}", conn))
    assertTrue(protocol.canHandle("{\"method\": \"eth_submitLogin\", \"id\": 1, \"params\": []}", conn))
  }

  @Test
  fun testSendNewWorkEth1Proxy() {
    val protocol = Stratum1EthProxyProtocol(
      submitCallback = { true },
      seedSupplier = { Bytes32.ZERO },
      Dispatchers.Default
    )
    val ref = AtomicReference<String>()
    val conn = StratumConnection(emptyArray(), {}, ref::set, "foo")
    assertTrue(protocol.canHandle("{\"method\": \"eth_submitLogin\", \"id\": 1, \"params\": []}", conn))
    protocol.setCurrentWorkTask(PoWInput(UInt256.ZERO, Bytes32.ZERO, 0L))
    assertEquals(
      """{"id":0,"jsonrpc":"2.0","result":["0x0000000000000000000000000000000000000000000000000000000000000000","0x0000000000000000000000000000000000000000000000000000000000000000","0x0000000000000000000000000000000000000000000000000000000000000000"]}
""",
      ref.get()
    )
  }

  @Test
  fun testGetWorkEth1ProxyNoWork() {
    val protocol = Stratum1EthProxyProtocol(
      submitCallback = { true },
      seedSupplier = { Bytes32.ZERO },
      Dispatchers.Default
    )
    val ref = AtomicReference<String>()
    val conn = StratumConnection(emptyArray(), {}, ref::set, "foo")
    assertTrue(protocol.canHandle("""{"method": "eth_submitLogin", "id": 1, "params": []}""", conn))
    assertEquals(
      """{"id":1,"jsonrpc":"2.0","result":true}
""",
      ref.get()
    )
    ref.set(null)
    protocol.handle(conn, """{"method": "eth_getWork", "id": 2, "params": []}""")
    assertNull(ref.get())
  }

  @Test
  fun testGetWorkEth1Proxy() {
    val protocol = Stratum1EthProxyProtocol(
      submitCallback = { true },
      seedSupplier = { Bytes32.ZERO },
      Dispatchers.Default
    )
    val ref = AtomicReference<String>()
    protocol.setCurrentWorkTask(PoWInput(UInt256.ZERO, Bytes32.ZERO, 0L))
    val conn = StratumConnection(emptyArray(), {}, ref::set, "foo")
    assertTrue(protocol.canHandle("""{"method": "eth_submitLogin", "id": 1, "params": []}""", conn))
    assertEquals(
      """{"id":1,"jsonrpc":"2.0","result":true}
""",
      ref.get()
    )
    protocol.handle(conn, """{"method": "eth_getWork", "id": 2, "params": []}""")
    assertEquals(
      """{"id":2,"jsonrpc":"2.0","result":["0x0000000000000000000000000000000000000000000000000000000000000000","0x0000000000000000000000000000000000000000000000000000000000000000","0x0000000000000000000000000000000000000000000000000000000000000000"]}
""",
      ref.get()
    )
  }

  @Test
  fun testMalformedSolutionClose() {
    val protocol = Stratum1EthProxyProtocol(
      submitCallback = { false },
      seedSupplier = { Bytes32.ZERO },
      Dispatchers.Default
    )
    val ref = AtomicReference<String>()
    protocol.setCurrentWorkTask(PoWInput(UInt256.ZERO, Bytes32.ZERO, 0L))
    val closeRef = AtomicBoolean()
    val conn = StratumConnection(emptyArray(), closeRef::set, ref::set, "foo")
    assertTrue(protocol.canHandle("""{"method": "eth_submitLogin", "id": 1, "params": []}""", conn))
    assertEquals(
      """{"id":1,"jsonrpc":"2.0","result":true}
""",
      ref.get()
    )
    protocol.handle(conn, """{"method": "eth_getWork", "id": 2, "params": []}""")
    assertEquals(
      """{"id":2,"jsonrpc":"2.0","result":["0x0000000000000000000000000000000000000000000000000000000000000000","0x0000000000000000000000000000000000000000000000000000000000000000","0x0000000000000000000000000000000000000000000000000000000000000000"]}
""",
      ref.get()
    )
    protocol.handle(conn, """{"method": "eth_submitWork", "id": 3, "params": []}""")
    assertTrue(closeRef.get())
  }

  @Test
  fun testBadSolutionsClose() {
    val protocol = Stratum1EthProxyProtocol(
      submitCallback = { false },
      seedSupplier = { Bytes32.ZERO },
      Dispatchers.Default
    )
    val ref = AtomicReference<String>()
    protocol.setCurrentWorkTask(PoWInput(UInt256.ZERO, Bytes32.ZERO, 0L))
    val closeRef = AtomicBoolean()
    val conn = StratumConnection(emptyArray(), closeRef::set, ref::set, "foo")
    assertTrue(protocol.canHandle("""{"method": "eth_submitLogin", "id": 1, "params": []}""", conn))
    assertEquals(
      """{"id":1,"jsonrpc":"2.0","result":true}
""",
      ref.get()
    )
    protocol.handle(conn, """{"method": "eth_getWork", "id": 2, "params": []}""")
    assertEquals(
      """{"id":2,"jsonrpc":"2.0","result":["0x0000000000000000000000000000000000000000000000000000000000000000","0x0000000000000000000000000000000000000000000000000000000000000000","0x0000000000000000000000000000000000000000000000000000000000000000"]}
""",
      ref.get()
    )
    protocol.handle(
      conn,
      """{"method": "eth_submitWork", "id": 3, "params": ["${Bytes.random(8)}","${Bytes.random(32)}","${Bytes.random(32)}"]}
"""
    )
    protocol.handle(
      conn,
      """{"method": "eth_submitWork", "id": 4, "params": ["${Bytes.random(8)}","${Bytes.random(32)}","${Bytes.random(32)}"]}
      """.trimMargin()
    )
    protocol.handle(
      conn,
      """{"method": "eth_submitWork", "id": 5, "params": ["${Bytes.random(8)}","${Bytes.random(32)}","${Bytes.random(32)}"]}
      """.trimMargin()
    )
    protocol.handle(
      conn,
      """{"method": "eth_submitWork", "id": 6, "params": ["${Bytes.random(8)}","${Bytes.random(32)}","${Bytes.random(32)}"]}
      """.trimMargin()
    )
    assertTrue(closeRef.get())
  }

  @Test
  fun testBadSolutionsResetOnSuccess() {
    var counter = 0
    val protocol = Stratum1EthProxyProtocol(
      submitCallback = {
        counter++
        counter == 2
      },
      seedSupplier = { Bytes32.ZERO },
      Dispatchers.Default
    )
    val ref = AtomicReference<String>()
    protocol.setCurrentWorkTask(PoWInput(UInt256.ZERO, Bytes32.ZERO, 0L))
    val closeRef = AtomicBoolean()
    val conn = StratumConnection(emptyArray(), closeRef::set, ref::set, "foo")
    assertTrue(protocol.canHandle("""{"method": "eth_submitLogin", "id": 1, "params": []}""", conn))
    assertEquals(
      """{"id":1,"jsonrpc":"2.0","result":true}
""",
      ref.get()
    )
    protocol.handle(conn, """{"method": "eth_getWork", "id": 2, "params": []}""")
    assertEquals(
      """{"id":2,"jsonrpc":"2.0","result":["0x0000000000000000000000000000000000000000000000000000000000000000","0x0000000000000000000000000000000000000000000000000000000000000000","0x0000000000000000000000000000000000000000000000000000000000000000"]}
""",
      ref.get()
    )
    protocol.setCurrentWorkTask(PoWInput(UInt256.ZERO, Bytes32.ZERO, 8L))
    protocol.handle(
      conn,
      """{"method": "eth_submitWork", "id": 3, "params": ["${Bytes.random(8)}","${Bytes32.ZERO}","${Bytes.random(32)}"]}
"""
    )
    protocol.handle(
      conn,
      """{"method": "eth_submitWork", "id": 4, "params": ["${Bytes.random(8)}","${Bytes32.ZERO}","${Bytes.random(32)}"]}
      """.trimMargin()
    )
    protocol.handle(
      conn,
      """{"method": "eth_submitWork", "id": 5, "params": ["${Bytes.random(8)}","${Bytes32.ZERO}","${Bytes.random(32)}"]}
      """.trimMargin()
    )
    protocol.handle(
      conn,
      """{"method": "eth_submitWork", "id": 6, "params": ["${Bytes.random(8)}","${Bytes32.ZERO}","${Bytes.random(32)}"]}
      """.trimMargin()
    )
    assertFalse(closeRef.get())
  }
}
