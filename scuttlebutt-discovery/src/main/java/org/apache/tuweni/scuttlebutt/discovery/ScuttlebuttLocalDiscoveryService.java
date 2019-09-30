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
package org.apache.tuweni.scuttlebutt.discovery;

import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.CompletableAsyncCompletion;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.google.common.net.InetAddresses;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import org.logl.Logger;

/**
 * Scuttlebutt local discovery service, based on the Scuttlebutt network protocol defined
 * <a href="https://ssbc.github.io/scuttlebutt-protocol-guide/">here</a>.
 *
 * This service offers two functions:
 * <p>
 * It broadcasts to the local network every minute Scuttlebutt identities, as individual packets.
 * <p>
 * It listens to broadcasted packets on the local network and relays Scuttlebutt identities identified to listeners.
 *
 *
 *
 */
public class ScuttlebuttLocalDiscoveryService {

  private final AtomicBoolean started = new AtomicBoolean(false);
  private final Vertx vertx;
  private final Logger logger;
  private final List<Consumer<LocalIdentity>> listeners = new ArrayList<>();
  private final List<LocalIdentity> identities = new ArrayList<>();
  private final int listenPort;
  private final int broadcastPort;
  private final String listenNetworkInterface;
  private final String multicastAddress;

  private DatagramSocket udpSocket;
  private long timerId;

  /**
   * Default constructor.
   *
   * @param vertx Vert.x instance used to create the UDP socket
   * @param logger the logger used to log events and errors
   * @param listenPort the port to bind the UDP socket to
   * @param listenNetworkInterface the network interface to bind the UDP socket to
   * @param multicastAddress the address to broadcast multicast packets to
   */
  public ScuttlebuttLocalDiscoveryService(
      Vertx vertx,
      Logger logger,
      int listenPort,
      String listenNetworkInterface,
      String multicastAddress) {
    this(vertx, logger, listenPort, listenPort, listenNetworkInterface, multicastAddress, true);
  }


  public ScuttlebuttLocalDiscoveryService(
      Vertx vertx,
      Logger logger,
      int listenPort,
      int broadcastPort,
      String listenNetworkInterface,
      String multicastAddress,
      boolean validateMulticast) {
    if (validateMulticast) {
      InetAddress multicastIP = InetAddresses.forString(multicastAddress);
      if (!multicastIP.isMulticastAddress()) {
        throw new IllegalArgumentException("Multicast address required, got " + multicastAddress);
      }
    }
    this.vertx = vertx;
    this.logger = logger;
    this.listenPort = listenPort;
    this.broadcastPort = broadcastPort;
    this.listenNetworkInterface = listenNetworkInterface;
    this.multicastAddress = multicastAddress;
  }

  /**
   * Starts the service.
   *
   * @return a handle to track the completion of the operation
   */
  public AsyncCompletion start() {
    if (started.compareAndSet(false, true)) {
      CompletableAsyncCompletion started = AsyncCompletion.incomplete();
      udpSocket = vertx.createDatagramSocket();
      udpSocket.handler(this::listen).listen(listenPort, listenNetworkInterface, handler -> {
        if (handler.failed()) {
          started.completeExceptionally(handler.cause());
        } else {
          started.complete();
        }
      });
      timerId = vertx.setPeriodic(60000, (time) -> broadcast());
      return started;
    }
    return AsyncCompletion.completed();
  }

  void listen(DatagramPacket datagramPacket) {
    logger.debug("Received new packet from {}", datagramPacket.sender());
    Buffer buffer = datagramPacket.data();
    if (buffer.length() > 100) {
      logger.debug("Packet too long, disregard");
      return;
    }
    String packetString = buffer.toString();
    try {
      LocalIdentity id = LocalIdentity.fromString(packetString);
      for (Consumer<LocalIdentity> listener : listeners) {
        listener.accept(id);
      }
    } catch (IllegalArgumentException e) {
      logger.debug("Invalid identity payload {}", packetString);
    }
  }

  void broadcast() {
    for (LocalIdentity id : identities) {
      udpSocket.send(id.toCanonicalForm(), broadcastPort, multicastAddress, res -> {
        if (res.failed()) {
          logger.error(res.cause().getMessage(), res.cause());
        }
      });
    }
  }

  /**
   * Stops the service.
   *
   * @return a handle to track the completion of the operation
   */
  public AsyncCompletion stop() {
    if (started.compareAndSet(true, false)) {
      vertx.cancelTimer(timerId);
      CompletableAsyncCompletion result = AsyncCompletion.incomplete();
      udpSocket.close((handler) -> {
        if (handler.failed()) {
          result.completeExceptionally(handler.cause());
        } else {
          result.complete();
        }
      });
      return result;
    }
    return AsyncCompletion.completed();
  }

  /**
   * Adds an identity to the ones to be broadcast by the service.
   *
   * Identities may be added at any time during the lifecycle of the service
   *
   * @param identity the identity to add to the broadcast list
   */
  public void addIdentityToBroadcastList(LocalIdentity identity) {
    identities.add(identity);
  }

  /**
   * Adds a listener to be notified when the service receives UDP packets that match Scuttlebutt identities.
   *
   * Listeners may be added at any time during the lifecycle of the service
   *
   * @param listener the listener to add
   */
  public void addListener(Consumer<LocalIdentity> listener) {
    this.listeners.add(listener);
  }
}
