/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.scuttlebutt.discovery

import com.google.common.net.InetAddresses
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.datagram.DatagramPacket
import io.vertx.core.datagram.DatagramSocket
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

/**
 * Scuttlebutt local discovery service, based on the Scuttlebutt network protocol defined
 * [here](https://ssbc.github.io/scuttlebutt-protocol-guide/).
 *
 * This service offers two functions:
 *
 * It broadcasts to the local network every minute Scuttlebutt identities, as individual packets.
 *
 * It listens to broadcasted packets on the local network and relays Scuttlebutt identities identified to listeners.
 *
 *
 * @param vertx Vert.x instance used to create the UDP socket
 * @param listenPort the port to bind the UDP socket to
 * @param listenNetworkInterface the network interface to bind the UDP socket to
 * @param multicastAddress the address to broadcast multicast packets to
 * @param validateMulticast validate the multicast address to use - true by default, used for tests.
 */
class ScuttlebuttLocalDiscoveryService(
  private val vertx: Vertx,
  private val listenPort: Int,
  private val broadcastPort: Int,
  private val listenNetworkInterface: String,
  private val multicastAddress: String,
  private val validateMulticast: Boolean = true
) {

  companion object {
    private val logger = LoggerFactory.getLogger(ScuttlebuttLocalDiscoveryService::class.java)
  }

  private val started = AtomicBoolean(false)
  private val listeners: MutableList<Consumer<LocalIdentity?>> = ArrayList()
  private val identities: MutableList<LocalIdentity> = ArrayList()
  private var udpSocket: DatagramSocket? = null
  private var timerId: Long = 0

  init {
    if (validateMulticast) {
      val multicastIP = InetAddresses.forString(multicastAddress)
      require(multicastIP.isMulticastAddress) { "Multicast address required, got $multicastAddress" }
    }
  }

  /**
   * Starts the service.
   *
   * @return a handle to track the completion of the operation
   */
  suspend fun start() {
    if (started.compareAndSet(false, true)) {
      udpSocket = vertx.createDatagramSocket()
      udpSocket!!.handler { datagramPacket: DatagramPacket ->
        listen(
          datagramPacket
        )
      }.listen(
        listenPort,
        listenNetworkInterface
      ).await()
      timerId = vertx.setPeriodic(60000) { broadcast() }
    }
  }

  private fun listen(datagramPacket: DatagramPacket) {
    logger.debug("Received new packet from {}", datagramPacket.sender())
    val buffer = datagramPacket.data()
    if (buffer.length() > 100) {
      logger.debug("Packet too long, disregard")
      return
    }
    val packetString = buffer.toString()
    try {
      val id = LocalIdentity.fromString(packetString)
      for (listener in listeners) {
        listener.accept(id)
      }
    } catch (e: IllegalArgumentException) {
      logger.debug("Invalid identity payload {}", packetString, e)
    }
  }

  fun broadcast() {
    for (id in identities) {
      udpSocket!!.send(
        id.toCanonicalForm(),
        broadcastPort,
        multicastAddress
      ) { res: AsyncResult<Void?> ->
        if (res.failed()) {
          logger.error(res.cause().message, res.cause())
        }
      }
    }
  }

  /**
   * Stops the service.
   *
   * @return a handle to track the completion of the operation
   */
  suspend fun stop() {
    if (started.compareAndSet(true, false)) {
      vertx.cancelTimer(timerId)
      udpSocket?.close()?.await()
    }
  }

  /**
   * Adds an identity to the ones to be broadcast by the service.
   *
   * Identities may be added at any time during the lifecycle of the service
   *
   * @param identity the identity to add to the broadcast list
   */
  fun addIdentityToBroadcastList(identity: LocalIdentity) {
    identities.add(identity)
  }

  /**
   * Adds a listener to be notified when the service receives UDP packets that match Scuttlebutt identities.
   *
   * Listeners may be added at any time during the lifecycle of the service
   *
   * @param listener the listener to add
   */
  fun addListener(listener: Consumer<LocalIdentity?>) {
    listeners.add(listener)
  }
}
