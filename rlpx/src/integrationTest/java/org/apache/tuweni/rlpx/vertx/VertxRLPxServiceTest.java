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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;
import org.apache.tuweni.rlpx.MemoryWireConnectionsRepository;
import org.apache.tuweni.rlpx.RLPxService;
import org.apache.tuweni.rlpx.wire.DisconnectReason;
import org.apache.tuweni.rlpx.wire.SubProtocol;
import org.apache.tuweni.rlpx.wire.SubProtocolClient;
import org.apache.tuweni.rlpx.wire.SubProtocolHandler;
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier;
import org.apache.tuweni.rlpx.wire.WireConnection;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({VertxExtension.class, BouncyCastleExtension.class})
class VertxRLPxServiceTest {

  private Meter meter = SdkMeterProvider.builder().build().get("vertxAcceptance");

  @Test
  void invalidPort(@VertxInstance Vertx vertx) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new VertxRLPxService(
            vertx,
            -1,
            "localhost",
            30,
            SECP256K1.KeyPair.random(),
            new ArrayList<>(),
            "a",
            meter));
  }

  @Test
  void invalidAdvertisedPort(@VertxInstance Vertx vertx) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new VertxRLPxService(
            vertx,
            3,
            "localhost",
            -1,
            SECP256K1.KeyPair.random(),
            new ArrayList<>(),
            "a",
            meter));
  }

  @Test
  void invalidClientId(@VertxInstance Vertx vertx) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new VertxRLPxService(
            vertx,
            34,
            "localhost",
            23,
            SECP256K1.KeyPair.random(),
            new ArrayList<>(),
            null,
            meter));
  }

  @Test
  void invalidClientIdSpaces(@VertxInstance Vertx vertx) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new VertxRLPxService(
            vertx,
            34,
            "localhost",
            23,
            SECP256K1.KeyPair.random(),
            new ArrayList<>(),
            "   ",
            meter));
  }

  @Test
  void startAndStopService(@VertxInstance Vertx vertx) throws InterruptedException {
    VertxRLPxService service = new VertxRLPxService(
        vertx,
        10000,
        "localhost",
        10000,
        SECP256K1.KeyPair.random(),
        new ArrayList<>(),
        "a",
        meter);

    service.start().join();
    try {
      assertEquals(10000, service.actualPort());
    } finally {
      service.stop();
    }
  }

  @Test
  void startServiceWithPortZero(@VertxInstance Vertx vertx) throws InterruptedException {
    VertxRLPxService service =
        new VertxRLPxService(vertx, 0, "localhost", 0, SECP256K1.KeyPair.random(), new ArrayList<>(), "a", meter);

    service.start().join();
    try {
      assertTrue(service.actualPort() != 0);
      assertEquals(service.actualPort(), service.advertisedPort());
    } finally {
      service.stop();
    }
  }

  @Test
  void stopServiceWithoutStartingItFirst(@VertxInstance Vertx vertx) {
    VertxRLPxService service =
        new VertxRLPxService(vertx, 0, "localhost", 10000, SECP256K1.KeyPair.random(), new ArrayList<>(), "abc", meter);
    AsyncCompletion completion = service.stop();
    assertTrue(completion.isDone());
  }

  @Test
  void connectToOtherPeerWithNoSubProtocols(@VertxInstance Vertx vertx) throws Exception {
    SECP256K1.KeyPair ourPair = SECP256K1.KeyPair.random();
    SECP256K1.KeyPair peerPair = SECP256K1.KeyPair.random();
    VertxRLPxService service =
        new VertxRLPxService(vertx, 0, "localhost", 10000, ourPair, new ArrayList<>(), "abc", meter);
    service.start().join();

    VertxRLPxService peerService =
        new VertxRLPxService(vertx, 0, "localhost", 10000, peerPair, new ArrayList<>(), "abc", meter);
    peerService.start().join();

    WireConnection conn =
        service.connectTo(peerPair.publicKey(), new InetSocketAddress("127.0.0.1", peerService.actualPort())).get();
    assertEquals(DisconnectReason.USELESS_PEER, conn.getDisconnectReason());
    service.stop();
    peerService.stop();
  }

  @Test
  void disconnectAfterStop(@VertxInstance Vertx vertx) throws Exception {
    SECP256K1.KeyPair ourPair = SECP256K1.KeyPair.random();
    SECP256K1.KeyPair peerPair = SECP256K1.KeyPair.random();
    List<SubProtocol> protocols = Arrays.asList(new VertxAcceptanceTest.MyCustomSubProtocol());
    VertxRLPxService service = new VertxRLPxService(vertx, 0, "localhost", 10000, ourPair, protocols, "abc", meter);
    service.start().join();


    VertxRLPxService peerService =
        new VertxRLPxService(vertx, 0, "localhost", 10000, peerPair, protocols, "abc", meter);
    peerService.start().join();

    WireConnection conn = null;
    try {
      conn =
          service.connectTo(peerPair.publicKey(), new InetSocketAddress("127.0.0.1", peerService.actualPort())).get();
    } finally {
      service.stop();
      peerService.stop();
    }
    WireConnection c = conn;
    assertThrows(IllegalStateException.class, () -> {
      service.disconnect(c, DisconnectReason.SUBPROTOCOL_REASON);
    });
  }

  @Test
  void checkWireConnectionCreated(@VertxInstance Vertx vertx) throws Exception {
    SECP256K1.KeyPair ourPair = SECP256K1.KeyPair.random();
    SECP256K1.KeyPair peerPair = SECP256K1.KeyPair.random();

    MemoryWireConnectionsRepository repository = new MemoryWireConnectionsRepository();
    AtomicBoolean called = new AtomicBoolean();
    repository.addConnectionListener(conn -> called.set(true));
    List<SubProtocol> protocols = Collections.singletonList(new SubProtocol() {
      @Override
      public SubProtocolIdentifier id() {
        return SubProtocolIdentifier.of("eth", 63, 17);
      }

      @Override
      public boolean supports(SubProtocolIdentifier subProtocolIdentifier) {
        return false;
      }

      @Override
      public SubProtocolHandler createHandler(RLPxService service, SubProtocolClient client) {
        SubProtocolHandler handler = mock(SubProtocolHandler.class);
        when(handler.handleNewPeerConnection(any())).thenReturn(AsyncCompletion.COMPLETED);
        when(handler.stop()).thenReturn(AsyncCompletion.COMPLETED);
        return handler;
      }

      @Override
      public SubProtocolClient createClient(RLPxService service, SubProtocolIdentifier identifier) {
        return mock(SubProtocolClient.class);
      }
    });
    VertxRLPxService service =
        new VertxRLPxService(vertx, 0, "localhost", 10000, ourPair, protocols, "abc", meter, repository);
    service.start().join();

    MemoryWireConnectionsRepository peerRepository = new MemoryWireConnectionsRepository();
    VertxRLPxService peerService =
        new VertxRLPxService(vertx, 0, "localhost", 10000, peerPair, protocols, "abc", meter, peerRepository);
    peerService.start().join();

    try {
      WireConnection conn =
          service.connectTo(peerPair.publicKey(), new InetSocketAddress("localhost", peerService.actualPort())).get();
      assertNotNull(conn);
      assertEquals(1, repository.asMap().size());

      AtomicBoolean disconnect = new AtomicBoolean();
      repository.addDisconnectionListener(c -> disconnect.set(true));
      service.disconnect(conn, DisconnectReason.CLIENT_QUITTING);
      assertTrue(disconnect.get());

    } finally {
      service.stop();
      peerService.stop();
    }
  }

  @Test
  void getClientWhenNotReady(@VertxInstance Vertx vertx) {
    SECP256K1.KeyPair peerPair = SECP256K1.KeyPair.random();
    MemoryWireConnectionsRepository peerRepository = new MemoryWireConnectionsRepository();
    VertxRLPxService peerService =
        new VertxRLPxService(vertx, 0, "localhost", 10000, peerPair, new ArrayList<>(), "abc", meter, peerRepository);
    assertThrows(IllegalStateException.class, () -> {
      peerService.getClient(SubProtocolIdentifier.of("foo", 1));
    });
  }

  @Test
  void getClientWeCreate(@VertxInstance Vertx vertx) throws Exception {
    SubProtocol sp = mock(SubProtocol.class);
    SubProtocolClient client = mock(SubProtocolClient.class);
    when(sp.id()).thenReturn(SubProtocolIdentifier.of("foo", 1, 2));
    when(sp.getCapabilities()).thenReturn(Arrays.asList(SubProtocolIdentifier.of("foo", 1, 2)));
    when(sp.createClient(any(), any())).thenReturn(client);
    MemoryWireConnectionsRepository peerRepository = new MemoryWireConnectionsRepository();
    VertxRLPxService peerService = new VertxRLPxService(
        vertx,
        0,
        "localhost",
        10000,
        SECP256K1.KeyPair.random(),
        Collections.singletonList(sp),
        "abc",
        meter,
        peerRepository);
    peerService.start().join();
    assertNull(peerService.getClient(SubProtocolIdentifier.of("foo", 2)));
    assertNull(peerService.getClient(SubProtocolIdentifier.of("bar", 1)));
    assertEquals(client, peerService.getClient(SubProtocolIdentifier.of("foo", 1)));
  }
}
