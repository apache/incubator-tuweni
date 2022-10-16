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
package org.apache.tuweni.ethstats

import io.vertx.core.Vertx
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.apache.tuweni.units.bigints.UInt256
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI
import java.time.Instant
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference

@Disabled("flaky")
@ExtendWith(VertxExtension::class)
public class EthStatsReporterTest {

  @Test
  fun testConnectToLocalEthStats(@VertxInstance vertx: Vertx) = runBlocking {
    val now = Instant.EPOCH
    val server = FakeEthStatsServer(vertx, "127.0.0.1", 0)
    val reporter = EthStatsReporter(
      vertx,
      "foo",
      Collections.singletonList(URI.create("ws://localhost:" + server.port + "/api")),
      "wat",
      "name",
      "node",
      33030,
      "10",
      "eth/63",
      "Windoz",
      "64",
      { },
      { now }
    )

    for (i in 1..3) {
      try {
        reporter.start()
        break
      } catch (e: Exception) {
        delay(100)
      }
    }

    reporter
      .sendNewHead(
        BlockStats(
          UInt256.ONE,
          Hash.fromBytes(Bytes32.random()),
          Hash.fromBytes(Bytes32.random()),
          3L,
          Address.fromBytes(Bytes.random(20)),
          42L,
          43,
          UInt256.valueOf(42L),
          UInt256.valueOf(84L),
          Collections.emptyList(),
          Hash.fromBytes(Bytes32.random()),
          Hash.fromBytes(Bytes32.random()),
          Collections.emptyList()
        )
      )

    reporter.sendNewNodeStats(NodeStats(true, false, true, 42, 9, 4000, 100))
    reporter.sendNewPendingTransactionCount(42)
    server.waitForMessages(4)
    assertTrue(
      server
        .messagesContain(
          "{\"emit\":[\"hello\",{\"id\":\"foo\",\"info\":{\"api\":\"No\",\"canUpdateHistory\":true,\"client\":\"Apache Tuweni Ethstats\",\"name\":\"name\",\"net\":\"10\",\"node\":\"node\",\"os\":\"Windoz\",\"os_v\":\"64\",\"port\":33030,\"protocol\":\"eth/63\"},\"secret\":\"wat\"}]}"
        )
    )
    assertTrue(server.messagesContain("{\"emit\":[\"node-ping\",{\"id\":\"foo\""))
    assertTrue(server.messagesContain("{\"emit\":[\"block\","), server.getResults().joinToString("\n"))
    assertTrue(server.messagesContain("\"stats\":{\"pending\":42}"), server.getResults().joinToString("\n"))
    assertTrue(server.messagesContain("{\"emit\":[\"stats\",{\"id\":\"foo\",\"stats\":"), server.getResults().joinToString("\n"))

    reporter.stop()
  }

  @Test
  fun testServer(@VertxInstance vertx: Vertx) = runBlocking {
    val now = Instant.EPOCH
    val nodeInfoReference = AtomicReference<NodeInfo>()
    val controller = object : EthStatsServerController {
      override fun readNodeInfo(remoteAddress: String, id: String, nodeInfo: NodeInfo) {
        nodeInfoReference.set(nodeInfo)
      }
    }
    val server = EthStatsServer(
      vertx,
      "127.0.0.1",
      33030,
      "wat",
      { now },
      controller
    )
    server.start()
    val reporter = EthStatsReporter(
      vertx,
      "foo",
      Collections.singletonList(URI.create("ws://localhost:" + server.port + "/api")),
      "wat",
      "name",
      "node",
      33030,
      "10",
      "eth/63",
      "Windoz",
      "64",
      { },
      { now }
    )

    reporter.start()

    reporter
      .sendNewHead(
        BlockStats(
          UInt256.ONE,
          Hash.fromBytes(Bytes32.random()),
          Hash.fromBytes(Bytes32.random()),
          3L,
          Address.fromBytes(Bytes.random(20)),
          42L,
          43,
          UInt256.valueOf(42L),
          UInt256.valueOf(84L),
          Collections.emptyList(),
          Hash.fromBytes(Bytes32.random()),
          Hash.fromBytes(Bytes32.random()),
          Collections.emptyList()
        )
      )

    reporter.sendNewNodeStats(NodeStats(true, false, true, 42, 9, 4000, 100))
    reporter.sendNewPendingTransactionCount(42)
    delay(5000)
    assertNotNull(nodeInfoReference.get())

    reporter.stop()
  }
}
