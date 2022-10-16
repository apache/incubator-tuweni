package org.apache.tuweni.scuttlebutt.discovery

import com.google.common.net.InetAddresses
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.datagram.DatagramPacket
import io.vertx.core.datagram.DatagramSocket
import org.apache.tuweni.concurrent.AsyncCompletion
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
 */
class ScuttlebuttLocalDiscoveryService internal constructor(
  vertx: Vertx,
  listenPort: Int,
  broadcastPort: Int,
  listenNetworkInterface: String?,
  multicastAddress: String,
  validateMulticast: Boolean,
) {

  companion object {
    private val logger = LoggerFactory.getLogger(ScuttlebuttLocalDiscoveryService::class.java)
  }

  private val started = AtomicBoolean(false)
  private val vertx: Vertx
  private val listeners: MutableList<Consumer<LocalIdentity?>> = ArrayList()
  private val identities: MutableList<LocalIdentity> = ArrayList()
  private val listenPort: Int
  private val broadcastPort: Int
  private val listenNetworkInterface: String?
  private val multicastAddress: String
  private var udpSocket: DatagramSocket? = null
  private var timerId: Long = 0

  /**
   * Default constructor.
   *
   * @param vertx Vert.x instance used to create the UDP socket
   * @param listenPort the port to bind the UDP socket to
   * @param listenNetworkInterface the network interface to bind the UDP socket to
   * @param multicastAddress the address to broadcast multicast packets to
   */
  constructor(
    vertx: Vertx,
    listenPort: Int,
    listenNetworkInterface: String?,
    multicastAddress: String,
  ) : this(vertx, listenPort, listenPort, listenNetworkInterface, multicastAddress, true) {
  }

  init {
    if (validateMulticast) {
      val multicastIP = InetAddresses.forString(multicastAddress)
      require(multicastIP.isMulticastAddress) { "Multicast address required, got $multicastAddress" }
    }
    this.vertx = vertx
    this.listenPort = listenPort
    this.broadcastPort = broadcastPort
    this.listenNetworkInterface = listenNetworkInterface
    this.multicastAddress = multicastAddress
  }

  /**
   * Starts the service.
   *
   * @return a handle to track the completion of the operation
   */
  fun start(): AsyncCompletion {
    if (started.compareAndSet(false, true)) {
      val started = AsyncCompletion.incomplete()
      udpSocket = vertx.createDatagramSocket()
      udpSocket!!.handler { datagramPacket: DatagramPacket ->
        listen(
          datagramPacket
        )
      }.listen(
        listenPort, listenNetworkInterface
      ) { handler: AsyncResult<DatagramSocket?> ->
        if (handler.failed()) {
          started.completeExceptionally(handler.cause())
        } else {
          started.complete()
        }
      }
      timerId = vertx.setPeriodic(60000) { time: Long? -> broadcast() }
      return started
    }
    return AsyncCompletion.completed()
  }

  fun listen(datagramPacket: DatagramPacket) {
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
      logger.debug("Invalid identity payload {}", packetString)
    }
  }

  fun broadcast() {
    for (id in identities) {
      udpSocket!!.send(
        id.toCanonicalForm(), broadcastPort, multicastAddress
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
  fun stop(): AsyncCompletion {
    if (started.compareAndSet(true, false)) {
      vertx.cancelTimer(timerId)
      val result = AsyncCompletion.incomplete()
      udpSocket!!.close { handler: AsyncResult<Void?> ->
        if (handler.failed()) {
          result.completeExceptionally(handler.cause())
        } else {
          result.complete()
        }
      }
      return result
    }
    return AsyncCompletion.completed()
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
