/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.ethstats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.eth.Address;
import org.apache.tuweni.eth.Hash;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;
import org.apache.tuweni.units.bigints.UInt256;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class EthStatsReporterTest {

  @Test
  void testConnectToLocalEthStats(@VertxInstance Vertx vertx) throws InterruptedException {

    Instant now = Instant.EPOCH;
    FakeEthStatsServer server = new FakeEthStatsServer(vertx, "127.0.0.1", 0);
    EthStatsReporter reporter = new EthStatsReporter(
        vertx,
        "foo",
        Collections.singletonList(URI.create("ws://localhost:" + server.getPort() + "/api")),
        "wat",
        "name",
        "node",
        33030,
        "10",
        "eth/63",
        "Windoz",
        "64",
        (blockNumbers) -> {
        },
        now::toEpochMilli);

    AsyncResult<String> nextMessage = server.captureNextMessage();
    reporter.start().join();
    assertNotNull(server.getWebsocket());
    assertEquals(
        "{\"emit\":[\"hello\",{\"id\":\"foo\",\"info\":{\"api\":\"No\",\"canUpdateHistory\":true,\"client\":\"0.1.0\",\"name\":\"name\",\"net\":\"10\",\"node\":\"node\",\"os\":\"Windoz\",\"os_v\":\"64\",\"port\":33030,\"protocol\":\"eth/63\"},\"secret\":\"wat\"}]}",
        nextMessage.get());

    nextMessage = server.captureNextMessage();
    reporter
        .sendNewHead(
            new BlockStats(
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
                Collections.emptyList()));
    assertTrue(nextMessage.get().startsWith("{\"emit\":[\"block\",{\"block\""), nextMessage.get());
    nextMessage = server.captureNextMessage();
    reporter.sendNewNodeStats(new NodeStats(true, false, true, 42, 9, 4000, 100));
    assertTrue(nextMessage.get().startsWith("{\"emit\":[\"stats\",{\"stats\":"), nextMessage.get());
    nextMessage = server.captureNextMessage();
    reporter.sendNewPendingTransactionCount(42);
    assertEquals("{\"emit\":[\"pending\",{\"stats\":{\"pending\":42},\"id\":\"foo\"}]}", nextMessage.get());

    reporter.stop();
  }
}
