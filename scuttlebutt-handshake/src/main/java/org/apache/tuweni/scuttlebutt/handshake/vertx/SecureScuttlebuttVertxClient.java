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


import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.concurrent.CompletableAsyncResult;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.scuttlebutt.Invite;
import org.apache.tuweni.scuttlebutt.handshake.HandshakeException;
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttHandshakeClient;
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttStreamClient;
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttStreamServer;
import org.apache.tuweni.scuttlebutt.handshake.StreamException;

import javax.annotation.Nullable;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secure Scuttlebutt client using Vert.x to manage persistent TCP connections.
 *
 */
public final class SecureScuttlebuttVertxClient {

  private static final Logger logger = LoggerFactory.getLogger(NetSocketClientHandler.class);

  private class NetSocketClientHandler<T extends ClientHandler> {

    private final NetSocket socket;
    private final SecureScuttlebuttHandshakeClient handshakeClient;
    private final ClientHandlerFactory<T> handlerFactory;
    private final CompletableAsyncResult<T> completionHandle;
    private int handshakeCounter;
    private SecureScuttlebuttStreamClient client;
    private T handler;

    private Bytes messageBuffer = Bytes.EMPTY;

    NetSocketClientHandler(
        NetSocket socket,
        Signature.PublicKey remotePublicKey,
        @Nullable Invite invite,
        ClientHandlerFactory<T> handlerFactory,
        CompletableAsyncResult<T> completionHandle) {
      this.socket = socket;
      if (invite != null) {
        this.handshakeClient = SecureScuttlebuttHandshakeClient.fromInvite(networkIdentifier, invite);
      } else {
        this.handshakeClient = SecureScuttlebuttHandshakeClient.create(keyPair, networkIdentifier, remotePublicKey);
      }
      this.handlerFactory = handlerFactory;
      this.completionHandle = completionHandle;
      socket.closeHandler(res -> {
        if (handler != null) {
          handler.streamClosed();
        }
        if (!completionHandle.isDone()) {
          completionHandle.completeExceptionally(new IllegalStateException("Connection closed before handshake"));
        }
      });
      socket.exceptionHandler(e -> logger.error(e.getMessage(), e));
      socket.handler(this::handle);
      socket.write(Buffer.buffer(handshakeClient.createHello().toArrayUnsafe()));
    }

    void handle(Buffer buffer) {
      try {
        if (handshakeCounter == 0) {
          handshakeClient.readHello(Bytes.wrapBuffer(buffer));
          socket.write(Buffer.buffer(handshakeClient.createIdentityMessage().toArrayUnsafe()));
          handshakeCounter++;
        } else if (handshakeCounter == 1) {
          handshakeClient.readAcceptMessage(Bytes.wrapBuffer(buffer));
          client = handshakeClient.createStream();
          this.handler = handlerFactory.createHandler(bytes -> {
            synchronized (NetSocketClientHandler.this) {
              socket.write(Buffer.buffer(client.sendToServer(bytes).toArrayUnsafe()));
            }
          }, () -> {
            synchronized (NetSocketClientHandler.this) {
              socket.write(Buffer.buffer(client.sendGoodbyeToServer().toArrayUnsafe()));
              socket.close();
            }
          });
          completionHandle.complete(handler);
          handshakeCounter++;
        } else {
          Bytes message = client.readFromServer(Bytes.wrapBuffer(buffer));
          messageBuffer = Bytes.concatenate(messageBuffer, message);

          int headerSize = 9;

          // Process any whole RPC message responses we have, and leave any partial ones at the end in the buffer
          // We may have 1 or more whole messages, or 1 and a half, etc..
          while (messageBuffer.size() >= headerSize) {

            Bytes header = messageBuffer.slice(0, 9);
            int bodyLength = getBodyLength(header);

            if ((messageBuffer.size() - headerSize) >= (bodyLength)) {

              int headerAndBodyLength = bodyLength + headerSize;
              Bytes wholeMessage = messageBuffer.slice(0, headerAndBodyLength);

              if (SecureScuttlebuttStreamServer.isGoodbye(wholeMessage)) {
                logger.debug("Goodbye received from remote peer");
                socket.close();
              } else {
                handler.receivedMessage(wholeMessage);
              }

              // We've removed 1 RPC message from the message buffer, leave the remaining messages / part of a message
              // in the buffer to be processed in the next iteration
              messageBuffer = messageBuffer.slice(headerAndBodyLength);
            } else {
              // We don't have a full RPC message, leave the bytes in the buffer for when more arrive
              break;
            }
          }

        }
      } catch (HandshakeException | StreamException e) {
        completionHandle.completeExceptionally(e);
        logger.debug(e.getMessage(), e);
        socket.close();
      } catch (Throwable t) {
        if (!completionHandle.isDone()) {
          completionHandle.completeExceptionally(t);
        }

        logger.error(t.getMessage(), t);
        throw new RuntimeException(t);
      }
    }
  }

  private int getBodyLength(Bytes rpcHeader) {
    Bytes size = rpcHeader.slice(1, 4);
    return size.toInt();
  }

  private final Vertx vertx;
  private final Signature.KeyPair keyPair;
  private final Bytes32 networkIdentifier;
  private NetClient client;

  /**
   * Default constructor.
   *
   * @param vertx the Vert.x instance
   * @param keyPair the identity of the server according to the Secure Scuttlebutt protocol
   * @param networkIdentifier the network identifier of the server according to the Secure Scuttlebutt protocol
   */
  public SecureScuttlebuttVertxClient(Vertx vertx, Signature.KeyPair keyPair, Bytes32 networkIdentifier) {
    this.vertx = vertx;
    this.keyPair = keyPair;
    this.networkIdentifier = networkIdentifier;
  }

  /**
   * Connects the client to a remote host.
   *
   * @param <T> the type of client handler
   * @param port the port of the remote host
   * @param host the host string of the remote host
   * @param remotePublicKey the public key of the remote host, may be null if an invite is used.
   * @param invite the invite to the server, may be null
   * @param handlerFactory the factory of handlers for connections
   * @return a handle to a new stream handler with the remote host
   */
  public <T extends ClientHandler> AsyncResult<T> connectTo(
      int port,
      String host,
      @Nullable Signature.PublicKey remotePublicKey,
      @Nullable Invite invite,
      ClientHandlerFactory<T> handlerFactory) {
    client = vertx.createNetClient(new NetClientOptions().setTcpKeepAlive(true));
    CompletableAsyncResult<T> completion = AsyncResult.incomplete();
    client.connect(port, host, res -> {
      if (res.failed()) {
        completion.completeExceptionally(res.cause());
      } else {
        NetSocket socket = res.result();
        new NetSocketClientHandler<>(socket, remotePublicKey, invite, handlerFactory, completion);
      }
    });

    return completion;
  }

  /**
   * Stops the server.
   *
   * @return a handle to the completion of the operation
   */
  public AsyncCompletion stop() {
    client.close();
    return AsyncCompletion.completed();
  }
}
