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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.rlpx.RLPxConnection;
import org.apache.tuweni.rlpx.RLPxConnectionFactory;
import org.apache.tuweni.rlpx.RLPxMessage;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class RLPxConnectionMessageExchangeTest {

  @Test
  void exchangeHello() throws Exception {
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.random();
    SECP256K1.KeyPair peerKeyPair = SECP256K1.KeyPair.random();

    AtomicReference<RLPxConnection> peerConnectionReference = new AtomicReference<>();

    Function<Bytes, AsyncResult<Bytes>> wireBytes = (bytes) -> {
      AtomicReference<Bytes> responseReference = new AtomicReference<>();
      peerConnectionReference.set(RLPxConnectionFactory.respondToHandshake(bytes, peerKeyPair, responseReference::set));
      return AsyncResult.completed(responseReference.get());
    };
    AsyncResult<RLPxConnection> futureConn =
        RLPxConnectionFactory.createHandshake(keyPair, peerKeyPair.publicKey(), wireBytes);

    RLPxConnection peerConn = peerConnectionReference.get();
    RLPxConnection conn = futureConn.get(1, TimeUnit.SECONDS);
    assertTrue(RLPxConnection.isComplementedBy(conn, peerConn));

    HelloMessage message =
        HelloMessage.create(Bytes.of(1, 2, 3), 30303, 28, "ClientID 1.0", Arrays.asList(new Capability("eth", 63)));
    RLPxMessage messageToWrite = new RLPxMessage(0, message.toBytes());
    Bytes messageBytes = peerConn.write(messageToWrite);
    RLPxMessage readMessage = conn.readFrame(messageBytes);
    assertEquals(messageToWrite, readMessage);
  }

  @Test
  void exchangeHelloAndSomeMoreMessagesWithCompression() throws Exception {
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.random();
    SECP256K1.KeyPair peerKeyPair = SECP256K1.KeyPair.random();

    AtomicReference<RLPxConnection> peerConnectionReference = new AtomicReference<>();

    Function<Bytes, AsyncResult<Bytes>> wireBytes = (bytes) -> {
      AtomicReference<Bytes> responseReference = new AtomicReference<>();
      peerConnectionReference.set(RLPxConnectionFactory.respondToHandshake(bytes, peerKeyPair, responseReference::set));
      return AsyncResult.completed(responseReference.get());
    };
    AsyncResult<RLPxConnection> futureConn =
        RLPxConnectionFactory.createHandshake(keyPair, peerKeyPair.publicKey(), wireBytes);

    RLPxConnection peerConn = peerConnectionReference.get();
    RLPxConnection conn = futureConn.get(1, TimeUnit.SECONDS);
    assertTrue(RLPxConnection.isComplementedBy(conn, peerConn));

    HelloMessage hello =
        HelloMessage.create(Bytes.of(1, 2, 3), 30303, 5, "ClientID 1.0", Arrays.asList(new Capability("eth", 63)));

    conn.configureAfterHandshake(hello);
    peerConn.configureAfterHandshake(hello);

    Bytes message = conn.write(new RLPxMessage(23, Bytes.fromHexString("deadbeef")));
    RLPxMessage readMessage = peerConn.readFrame(message);
    assertEquals(Bytes.fromHexString("deadbeef"), readMessage.content());
  }

  @Test
  void exchangeHelloAndSomeMoreMessagesWithoutCompression() throws Exception {
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.random();
    SECP256K1.KeyPair peerKeyPair = SECP256K1.KeyPair.random();

    AtomicReference<RLPxConnection> peerConnectionReference = new AtomicReference<>();

    Function<Bytes, AsyncResult<Bytes>> wireBytes = (bytes) -> {
      AtomicReference<Bytes> responseReference = new AtomicReference<>();
      peerConnectionReference.set(RLPxConnectionFactory.respondToHandshake(bytes, peerKeyPair, responseReference::set));
      return AsyncResult.completed(responseReference.get());
    };
    AsyncResult<RLPxConnection> futureConn =
        RLPxConnectionFactory.createHandshake(keyPair, peerKeyPair.publicKey(), wireBytes);

    RLPxConnection peerConn = peerConnectionReference.get();
    RLPxConnection conn = futureConn.get(1, TimeUnit.SECONDS);
    assertTrue(RLPxConnection.isComplementedBy(conn, peerConn));

    HelloMessage hello =
        HelloMessage.create(Bytes.of(1, 2, 3), 30303, 4, "ClientID 1.0", Arrays.asList(new Capability("eth", 63)));

    conn.configureAfterHandshake(hello);
    peerConn.configureAfterHandshake(hello);

    Bytes message = conn.write(new RLPxMessage(23, Bytes.fromHexString("deadbeef")));
    RLPxMessage readMessage = peerConn.readFrame(message);
    assertEquals(Bytes.fromHexString("deadbeef"), readMessage.content());
  }
}
