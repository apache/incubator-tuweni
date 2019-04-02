/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.rlpx.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.rlpx.RLPxMessage;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logl.LoggerProvider;

@ExtendWith(BouncyCastleExtension.class)
class PingPongTest {

  private static final Bytes nodeId = SECP256K1.KeyPair.random().publicKey().bytes();
  private static final Bytes peerNodeId = SECP256K1.KeyPair.random().publicKey().bytes();

  @Test
  void pingPongRoundtrip() {
    AtomicReference<RLPxMessage> capturedPing = new AtomicReference<>();
    DefaultWireConnection conn = new DefaultWireConnection(
        "abc",
        nodeId,
        peerNodeId,
        LoggerProvider.nullProvider().getLogger("rlpx"),
        capturedPing::set,

        helloMessage -> {
        },
        () -> {
        },
        new LinkedHashMap<>(),
        2,
        "abc",
        10000);

    AsyncCompletion completion = conn.sendPing();
    assertFalse(completion.isDone());
    assertNotNull(capturedPing.get());

    conn.messageReceived(new RLPxMessage(3, Bytes.EMPTY));
    assertTrue(completion.isDone());
  }

  @Test
  void pongPingRoundtrip() {
    AtomicReference<RLPxMessage> capturedPong = new AtomicReference<>();
    DefaultWireConnection conn = new DefaultWireConnection(
        "abc",
        nodeId,
        peerNodeId,
        LoggerProvider.nullProvider().getLogger("rlpx"),
        capturedPong::set,
        helloMessage -> {
        },
        () -> {
        },
        new LinkedHashMap<>(),
        1,
        "abc",
        10000);

    conn.messageReceived(new RLPxMessage(2, Bytes.EMPTY));
    assertNotNull(capturedPong.get());
    assertEquals(3, capturedPong.get().messageId());
  }
}
