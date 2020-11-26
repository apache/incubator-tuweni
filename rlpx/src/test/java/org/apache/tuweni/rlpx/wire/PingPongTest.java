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
package org.apache.tuweni.rlpx.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.rlpx.RLPxMessage;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class PingPongTest {

  private static final Bytes nodeId = SECP256K1.KeyPair.random().publicKey().bytes();
  private static final Bytes peerNodeId = SECP256K1.KeyPair.random().publicKey().bytes();

  @Test
  void pingPongRoundtrip() {
    AtomicReference<RLPxMessage> capturedPing = new AtomicReference<>();
    DefaultWireConnection conn = new DefaultWireConnection(
        nodeId,
        peerNodeId,
        capturedPing::set,

        helloMessage -> {
        },
        () -> {
        },
        new LinkedHashMap<>(),
        2,
        "abc",
        10000,
        AsyncResult.incomplete(),
        "127.0.0.1",
        1234);
    conn.registerListener(event -> {
    });

    AsyncCompletion completion = conn.sendPing();
    assertFalse(completion.isDone());
    assertNotNull(capturedPing.get());

    conn.messageReceived(new RLPxMessage(3, Bytes.EMPTY));
    assertTrue(completion.isDone());
  }

  @Test
  void pongPingRoundtrip() {
    AtomicReference<RLPxMessage> capturedPong = new AtomicReference<>();
    DefaultWireConnection conn = new DefaultWireConnection(nodeId, peerNodeId, capturedPong::set, helloMessage -> {
    }, () -> {
    }, new LinkedHashMap<>(), 1, "abc", 10000, AsyncResult.incomplete(), "127.0.0.1", 1234);
    conn.registerListener(event -> {
    });
    conn.messageReceived(new RLPxMessage(2, Bytes.EMPTY));
    assertNotNull(capturedPong.get());
    assertEquals(3, capturedPong.get().messageId());
  }
}
