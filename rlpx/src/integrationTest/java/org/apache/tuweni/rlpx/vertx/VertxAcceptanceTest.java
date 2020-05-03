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
package org.apache.tuweni.rlpx.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.CompletableAsyncCompletion;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;
import org.apache.tuweni.rlpx.MemoryWireConnectionsRepository;
import org.apache.tuweni.rlpx.RLPxService;
import org.apache.tuweni.rlpx.wire.DefaultWireConnection;
import org.apache.tuweni.rlpx.wire.SubProtocol;
import org.apache.tuweni.rlpx.wire.SubProtocolHandler;
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({VertxExtension.class, BouncyCastleExtension.class})
class VertxAcceptanceTest {

  private static class MyCustomSubProtocolHandler implements SubProtocolHandler {

    public final List<Bytes> messages = new ArrayList<>();

    private final RLPxService rlpxService;
    private final SubProtocolIdentifier identifier;

    public MyCustomSubProtocolHandler(RLPxService rlpxService, SubProtocolIdentifier identifier) {
      this.rlpxService = rlpxService;
      this.identifier = identifier;
    }

    @Override
    public AsyncCompletion handle(String connectionId, int messageType, Bytes message) {
      messages.add(message);
      return AsyncCompletion.completed();
    }

    @Override
    public AsyncCompletion handleNewPeerConnection(String connId) {
      rlpxService.send(identifier, 0, connId, Bytes.fromHexString("deadbeef"));
      return AsyncCompletion.completed();
    }

    @Override
    public AsyncCompletion stop() {
      return AsyncCompletion.completed();
    }
  }

  private static class MyCustomSubProtocol implements SubProtocol {

    private final int i;

    public MyCustomSubProtocol(int i) {
      this.i = i;
    }

    public MyCustomSubProtocolHandler handler;

    @Override
    public SubProtocolIdentifier id() {
      return SubProtocolIdentifier.of("cus", 1);
    }

    @Override
    public boolean supports(SubProtocolIdentifier subProtocolIdentifier) {
      return "cus".equals(subProtocolIdentifier.name()) && 1 == subProtocolIdentifier.version();
    }

    @Override
    public int versionRange(int version) {
      return 1;
    }

    @Override
    public SubProtocolHandler createHandler(RLPxService service) {
      handler = new MyCustomSubProtocolHandler(service, id());
      return handler;
    }
  }

  @Test
  void testTwoServicesSendingMessagesOfCustomSubProtocolToEachOther(@VertxInstance Vertx vertx) throws Exception {
    SECP256K1.KeyPair kp = SECP256K1.KeyPair.random();
    SECP256K1.KeyPair secondKp = SECP256K1.KeyPair.random();
    MyCustomSubProtocol sp = new MyCustomSubProtocol(1);
    MyCustomSubProtocol secondSp = new MyCustomSubProtocol(2);
    MemoryWireConnectionsRepository repository = new MemoryWireConnectionsRepository();
    VertxRLPxService service =
        new VertxRLPxService(vertx, 0, "localhost", 10000, kp, Collections.singletonList(sp), "Client 1", repository);
    MemoryWireConnectionsRepository secondRepository = new MemoryWireConnectionsRepository();

    VertxRLPxService secondService = new VertxRLPxService(
        vertx,
        0,
        "localhost",
        10000,
        secondKp,
        Collections.singletonList(secondSp),
        "Client 2",
        secondRepository);
    service.start().join();
    secondService.start().join();

    try {
      service.connectTo(secondKp.publicKey(), new InetSocketAddress("localhost", secondService.actualPort()));

      Thread.sleep(3000);
      assertEquals(1, repository.asMap().size());
      assertEquals(1, secondRepository.asMap().size());

      assertEquals(1, sp.handler.messages.size());
      assertEquals(1, secondSp.handler.messages.size());

      AsyncCompletion completion = ((DefaultWireConnection) repository.asMap().values().iterator().next()).sendPing();
      completion.join();
      assertTrue(completion.isDone());
    } finally {
      AsyncCompletion.allOf(service.stop(), secondService.stop());
    }
  }

  @Test
  void testTwoServicesSendingMessagesOfCustomSubProtocolToEachOtherSimultaneously(@VertxInstance Vertx vertx)
      throws Exception {
    SECP256K1.KeyPair kp = SECP256K1.KeyPair.random();
    SECP256K1.KeyPair secondKp = SECP256K1.KeyPair.random();
    MyCustomSubProtocol sp = new MyCustomSubProtocol(1);
    MyCustomSubProtocol secondSp = new MyCustomSubProtocol(2);
    MemoryWireConnectionsRepository repository = new MemoryWireConnectionsRepository();
    MemoryWireConnectionsRepository secondRepository = new MemoryWireConnectionsRepository();

    VertxRLPxService service =
        new VertxRLPxService(vertx, 0, "localhost", 10000, kp, Collections.singletonList(sp), "Client 1", repository);
    VertxRLPxService secondService = new VertxRLPxService(
        vertx,
        0,
        "localhost",
        10000,
        secondKp,
        Collections.singletonList(secondSp),
        "Client 2",
        secondRepository);
    service.start().join();
    secondService.start().join();

    try {
      service.connectTo(secondKp.publicKey(), new InetSocketAddress("localhost", secondService.actualPort()));

      Thread.sleep(3000);
      assertEquals(1, repository.asMap().size());
      assertEquals(1, secondRepository.asMap().size());

      assertEquals(1, sp.handler.messages.size());
      assertEquals(1, secondSp.handler.messages.size());

      List<AsyncCompletion> completionList = new ArrayList<>();
      ExecutorService threadPool = Executors.newFixedThreadPool(16);
      for (int i = 0; i < 128; i++) {
        CompletableAsyncCompletion task = AsyncCompletion.incomplete();
        completionList.add(task);
        threadPool.submit(() -> {
          try {

            ((DefaultWireConnection) repository.asMap().values().iterator().next()).sendPing();
            task.complete();
          } catch (Throwable t) {
            task.completeExceptionally(t);
          }
        });
      }
      threadPool.shutdown();

      AsyncCompletion allTasks = AsyncCompletion.allOf(completionList);
      allTasks.join(30, TimeUnit.SECONDS);
      assertTrue(allTasks.isDone());

    } finally {
      AsyncCompletion.allOf(service.stop(), secondService.stop());
    }
  }

  @Test
  @Disabled
  void connectToPeer(@VertxInstance Vertx vertx) throws Exception {


    SECP256K1.KeyPair kp = SECP256K1.KeyPair
        .fromSecretKey(
            SECP256K1.SecretKey
                .fromBytes(
                    Bytes32.fromHexString("0x2CADB9DDEA3E675CC5349A1AF053CF2E144AF657016A6155DF4AD767F561F18E")));

    MemoryWireConnectionsRepository repository = new MemoryWireConnectionsRepository();

    VertxRLPxService service =
        new VertxRLPxService(vertx, 36000, "localhost", 36000, kp, Collections.singletonList(new SubProtocol() {
          @Override
          public SubProtocolIdentifier id() {
            return new SubProtocolIdentifier() {
              @Override
              public String name() {
                return "eth";
              }

              @Override
              public int version() {
                return 63;
              }
            };
          }

          @Override
          public boolean supports(SubProtocolIdentifier subProtocolIdentifier) {
            return false;
          }

          @Override
          public int versionRange(int version) {
            return 0;
          }

          @Override
          public SubProtocolHandler createHandler(RLPxService service) {
            return null;
          }
        }), "Client 1", repository);
    service.start().join();

    AsyncCompletion completion = service
        .connectTo(
            SECP256K1.PublicKey
                .fromHexString(
                    "7a8fbb31bff7c48179f8504b047313ebb7446a0233175ffda6eb4c27aaa5d2aedcef4dd9501b4f17b4f16588f0fd037f9b9416b8caca655bee3b14b4ef67441a"),
            new InetSocketAddress("localhost", 30303));
    completion.join();
    Thread.sleep(10000);

    service.stop().join();
  }
}
