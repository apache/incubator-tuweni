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
package org.apache.tuweni.plumtree.servlet;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
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
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GossipServlet extends HttpServlet {

  private static final String PLUMTREE_SERVER_HEADER = "Plumtree-Server";

  private static final Logger logger = LoggerFactory.getLogger(GossipServlet.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private void sendMessage(MessageSender.Verb verb, String attributes, Peer peer, Bytes hash, Bytes payload) {
    Message message = new Message();
    message.verb = verb;
    message.attributes = attributes;
    message.hash = hash.toHexString();
    message.payload = payload == null ? null : payload.toHexString();
    HttpPost postMessage = new HttpPost("http://" + ((ServletPeer) peer).getAddress());
    postMessage.setHeader(PLUMTREE_SERVER_HEADER, this.networkInterface + ":" + this.port);
    try {
      ByteArrayEntity entity = new ByteArrayEntity(mapper.writeValueAsBytes(message), ContentType.APPLICATION_JSON);
      postMessage.setEntity(entity);
      httpclient.execute(postMessage, response -> {
        ((ServletPeer) peer).getErrorsCounter().set(0);
        return null;
      });
    } catch (IOException e) {
      logger.info("Error sending to peer " + ((ServletPeer) peer).getAddress() + " : " + e.getMessage(), e);
      int newErrCount = ((ServletPeer) peer).getErrorsCounter().addAndGet(1);
      if (newErrCount > 5) {
        logger.error("Too many errors with peer {}, disconnecting ", ((ServletPeer) peer).getAddress());
        state.removePeer(peer);
      }
    }
  }

  private static final class Message {
    public MessageSender.Verb verb;
    public String attributes;
    public String hash;
    public String payload;
  }

  private final int graftDelay;
  private final int lazyQueueInterval;
  private final MessageHashing messageHashing;
  private final String networkInterface;
  private final int port;
  private final MessageListener payloadListener;
  private final MessageValidator payloadValidator;
  private final PeerPruning peerPruningFunction;
  private final PeerRepository peerRepository;

  private final AtomicBoolean started = new AtomicBoolean(false);
  private State state;

  public GossipServlet(
      int graftDelay,
      int lazyQueueInterval,
      MessageHashing messageHashing,
      String networkInterface,
      int port,
      MessageListener payloadListener,
      MessageValidator payloadValidator,
      PeerPruning peerPruningFunction,
      PeerRepository peerRepository) {
    this.graftDelay = graftDelay;
    this.lazyQueueInterval = lazyQueueInterval;
    this.messageHashing = messageHashing;
    this.networkInterface = networkInterface;
    this.port = port;
    this.payloadListener = payloadListener;
    this.payloadValidator = payloadValidator == null ? (bytes, peer) -> true : payloadValidator;
    this.peerPruningFunction = peerPruningFunction == null ? (peer) -> true : peerPruningFunction;
    this.peerRepository = peerRepository;
  }

  private CloseableHttpClient httpclient;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    if (started.compareAndSet(false, true)) {
      httpclient = HttpClients.createDefault();
      state = new State(peerRepository, messageHashing, this::sendMessage, payloadListener, payloadValidator, peerPruningFunction, graftDelay, lazyQueueInterval);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String peerServerAddress = req.getHeader(PLUMTREE_SERVER_HEADER);
    if (peerServerAddress == null || peerServerAddress.isBlank()) {
      resp.sendError(500);
      return;
    }
    ServletPeer peer = null;
    for (Peer p : peerRepository.peers()) {
      if (p instanceof ServletPeer && ((ServletPeer) p).getAddress().equals(peerServerAddress)) {
        peer = (ServletPeer) p;
      }
    }
    if (peer == null) {
      peer = new ServletPeer(peerServerAddress);
      state.addPeer(peer);
    }
    if (req.getContentLength() <= 0) {
      resp.setStatus(200);
      return;
    }
    Message message = mapper.readValue(req.getInputStream(), Message.class);
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
    resp.setStatus(200);
  }

  @Override
  public void destroy() {
    super.destroy();
    if (started.compareAndSet(true, false)) {
      state.stop();
      try {
        httpclient.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
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
    ServletPeer peer = new ServletPeer(host + ":" + port);
    HttpPost postMessage = new HttpPost("http://" + peer.getAddress());
    postMessage.setHeader(PLUMTREE_SERVER_HEADER, this.networkInterface + ":" + this.port);
    try {
      httpclient.execute(postMessage, response -> {
        if (response.getCode() > 299) {
          if (counter.incrementAndGet() > 5) {
            completion.completeExceptionally(new RuntimeException(response.getEntity().toString()));
          } else {
            roundConnect(host, port, counter, completion);
          }
        } else {
          state.addPeer(peer);
          completion.complete();
        }
        return null;
      });
    } catch (IOException e) {
      if (counter.incrementAndGet() > 5) {
        completion.completeExceptionally(e);
      } else {
        roundConnect(host, port, counter, completion);
      }
    }
  }
}
