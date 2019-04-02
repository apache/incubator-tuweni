/*
 * Copyright 2019 ConsenSys AG.
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
package org.apache.tuweni.scuttlebutt.handshake.vertx;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.CompletableAsyncCompletion;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.scuttlebutt.handshake.HandshakeException;
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttHandshakeServer;
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttStreamServer;
import org.apache.tuweni.scuttlebutt.handshake.StreamException;

import java.net.InetSocketAddress;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;

/**
 * Secure Scuttlebutt server using Vert.x to manage persistent TCP connections.
 *
 */
public final class SecureScuttlebuttVertxServer {

  private final class NetSocketHandler {

    int handshakeCounter = 0;
    NetSocket netSocket;
    ServerHandler handler;
    SecureScuttlebuttStreamServer streamServer;
    SecureScuttlebuttHandshakeServer handshakeServer =
        SecureScuttlebuttHandshakeServer.create(keyPair, networkIdentifier);

    private Bytes messageBuffer = Bytes.EMPTY;

    void handle(NetSocket netSocket) {
      this.netSocket = netSocket;
      netSocket.closeHandler(res -> {
        if (handler != null) {
          handler.streamClosed();
        }
      });

      netSocket.handler(this::handleMessage);
    }

    private void handleMessage(Buffer buffer) {
      try {
        if (handshakeCounter == 0) {
          handshakeServer.readHello(Bytes.wrapBuffer(buffer));
          netSocket.write(Buffer.buffer(handshakeServer.createHello().toArrayUnsafe()));
          handshakeCounter++;
        } else if (handshakeCounter == 1) {
          handshakeServer.readIdentityMessage(Bytes.wrapBuffer(buffer));
          netSocket.write(Buffer.buffer(handshakeServer.createAcceptMessage().toArrayUnsafe()));
          streamServer = handshakeServer.createStream();
          handshakeCounter++;
          handler = handlerFactory.createHandler(bytes -> {
            synchronized (NetSocketHandler.this) {
              netSocket.write(Buffer.buffer(streamServer.sendToClient(bytes).toArrayUnsafe()));
            }
          }, () -> {
            synchronized (NetSocketHandler.this) {
              netSocket.write(Buffer.buffer(streamServer.sendGoodbyeToClient().toArrayUnsafe()));
              netSocket.close();
            }
          });
        } else {
          Bytes message = streamServer.readFromClient(Bytes.wrapBuffer(buffer));
          messageBuffer = Bytes.concatenate(messageBuffer, message);

          int headerSize = 9;

          // Process any whole RPC message repsonses we have, and leave any partial ones at the end in the buffer
          // We may have 1 or more whole messages, or 1 and a half, etc..
          while (messageBuffer.size() >= headerSize) {

            Bytes header = messageBuffer.slice(0, 9);
            int bodyLength = getBodyLength(header);

            if ((messageBuffer.size() - headerSize) >= (bodyLength)) {

              int headerAndBodyLength = bodyLength + headerSize;
              Bytes wholeMessage = messageBuffer.slice(0, headerAndBodyLength);

              if (SecureScuttlebuttStreamServer.isGoodbye(wholeMessage)) {
                netSocket.close();
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
        e.printStackTrace();
        netSocket.close();
      }
    }
  }

  private int getBodyLength(Bytes rpcHeader) {
    Bytes size = rpcHeader.slice(1, 4);
    return size.toInt();
  }

  private final Vertx vertx;
  private final InetSocketAddress addr;
  private final Signature.KeyPair keyPair;
  private final Bytes32 networkIdentifier;
  private NetServer server;
  private final ServerHandlerFactory handlerFactory;

  /**
   * Default constructor.
   *
   * @param vertx the Vert.x instance
   * @param addr the network interface and port to bind the server to
   * @param keyPair the identity of the server according to the Secure Scuttlebutt protocol
   * @param networkIdentifier the network identifier of the server according to the Secure Scuttlebutt protocol
   * @param handlerFactory the factory of handlers that will manage stream connections
   */
  public SecureScuttlebuttVertxServer(
      Vertx vertx,
      InetSocketAddress addr,
      Signature.KeyPair keyPair,
      Bytes32 networkIdentifier,
      ServerHandlerFactory handlerFactory) {
    this.vertx = vertx;
    this.addr = addr;
    this.keyPair = keyPair;
    this.networkIdentifier = networkIdentifier;
    this.handlerFactory = handlerFactory;
  }

  /**
   * Starts the server.
   *
   * @return a handle to the completion of the operation
   */
  public AsyncCompletion start() {
    server = vertx.createNetServer(
        new NetServerOptions().setTcpKeepAlive(true).setHost(addr.getHostString()).setPort(addr.getPort()));
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    server.connectHandler(new NetSocketHandler()::handle);
    server.listen(res -> {
      if (res.failed()) {
        completion.completeExceptionally(res.cause());
      } else {
        completion.complete();
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
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();

    server.close(res -> {
      if (res.failed()) {
        completion.completeExceptionally(res.cause());
      } else {
        completion.complete();
      }
    });
    return completion;
  }
}
