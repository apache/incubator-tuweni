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

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.CompletableAsyncCompletion;
import org.apache.tuweni.concurrent.CompletableAsyncResult;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.rlpx.RLPxMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A stateful connection between two peers under the Devp2p wire protocol.
 */
public final class DefaultWireConnection implements WireConnection {

  private final static Logger logger = LoggerFactory.getLogger(DefaultWireConnection.class);

  private final Bytes nodeId;
  private final Bytes peerNodeId;
  private final Consumer<RLPxMessage> writer;
  private final Consumer<HelloMessage> afterHandshakeListener;
  private final Runnable disconnectHandler;
  private final LinkedHashMap<SubProtocolIdentifier, SubProtocolHandler> subprotocols;
  private final int p2pVersion;
  private final String clientId;
  private final int advertisedPort;
  private final CompletableAsyncResult<WireConnection> ready;
  private final String peerHost;
  private final int peerPort;
  private final AtomicBoolean disconnectRequested = new AtomicBoolean(false);

  private CompletableAsyncCompletion awaitingPong;
  private HelloMessage myHelloMessage;
  private HelloMessage peerHelloMessage;
  private RangeMap<Integer, SubProtocolIdentifier> subprotocolRangeMap = TreeRangeMap.create();
  private DisconnectReason disconnectReason;
  private boolean disconnectReceived;
  private EventListener eventListener;

  /**
   * Default constructor.
   *
   * @param nodeId the node id of this node
   * @param peerNodeId the node id of the peer
   * @param writer the message writer
   * @param afterHandshakeListener a listener called after the handshake is complete with the peer hello message.
   * @param disconnectHandler the handler to run upon receiving a disconnect message
   * @param subprotocols the subprotocols supported by this connection
   * @param p2pVersion the version of the devp2p protocol supported by this client
   * @param clientId the client ID to announce in HELLO messages
   * @param advertisedPort the port we listen to, to announce in HELLO messages
   * @param ready a handle to complete when the connection is ready for use.
   * @param peerHost the peer's host
   * @param peerPort the peer's port
   */
  public DefaultWireConnection(
      Bytes nodeId,
      Bytes peerNodeId,
      Consumer<RLPxMessage> writer,
      Consumer<HelloMessage> afterHandshakeListener,
      Runnable disconnectHandler,
      LinkedHashMap<SubProtocolIdentifier, SubProtocolHandler> subprotocols,
      int p2pVersion,
      String clientId,
      int advertisedPort,
      CompletableAsyncResult<WireConnection> ready,
      String peerHost,
      int peerPort) {
    this.nodeId = nodeId;
    this.peerNodeId = peerNodeId;
    this.writer = writer;
    this.afterHandshakeListener = afterHandshakeListener;
    this.disconnectHandler = disconnectHandler;
    this.subprotocols = subprotocols;
    this.p2pVersion = p2pVersion;
    this.clientId = clientId;
    this.advertisedPort = advertisedPort;
    this.ready = ready;
    this.peerHost = peerHost;
    this.peerPort = peerPort;
    logger.debug("New wire connection created");
  }

  public void messageReceived(RLPxMessage message) {
    if (message.messageId() == 0) {
      peerHelloMessage = HelloMessage.read(message.content());
      logger.debug("Received peer Hello message {}", peerHelloMessage);
      initSupportedRange(peerHelloMessage.capabilities());
      if (peerHelloMessage.nodeId() == null || peerHelloMessage.nodeId().isEmpty()) {
        disconnect(DisconnectReason.NULL_NODE_IDENTITY_RECEIVED);
        ready.complete(this);
        return;
      }

      if (!peerHelloMessage.nodeId().equals(peerNodeId)) {
        disconnect(DisconnectReason.UNEXPECTED_IDENTITY);
        ready.complete(this);
        return;
      }

      if (peerHelloMessage.nodeId().equals(nodeId)) {
        disconnect(DisconnectReason.CONNECTED_TO_SELF);
        ready.complete(this);
        return;
      }

      if (peerHelloMessage.p2pVersion() > p2pVersion) {
        disconnect(DisconnectReason.INCOMPATIBLE_DEVP2P_VERSION);
        ready.complete(this);
        return;
      }

      if (subprotocolRangeMap.asMapOfRanges().isEmpty()) {
        logger
            .debug(
                "Useless peer detected, caps {}, our caps {}",
                peerHelloMessage.capabilities(),
                subprotocols.keySet());
        disconnect(DisconnectReason.USELESS_PEER);
        ready.complete(this);
        return;
      }

      if (myHelloMessage == null) {
        sendHello();
      }

      afterHandshakeListener.accept(peerHelloMessage);

      AsyncCompletion allSubProtocols = AsyncCompletion
          .allOf(
              subprotocolRangeMap
                  .asMapOfRanges()
                  .values()
                  .stream()
                  .map(subprotocols::get)
                  .map(handler -> handler.handleNewPeerConnection(this)));
      allSubProtocols.thenRun(() -> {
        ready.complete(this);
        eventListener.onEvent(Event.CONNECTED);
      });
      return;
    } else if (message.messageId() == 1) {
      DisconnectMessage disconnect = DisconnectMessage.read(message.content());
      logger.debug("Received disconnect {} {}", disconnect.disconnectReason(), uri());
      disconnectReceived = true;
      disconnectReason = disconnect.disconnectReason();
      disconnectHandler.run();
      if (!ready.isDone()) {
        ready.complete(this); // Return the connection as is.
      }
      eventListener.onEvent(Event.DISCONNECTED);
      return;
    }

    if (peerHelloMessage == null || myHelloMessage == null) {
      logger.debug("Message sent before hello exchanged {}", message.messageId());
      disconnect(DisconnectReason.PROTOCOL_BREACH);
    }

    if (message.messageId() == 2) {
      sendPong();
    } else if (message.messageId() == 3) {
      if (awaitingPong != null) {
        awaitingPong.complete();
      }
    } else {
      Map.Entry<Range<Integer>, SubProtocolIdentifier> subProtocolEntry =
          subprotocolRangeMap.getEntry(message.messageId());
      if (subProtocolEntry == null) {
        logger.debug("Unknown message received {}", message.messageId());
        disconnect(DisconnectReason.PROTOCOL_BREACH);
        if (!ready.isDone()) {
          ready.complete(this);
        }
      } else {
        int offset = subProtocolEntry.getKey().lowerEndpoint();
        logger.trace("Received message of type {}", message.messageId() - offset);
        SubProtocolHandler handler = subprotocols.get(subProtocolEntry.getValue());
        handler
            .handle(this, message.messageId() - offset, message.content())
            .exceptionally(t -> logger.error("Handler " + handler.toString() + " threw an exception", t));
      }
    }
  }

  void initSupportedRange(List<Capability> capabilities) {
    int startRange = 16;
    Map<String, SubProtocolIdentifier> pickedCapabilities = new HashMap<>();
    // find the max capability supported by the subprotocol
    for (SubProtocolIdentifier sp : subprotocols.keySet()) {
      for (Capability cap : capabilities) {
        if (sp.equals(SubProtocolIdentifier.of(cap.name(), cap.version()))) {
          SubProtocolIdentifier oldPick = pickedCapabilities.get(cap.name());
          if (oldPick == null || oldPick.version() < cap.version()) {
            pickedCapabilities.put(cap.name(), sp);
          }
        }
      }
    }

    for (Capability cap : capabilities) {
      SubProtocolIdentifier capSp = SubProtocolIdentifier.of(cap.name(), cap.version());
      if (!Objects.equals(pickedCapabilities.get(cap.name()), capSp)) {
        continue;
      }
      for (SubProtocolIdentifier sp : subprotocols.keySet()) {
        if (sp.equals(capSp)) {
          int numberOfMessageTypes = sp.versionRange();
          subprotocolRangeMap.put(Range.closedOpen(startRange, startRange + numberOfMessageTypes), sp);
          startRange += numberOfMessageTypes;
          break;
        }
      }
    }
  }

  /**
   * Sends a message to the peer explaining that we are about to disconnect.
   *
   * @param reason the reason for disconnection
   */
  @Override
  public void disconnect(DisconnectReason reason) {
    if (disconnectRequested.compareAndSet(false, true)) {
      logger.debug("Sending disconnect message with reason {}", reason);
      writer.accept(new RLPxMessage(1, new DisconnectMessage(reason).toBytes()));
      disconnectReason = reason;
      disconnectHandler.run();
      eventListener.onEvent(Event.DISCONNECTED);
    }
  }

  /**
   * Sends a ping message to the remote peer.
   *
   * @return a handler marking completion when a pong response is received
   */
  public AsyncCompletion sendPing() {
    logger.debug("Sending ping message");
    writer.accept(new RLPxMessage(2, Bytes.EMPTY));
    this.awaitingPong = AsyncCompletion.incomplete();
    return awaitingPong;
  }

  private void sendPong() {
    logger.debug("Sending pong message");
    writer.accept(new RLPxMessage(3, Bytes.EMPTY));
  }

  void sendHello() {
    myHelloMessage = HelloMessage
        .create(
            nodeId,
            advertisedPort,
            p2pVersion,
            clientId,
            subprotocols
                .keySet()
                .stream()
                .map(
                    subProtocolIdentifier -> new Capability(
                        subProtocolIdentifier.name(),
                        subProtocolIdentifier.version()))
                .collect(Collectors.toList()));
    logger.debug("Sending hello message {}", myHelloMessage);
    writer.accept(new RLPxMessage(0, myHelloMessage.toBytes()));
  }

  @Override
  public boolean supports(SubProtocolIdentifier subProtocolIdentifier) {
    for (SubProtocolIdentifier sp : subprotocolRangeMap.asMapOfRanges().values()) {
      if (sp.equals(subProtocolIdentifier)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Collection<SubProtocolIdentifier> agreedSubprotocols() {
    List<SubProtocolIdentifier> identifiers = new ArrayList<>();
    for (SubProtocolIdentifier sp : subprotocolRangeMap.asMapOfRanges().values()) {
      identifiers.add(sp);
    }
    return identifiers;
  }

  public void sendMessage(SubProtocolIdentifier subProtocolIdentifier, int messageType, Bytes message) {
    logger.trace("Sending sub-protocol message {} {} {}", messageType, message, uri());
    Integer offset = null;
    for (Map.Entry<Range<Integer>, SubProtocolIdentifier> entry : subprotocolRangeMap.asMapOfRanges().entrySet()) {
      if (entry.getValue().equals(subProtocolIdentifier)) {
        offset = entry.getKey().lowerEndpoint();
        break;
      }
    }
    if (offset == null) {
      throw new UnsupportedOperationException(); // no subprotocol mapped to this connection. Exit.
    }
    writer.accept(new RLPxMessage(messageType + offset, message));
  }

  public void handleConnectionStart() {
    sendHello();
  }

  @Override
  public String toString() {
    return peerNodeId.toHexString();
  }

  @Override
  public boolean isDisconnectReceived() {
    return disconnectReceived;
  }

  @Override
  public boolean isDisconnectRequested() {
    return disconnectRequested.get();
  }

  @Override
  public DisconnectReason getDisconnectReason() {
    return disconnectReason;
  }

  @Override
  public String peerHost() {
    return peerHost;
  }

  @Override
  public int peerPort() {
    return peerPort;
  }

  @Override
  public SECP256K1.PublicKey peerPublicKey() {
    return SECP256K1.PublicKey.fromBytes(peerNodeId);
  }

  @Override
  public HelloMessage getPeerHello() {
    return peerHelloMessage;
  }

  @Override
  public void registerListener(EventListener listener) {
    eventListener = listener;
  }
}
