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
package org.apache.tuweni.plumtree.vertx;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.CompletableAsyncCompletion;
import org.apache.tuweni.plumtree.MessageHashing;
import org.apache.tuweni.plumtree.MessageListener;
import org.apache.tuweni.plumtree.MessageSender;
import org.apache.tuweni.plumtree.MessageValidator;
import org.apache.tuweni.plumtree.Peer;
import org.apache.tuweni.plumtree.PeerPruning;
import org.apache.tuweni.plumtree.PeerRepository;
import org.apache.tuweni.plumtree.State;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;

/**
 * Vert.x implementation of the plumtree gossip.
 *
 * This implementation is provided as an example and relies on a simplistic JSON serialization of messages.
 *
 */
public final class VertxGossipServer {

  private static final ObjectMapper mapper = new ObjectMapper();

  private static final class Message {

    public MessageSender.Verb verb;
    public String attributes;
    public String hash;
    public String payload;
  }
  private final class SocketHandler {

    private final Peer peer;

    SocketHandler(Peer peer) {
      this.peer = peer;
      state.addPeer(peer);
    }

    private Bytes buffer = Bytes.EMPTY;

    void handle(Buffer data) {
      buffer = Bytes.concatenate(buffer, Bytes.wrapBuffer(data));
      while (!buffer.isEmpty()) {
        Message message;
        try {
          JsonParser parser = mapper.getFactory().createParser(buffer.toArrayUnsafe());
          message = parser.readValueAs(Message.class);
          buffer = buffer.slice((int) parser.getCurrentLocation().getByteOffset());
        } catch (IOException e) {
          return;
        }

        switch (message.verb) {
          case IHAVE:
            state.receiveIHaveMessage(peer, Bytes.fromHexString(message.hash));
            break;
          case GOSSIP:
            state
                .receiveGossipMessage(
                    peer,
                    message.attributes,
                    Bytes.fromHexString(message.payload),
                    Bytes.fromHexString(message.hash));
            break;
          case GRAFT:
            state.receiveGraftMessage(peer, Bytes.fromHexString(message.payload));
            break;
          case PRUNE:
            state.receivePruneMessage(peer);
            break;
          case SEND:
            payloadListener.listen(Bytes.fromHexString(message.payload), message.attributes, peer);
        }
      }
    }

    void close(Void aVoid) {
      state.removePeer(peer);
    }
  }

  private NetClient client;
  private final int graftDelay;
  private final int lazyQueueInterval;
  private final MessageHashing messageHashing;
  private final String networkInterface;
  private final MessageListener payloadListener;
  private final MessageValidator payloadValidator;
  private final PeerPruning peerPruningFunction;
  private final PeerRepository peerRepository;
  private final int port;
  private NetServer server;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private State state;
  private final Vertx vertx;

  public VertxGossipServer(
      Vertx vertx,
      String networkInterface,
      int port,
      MessageHashing messageHashing,
      PeerRepository peerRepository,
      MessageListener payloadListener,
      @Nullable MessageValidator payloadValidator,
      @Nullable PeerPruning peerPruningFunction,
      int graftDelay,
      int lazyQueueInterval) {
    this.vertx = vertx;
    this.networkInterface = networkInterface;
    this.port = port;
    this.messageHashing = messageHashing;
    this.peerRepository = peerRepository;
    this.payloadListener = payloadListener;
    this.payloadValidator = payloadValidator == null ? (bytes, peer) -> true : payloadValidator;
    this.peerPruningFunction = peerPruningFunction == null ? (peer) -> true : peerPruningFunction;
    this.graftDelay = graftDelay;
    this.lazyQueueInterval = lazyQueueInterval;
  }

  public AsyncCompletion start() {
    if (started.compareAndSet(false, true)) {
      CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
      server = vertx.createNetServer();
      client = vertx.createNetClient();
      server.connectHandler(socket -> {
        Peer peer = new SocketPeer(socket);
        SocketHandler handler = new SocketHandler(peer);
        socket.handler(handler::handle).closeHandler(handler::close).exceptionHandler(Throwable::printStackTrace);
      });
      server.exceptionHandler(Throwable::printStackTrace);
      server.listen(port, networkInterface, res -> {
        if (res.failed()) {
          completion.completeExceptionally(res.cause());
        } else {
          state = new State(peerRepository, messageHashing, (verb, attributes, peer, hash, payload) -> {
            vertx.executeBlocking(future -> {
              Message message = new Message();
              message.verb = verb;
              message.attributes = attributes;
              message.hash = hash.toHexString();
              message.payload = payload == null ? null : payload.toHexString();
              try {
                ((SocketPeer) peer).socket().write(Buffer.buffer(mapper.writeValueAsBytes(message)));
                future.complete();
              } catch (JsonProcessingException e) {
                future.fail(e);
              }

            }, done -> {
            });
          }, payloadListener, payloadValidator, peerPruningFunction, graftDelay, lazyQueueInterval);

          completion.complete();
        }
      });

      return completion;
    } else {
      return AsyncCompletion.completed();
    }
  }

  public AsyncCompletion stop() {
    if (started.compareAndSet(true, false)) {
      CompletableAsyncCompletion completion = AsyncCompletion.incomplete();

      state.stop();
      client.close();
      server.close(res -> {
        if (res.failed()) {
          completion.completeExceptionally(res.cause());
        } else {
          completion.complete();
        }
      });

      return completion;
    }
    return AsyncCompletion.completed();
  }

  public AsyncCompletion connectTo(String host, int port) {
    if (!started.get()) {
      throw new IllegalStateException("Server has not started");
    }

    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    AtomicInteger counter = new AtomicInteger(0);

    roundConnect(host, port, counter, completion);

    return completion;
  }

  private void roundConnect(String host, int port, AtomicInteger counter, CompletableAsyncCompletion completion) {
    client.connect(port, host, res -> {
      if (res.failed()) {
        if (counter.incrementAndGet() > 5) {
          completion.completeExceptionally(res.cause());
        } else {
          roundConnect(host, port, counter, completion);
        }
      } else {
        Peer peer = new SocketPeer(res.result());
        SocketHandler handler = new SocketHandler(peer);
        res.result().handler(handler::handle).closeHandler(handler::close);
        completion.complete();
      }
    });
  }

  /**
   * Gossip a message to all known peers.
   * 
   * @param attributes the payload to propagate
   * @param message the payload to propagate
   */
  public void gossip(String attributes, Bytes message) {
    if (!started.get()) {
      throw new IllegalStateException("Server has not started");
    }
    state.sendGossipMessage(attributes, message);
  }

  /**
   * Send a message to one peer specifically.
   *
   * @param peer the peer to send to
   * @param attributes the payload to propagate
   * @param message the payload to propagate
   */
  public void send(Peer peer, String attributes, Bytes message) {
    if (!started.get()) {
      throw new IllegalStateException("Server has not started");
    }
    state.sendMessage(peer, attributes, message);
  }
}
