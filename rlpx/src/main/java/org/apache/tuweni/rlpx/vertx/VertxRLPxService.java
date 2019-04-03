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

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.CompletableAsyncCompletion;
import org.apache.tuweni.crypto.SECP256K1.KeyPair;
import org.apache.tuweni.crypto.SECP256K1.PublicKey;
import org.apache.tuweni.rlpx.HandshakeMessage;
import org.apache.tuweni.rlpx.MemoryWireConnectionsRepository;
import org.apache.tuweni.rlpx.RLPxConnection;
import org.apache.tuweni.rlpx.RLPxConnectionFactory;
import org.apache.tuweni.rlpx.RLPxService;
import org.apache.tuweni.rlpx.WireConnectionRepository;
import org.apache.tuweni.rlpx.wire.DefaultWireConnection;
import org.apache.tuweni.rlpx.wire.DisconnectReason;
import org.apache.tuweni.rlpx.wire.SubProtocol;
import org.apache.tuweni.rlpx.wire.SubProtocolHandler;
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier;
import org.apache.tuweni.rlpx.wire.WireConnection;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import org.logl.Logger;
import org.logl.LoggerProvider;

/**
 * Implementation of RLPx service using Vert.x.
 */
public final class VertxRLPxService implements RLPxService {

  private final static int DEVP2P_VERSION = 5;

  private final LoggerProvider loggerProvider;
  private final Logger logger;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final Vertx vertx;
  private final int listenPort;
  private final String networkInterface;
  private final int advertisedPort;
  private final KeyPair keyPair;
  private final List<SubProtocol> subProtocols;
  private final String clientId;

  private LinkedHashMap<SubProtocol, SubProtocolHandler> handlers;
  private NetClient client;
  private NetServer server;

  private final WireConnectionRepository repository;

  private static void checkPort(int port) {
    if (port < 0 || port > 65536) {
      throw new IllegalArgumentException("Invalid port: " + port);
    }
  }

  /**
   * Default constructor.
   *
   * @param vertx Vert.x object used to build the network components
   * @param loggerProvider logger provider to log messages
   * @param listenPort the port to listen to
   * @param networkInterface the network interface to bind to
   * @param advertisedPort the port to advertise in HELLO messages to peers
   * @param identityKeyPair the identity of this client
   * @param subProtocols subprotocols supported
   * @param clientId the client identifier, such as "RLPX 1.2/build 389"
   */
  public VertxRLPxService(
      Vertx vertx,
      LoggerProvider loggerProvider,
      int listenPort,
      String networkInterface,
      int advertisedPort,
      KeyPair identityKeyPair,
      List<SubProtocol> subProtocols,
      String clientId) {
    this(
        vertx,
        loggerProvider,
        listenPort,
        networkInterface,
        advertisedPort,
        identityKeyPair,
        subProtocols,
        clientId,
        new MemoryWireConnectionsRepository());
  }

  /**
   * Default constructor.
   *
   * @param vertx Vert.x object used to build the network components
   * @param loggerProvider logger provider to log messages
   * @param listenPort the port to listen to
   * @param networkInterface the network interface to bind to
   * @param advertisedPort the port to advertise in HELLO messages to peers
   * @param identityKeyPair the identity of this client
   * @param subProtocols subprotocols supported
   * @param clientId the client identifier, such as "RLPX 1.2/build 389"
   * @param repository a wire connection repository
   */
  public VertxRLPxService(
      Vertx vertx,
      LoggerProvider loggerProvider,
      int listenPort,
      String networkInterface,
      int advertisedPort,
      KeyPair identityKeyPair,
      List<SubProtocol> subProtocols,
      String clientId,
      WireConnectionRepository repository) {
    checkPort(listenPort);
    checkPort(advertisedPort);
    if (clientId == null || clientId.trim().isEmpty()) {
      throw new IllegalArgumentException("Client ID must contain a valid identifier");
    }
    this.vertx = vertx;
    this.loggerProvider = loggerProvider;
    this.listenPort = listenPort;
    this.networkInterface = networkInterface;
    this.advertisedPort = advertisedPort;
    this.keyPair = identityKeyPair;
    this.subProtocols = subProtocols;
    this.clientId = clientId;
    this.repository = repository;

    this.logger = loggerProvider.getLogger("VertxRLPxService");
  }

  @Override
  public AsyncCompletion start() {
    if (started.compareAndSet(false, true)) {
      handlers = new LinkedHashMap<SubProtocol, SubProtocolHandler>();
      for (SubProtocol subProtocol : subProtocols) {
        handlers.put(subProtocol, subProtocol.createHandler(this));
      }
      client = vertx.createNetClient(new NetClientOptions());
      server = vertx
          .createNetServer(new NetServerOptions().setPort(listenPort).setHost(networkInterface).setTcpKeepAlive(true))
          .connectHandler(this::receiveMessage);
      CompletableAsyncCompletion complete = AsyncCompletion.incomplete();
      server.listen(res -> {
        if (res.succeeded()) {
          complete.complete();
        } else {
          complete.completeExceptionally(res.cause());
        }
      });
      return complete;
    } else {
      return AsyncCompletion.completed();
    }
  }

  @Override
  public void send(SubProtocolIdentifier subProtocolIdentifier, int messageType, String connectionId, Bytes message) {
    if (!started.get()) {
      throw new IllegalStateException("The RLPx service is not active");
    }
    DefaultWireConnection conn = wireConnection(connectionId);
    if (conn != null) {
      conn.sendMessage(subProtocolIdentifier, messageType, message);
    }
  }

  @Override
  public void disconnect(String connectionId, DisconnectReason disconnectReason) {
    if (!started.get()) {
      throw new IllegalStateException("The RLPx service is not active");
    }
    DefaultWireConnection conn = wireConnection(connectionId);
    if (conn != null) {
      conn.disconnect(disconnectReason);
    }
  }

  @Override
  public void broadcast(SubProtocolIdentifier subProtocolIdentifier, int messageType, Bytes message) {
    if (!started.get()) {
      throw new IllegalStateException("The RLPx service is not active");
    }
    for (WireConnection conn : repository.asIterable()) {
      ((DefaultWireConnection) conn).sendMessage(subProtocolIdentifier, messageType, message);
    }
  }

  private void receiveMessage(NetSocket netSocket) {
    netSocket.handler(new Handler<Buffer>() {

      private RLPxConnection conn;

      private DefaultWireConnection wireConnection;

      @Override
      public void handle(Buffer buffer) {
        if (conn == null) {
          conn = RLPxConnectionFactory.respondToHandshake(
              Bytes.wrapBuffer(buffer),
              keyPair,
              bytes -> netSocket.write(Buffer.buffer(bytes.toArrayUnsafe())));
          if (wireConnection == null) {
            this.wireConnection = createConnection(conn, netSocket);
            wireConnection.handleConnectionStart();
          }
        } else {
          conn.stream(Bytes.wrapBuffer(buffer), wireConnection::messageReceived);
        }
      }
    });
  }

  @Override
  public AsyncCompletion stop() {
    if (started.compareAndSet(true, false)) {
      for (WireConnection conn : repository.asIterable()) {
        ((DefaultWireConnection) conn).disconnect(DisconnectReason.CLIENT_QUITTING);
      }
      repository.close();
      client.close();

      AsyncCompletion handlersCompletion =
          AsyncCompletion.allOf(handlers.values().stream().map(SubProtocolHandler::stop).collect(Collectors.toList()));

      CompletableAsyncCompletion completableAsyncCompletion = AsyncCompletion.incomplete();
      server.close(res -> {
        if (res.succeeded()) {
          completableAsyncCompletion.complete();
        } else {
          completableAsyncCompletion.completeExceptionally(res.cause());
        }
      });
      return handlersCompletion.thenCombine(completableAsyncCompletion);
    } else {
      return AsyncCompletion.completed();
    }
  }

  /**
   *
   * @return the port used by the server
   * @throws IllegalStateException if the service is not started
   */
  public int actualPort() {
    if (!started.get()) {
      throw new IllegalStateException("The RLPx service is not active");
    }
    return server.actualPort();
  }

  /**
   *
   * @return the port advertised by the server
   * @throws IllegalStateException if the service is not started
   */
  public int advertisedPort() {
    if (!started.get()) {
      throw new IllegalStateException("The RLPx service is not active");
    }
    return listenPort == 0 ? actualPort() : advertisedPort;
  }

  @Override
  public WireConnectionRepository repository() {
    return repository;
  }

  @Override
  public AsyncCompletion connectTo(PublicKey peerPublicKey, InetSocketAddress peerAddress) {
    if (!started.get()) {
      throw new IllegalStateException("The RLPx service is not active");
    }
    CompletableAsyncCompletion connected = AsyncCompletion.incomplete();
    logger.debug("Connecting to {} with public key {}", peerAddress, peerPublicKey);
    client.connect(
        peerAddress.getPort(),
        peerAddress.getHostString(),
        netSocketFuture -> netSocketFuture.map(netSocket -> {
          Bytes32 nonce = RLPxConnectionFactory.generateRandomBytes32();
          KeyPair ephemeralKeyPair = KeyPair.random();
          Bytes initHandshakeMessage = RLPxConnectionFactory.init(keyPair, peerPublicKey, ephemeralKeyPair, nonce);
          logger.debug("Initiating handshake to {}", peerAddress);
          netSocket.write(Buffer.buffer(initHandshakeMessage.toArrayUnsafe()));

          netSocket.handler(new Handler<Buffer>() {

            private RLPxConnection conn;

            private DefaultWireConnection wireConnection;

            @Override
            public void handle(Buffer buffer) {
              try {
                Bytes messageBytes = Bytes.wrapBuffer(buffer);
                if (conn == null) {
                  int messageSize = RLPxConnectionFactory.messageSize(messageBytes);
                  Bytes responseBytes = messageBytes;
                  if (messageBytes.size() > messageSize) {
                    responseBytes = responseBytes.slice(0, messageSize);
                  }
                  messageBytes = messageBytes.slice(messageSize);
                  HandshakeMessage responseMessage =
                      RLPxConnectionFactory.readResponse(responseBytes, keyPair.secretKey());
                  conn = RLPxConnectionFactory.createConnection(
                      true,
                      initHandshakeMessage,
                      responseBytes,
                      ephemeralKeyPair.secretKey(),
                      responseMessage.ephemeralPublicKey(),
                      nonce,
                      responseMessage.nonce(),
                      keyPair.publicKey(),
                      peerPublicKey);

                  this.wireConnection = createConnection(conn, netSocket);
                  connected.complete();
                  if (messageBytes.isEmpty()) {
                    return;
                  }
                }
                if (conn != null) {
                  conn.stream(messageBytes, wireConnection::messageReceived);
                }
              } catch (Exception e) {
                logger.error(e.getMessage(), e);
                connected.completeExceptionally(e);
                netSocket.close();
              }
            }
          });
          return null;
        }));
    return connected;
  }

  private DefaultWireConnection createConnection(RLPxConnection conn, NetSocket netSocket) {
    String id = UUID.randomUUID().toString();
    DefaultWireConnection wireConnection = new DefaultWireConnection(
        id,
        conn.publicKey().bytes(),
        conn.peerPublicKey().bytes(),
        loggerProvider.getLogger("wireConnection-" + id),
        message -> {
          synchronized (conn) {
            Bytes bytes = conn.write(message);
            vertx.eventBus().send(netSocket.writeHandlerID(), Buffer.buffer(bytes.toArrayUnsafe()));
          }
        },
        conn::configureAfterHandshake,
        netSocket::end,
        handlers,
        DEVP2P_VERSION,
        clientId,
        advertisedPort());
    repository.add(wireConnection);
    return wireConnection;
  }

  private DefaultWireConnection wireConnection(String id) {
    if (!started.get()) {
      throw new IllegalStateException("The RLPx service is not active");
    }
    return (DefaultWireConnection) repository.get(id);
  }
}
