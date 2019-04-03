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
package org.apache.tuweni.scuttlebutt.handshake.vertx;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.crypto.sodium.Sodium;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec;
import org.apache.tuweni.scuttlebutt.rpc.RPCFlag;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logl.Level;
import org.logl.LoggerProvider;
import org.logl.logl.SimpleLogger;
import org.logl.vertx.LoglLogDelegateFactory;


@ExtendWith(VertxExtension.class)
class VertxIntegrationTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  private static class MyClientHandler implements ClientHandler {


    private final Consumer<Bytes> sender;
    private final Runnable terminationFn;

    public MyClientHandler(Consumer<Bytes> sender, Runnable terminationFn) {
      this.sender = sender;
      this.terminationFn = terminationFn;
    }

    @Override
    public void receivedMessage(Bytes message) {

    }

    @Override
    public void streamClosed() {

    }

    void sendMessage(Bytes bytes) {
      sender.accept(bytes);
    }

    void closeStream() {
      terminationFn.run();
    }
  }

  private static class MyServerHandler implements ServerHandler {

    boolean closed = false;
    Bytes received = null;

    @Override
    public void receivedMessage(Bytes message) {
      received = message;
    }

    @Override
    public void streamClosed() {
      closed = true;
    }
  }

  @Test
  void connectToServer(@VertxInstance Vertx vertx) throws Exception {
    LoggerProvider provider = SimpleLogger.withLogLevel(Level.DEBUG).toPrintWriter(
        new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, UTF_8))));
    LoglLogDelegateFactory.setProvider(provider);
    Signature.KeyPair serverKeyPair = Signature.KeyPair.random();
    Bytes32 networkIdentifier = Bytes32.random();
    AtomicReference<MyServerHandler> serverHandlerRef = new AtomicReference<>();
    SecureScuttlebuttVertxServer server = new SecureScuttlebuttVertxServer(
        vertx,
        new InetSocketAddress("0.0.0.0", 20000),
        serverKeyPair,
        networkIdentifier,
        (streamServer, fn) -> {
          serverHandlerRef.set(new MyServerHandler());
          return serverHandlerRef.get();
        });

    server.start().join();

    SecureScuttlebuttVertxClient client =
        new SecureScuttlebuttVertxClient(provider, vertx, Signature.KeyPair.random(), networkIdentifier);
    MyClientHandler handler =
        (MyClientHandler) client.connectTo(20000, "0.0.0.0", serverKeyPair.publicKey(), MyClientHandler::new).get();

    Thread.sleep(1000);
    assertNotNull(handler);

    String rpcRequestBody = "{\"name\": [\"whoami\"],\"type\": \"async\",\"args\":[]}";
    Bytes rpcRequest = RPCCodec.encodeRequest(rpcRequestBody, RPCFlag.BodyType.JSON);

    handler.sendMessage(rpcRequest);

    Thread.sleep(1000);
    MyServerHandler serverHandler = serverHandlerRef.get();

    Bytes receivedBytes = serverHandler.received;
    Bytes receivedBody = receivedBytes.slice(9);

    Bytes requestBody = rpcRequest.slice(9);

    assertEquals(requestBody, receivedBody);

    handler.closeStream();
    Thread.sleep(1000);
    assertTrue(serverHandler.closed);

    client.stop();
    server.stop();

  }

}
