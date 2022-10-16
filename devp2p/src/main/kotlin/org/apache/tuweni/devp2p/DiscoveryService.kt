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
package org.apache.tuweni.devp2p

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.datagram.DatagramPacket
import io.vertx.core.net.SocketAddress
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.asyncCompletion
import org.apache.tuweni.concurrent.coroutines.asyncResult
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.kademlia.orderedInsert
import org.apache.tuweni.kademlia.xorDistCmp
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.URI
import java.nio.ByteBuffer
import java.time.Instant
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

internal const val PACKET_EXPIRATION_PERIOD_MS = (20 * 1000).toLong() // 20 seconds
internal const val PACKET_EXPIRATION_CHECK_GRACE_MS = (5 * 1000).toLong() // 5 seconds
internal const val PEER_VERIFICATION_TIMEOUT_MS = (22 * 1000).toLong() // 22 seconds (packet expiration + a little)
internal const val ENR_REQUEST_TIMEOUT_MS = (22 * 1000).toLong() // 22 seconds (packet expiration + a little)
internal const val PEER_VERIFICATION_RETRY_DELAY_MS = (5 * 60 * 1000).toLong() // 5 minutes
internal const val ENR_REQUEST_RETRY_DELAY_MS = (5 * 60 * 1000).toLong() // 5 minutes
internal const val BOOTSTRAP_PEER_VERIFICATION_TIMEOUT_MS = (2 * 60 * 1000).toLong() // 2 minutes
internal const val REFRESH_INTERVAL_MS = (60 * 1000).toLong() // 1 minute
internal const val PING_RETRIES: Int = 20
internal const val RESEND_DELAY_MS = 1000.toLong() // 1 second
internal const val RESEND_DELAY_INCREASE_MS = 500.toLong() // 500 milliseconds
internal const val RESEND_MAX_DELAY_MS = (30 * 1000).toLong() // 30 seconds
internal const val ENDPOINT_PROOF_LONGEVITY_MS = (12 * 60 * 60 * 1000).toLong() // 12 hours
internal const val FIND_NODES_CACHE_EXPIRY = (3 * 60 * 1000).toLong() // 3 minutes
internal const val FIND_NODES_QUERY_GAP_MS = (30 * 1000).toLong() // 30 seconds
internal const val LOOKUP_RESPONSE_TIMEOUT_MS = 500.toLong() // 500 milliseconds

/**
 * An Ethereum ÐΞVp2p discovery service.
 *
 * This service supports devp2p discovery v4, alongside support for EIP-868.
 * http://eips.ethereum.org/EIPS/eip-868
 *
 */
interface DiscoveryService {

  companion object {
    internal val CURRENT_TIME_SUPPLIER: () -> Long = { System.currentTimeMillis() }

    /**
     * Start the discovery service.
     *
     * @param vertx Vert.x instance
     * @param keyPair the local node's keypair
     * @param port the port to listen on (defaults to `0`, which will cause a random free port to be chosen)
     * @param host the host name or IP address of the interface to bind to (defaults to `null`, which will cause the
     *         service to listen on all interfaces
     * @param seq the sequence number of the Ethereum Node Record
     * @param enrData the additional key/value pair entries to broadcast as an Ethereum Node Record (ENR).
     * @param bootstrapURIs the URIs for bootstrap nodes
     * @param peerRepository a [PeerRepository] for obtaining [Peer] instances
     * @param advertiseAddress the IP address to advertise to peers, or `null` if the address of the first bound
     *         interface should be used.
     * @param advertiseUdpPort the UDP port to advertise to peers, or `null` if the bound port should to be used.
     * @param advertiseTcpPort the TCP port to advertise to peers, or `null` if it should be the same as the UDP port.
     * @param routingTable a [PeerRoutingTable] which handles the ÐΞVp2p routing table
     * @param packetFilter a filter for incoming packets
     \     * @param bufferAllocator a [ByteBuffer] allocator, which must return buffers of size 1280 bytes or larger
     * @param timeSupplier a function supplying the current time, in milliseconds since the epoch
     */
    @JvmOverloads
    fun open(
      vertx: Vertx,
      keyPair: SECP256K1.KeyPair,
      port: Int = 0,
      host: String? = null,
      seq: Long = Instant.now().toEpochMilli(),
      enrData: Map<String, Bytes> = emptyMap(),
      bootstrapURIs: List<URI> = emptyList(),
      peerRepository: PeerRepository = EphemeralPeerRepository(),
      advertiseAddress: String? = null,
      advertiseUdpPort: Int? = null,
      advertiseTcpPort: Int? = null,
      routingTable: PeerRoutingTable = DevP2PPeerRoutingTable(keyPair.publicKey()),
      packetFilter: ((SECP256K1.PublicKey, SocketAddress) -> Boolean)? = null,
      timeSupplier: () -> Long = CURRENT_TIME_SUPPLIER
    ): DiscoveryService {
      val bindAddress =
        if (host == null) {
          SocketAddress.inetSocketAddress(port, "127.0.0.1")
        } else {
          SocketAddress.inetSocketAddress(
            port,
            host
          )
        }
      return open(
        vertx,
        keyPair,
        bindAddress,
        seq,
        enrData,
        bootstrapURIs,
        peerRepository,
        advertiseAddress,
        advertiseUdpPort,
        advertiseTcpPort,
        routingTable,
        packetFilter,
        timeSupplier
      )
    }

    /**
     * Start the discovery service.
     *
     * @param vertx Vert.x instance
     * @param keyPair the local node's keypair
     * @param bindAddress the address to listen on
     * @param seq the sequence number of the Ethereum Node Record
     * @param enrData the additional key/value pair entries to broadcast as an Ethereum Node Record (ENR).
     * @param bootstrapURIs the URIs for bootstrap nodes
     * @param peerRepository a [PeerRepository] for obtaining [Peer] instances
     * @param advertiseAddress the IP address to advertise for incoming packets
     * @param advertiseUdpPort the UDP port to advertise to peers, or `null` if the bound port should to be used.
     * @param advertiseTcpPort the TCP port to advertise to peers, or `null` if it should be the same as the UDP port.
     * @param routingTable a [PeerRoutingTable] which handles the ÐΞVp2p routing table
     * @param packetFilter a filter for incoming packets
     * @param timeSupplier a function supplying the current time, in milliseconds since the epoch
     */
    @JvmOverloads
    fun open(
      vertx: Vertx,
      keyPair: SECP256K1.KeyPair,
      bindAddress: SocketAddress,
      seq: Long = Instant.now().toEpochMilli(),
      enrData: Map<String, Bytes> = emptyMap(),
      bootstrapURIs: List<URI> = emptyList(),
      peerRepository: PeerRepository = EphemeralPeerRepository(),
      advertiseAddress: String? = null,
      advertiseUdpPort: Int? = null,
      advertiseTcpPort: Int? = null,
      routingTable: PeerRoutingTable = DevP2PPeerRoutingTable(keyPair.publicKey()),
      packetFilter: ((SECP256K1.PublicKey, SocketAddress) -> Boolean)? = null,
      timeSupplier: () -> Long = CURRENT_TIME_SUPPLIER
    ): DiscoveryService {
      return CoroutineDiscoveryService(
        vertx,
        keyPair, seq, enrData, bindAddress, bootstrapURIs, advertiseAddress, advertiseUdpPort, advertiseTcpPort,
        peerRepository, routingTable, packetFilter, timeSupplier
      )
    }
  }

  /**
   * `true` if the service has been shutdown
   */
  val isShutdown: Boolean

  /**
   * the UDP port that the service is listening on
   */
  val localPort: Int

  /**
   * the node id for this node (i.e. it's public key)
   */
  val nodeId: SECP256K1.PublicKey

  /**
   * Suspend until the bootstrap peers have been reached, or failed.
   *
   */
  suspend fun awaitBootstrap()

  /**
   * Suspend until the bootstrap peers have been reached, or failed.
   *
   */
  fun awaitBootstrapAsync(): AsyncCompletion

  /**
   * Attempt to find a specific peer, or peers close to it.
   *
   * @param target the node-id to search for
   * @return a list of 16 peers, ordered by their distance to the target node-id.
   */
  suspend fun lookup(target: SECP256K1.PublicKey): List<Peer>

  /**
   * Attempt to find a specific peer, or peers close to it asynchronously.
   *
   * @param target the node-id to search for
   * @return a future of a list of 16 peers, ordered by their distance to the target node-id.
   */
  fun lookupAsync(target: SECP256K1.PublicKey): AsyncResult<List<Peer>>

  /**
   * Shuts down this service.
   */
  suspend fun shutdown()

  /**
   * Suspend until this service has terminated.
   */
  fun shutdownAsync(): AsyncCompletion

  /**
   * Counter of invalid packets
   */
  val invalidPackets: Long

  /**
   * Counter of packets sent to self
   */
  val selfPackets: Long

  /**
   * Counter of expired packets
   */
  val expiredPackets: Long

  /**
   * Counter of filtered packets
   */
  val filteredPackets: Long

  /**
   * Counter of unvalidated peer packets
   */
  val unvalidatedPeerPackets: Long

  /**
   * Counter of unexpected pongs
   */
  val unexpectedPongs: Long

  /**
   * Counter of unexpected NEIGHBORS messages
   */
  val unexpectedNeighbors: Long

  /**
   * Counter of unexpected ENRResponse messages
   */
  val unexpectedENRResponses: Long
}

internal class CoroutineDiscoveryService constructor(
  vertx: Vertx,
  private val keyPair: SECP256K1.KeyPair,
  private val seq: Long = Instant.now().toEpochMilli(),
  private val enrData: Map<String, Bytes>,
  private val bindAddress: SocketAddress,
  private val bootstrapURIs: List<URI> = emptyList(),
  private val advertiseAddress: String? = null,
  private val advertiseUdpPort: Int? = null,
  private val advertiseTcpPort: Int? = null,
  private val peerRepository: PeerRepository = EphemeralPeerRepository(),
  private val routingTable: PeerRoutingTable = DevP2PPeerRoutingTable(keyPair.publicKey()),
  private val packetFilter: ((SECP256K1.PublicKey, SocketAddress) -> Boolean)? = null,
  private val timeSupplier: () -> Long = DiscoveryService.CURRENT_TIME_SUPPLIER,
  override val coroutineContext: CoroutineContext = vertx.dispatcher() + CoroutineExceptionHandler { _, _ -> }
) : DiscoveryService, CoroutineScope {

  companion object {
    internal val logger = LoggerFactory.getLogger(DiscoveryService::class.java)
  }

  private val serviceDescriptor = "ÐΞVp2p discovery " + System.identityHashCode(this)
  private var selfEndpoint: Endpoint? = null
  private var enr: Bytes? = null

  private val shutdown = AtomicBoolean(false)
  private val bootstrapped = AsyncCompletion.incomplete()
  private var refreshLoop: Job? = null
  private val server = vertx.createDatagramSocket()

  override val isShutdown: Boolean
    get() = shutdown.get()

  override val localPort: Int
    get() = server.localAddress().port()

  override val nodeId: SECP256K1.PublicKey
    get() = keyPair.publicKey()

  private val verifyingEndpoints: Cache<SocketAddress, EndpointVerification> =
    CacheBuilder.newBuilder().expireAfterAccess(PEER_VERIFICATION_RETRY_DELAY_MS, TimeUnit.MILLISECONDS).build()
  private val requestingENRs: Cache<SocketAddress, ENRRequest> =
    CacheBuilder.newBuilder().expireAfterAccess(ENR_REQUEST_RETRY_DELAY_MS, TimeUnit.MILLISECONDS).build()
  private val awaitingPongs = ConcurrentHashMap<Bytes32, EndpointVerification>()
  private val awaitingENRs = ConcurrentHashMap<Bytes32, ENRRequest>()
  private val findNodeStates: Cache<SECP256K1.PublicKey, FindNodeState> =
    CacheBuilder.newBuilder().expireAfterAccess(FIND_NODES_CACHE_EXPIRY, TimeUnit.MILLISECONDS)
      .removalListener<SECP256K1.PublicKey, FindNodeState> { it.value?.close() }
      .build()

  override var invalidPackets: Long by AtomicLong(0)
  override var selfPackets: Long by AtomicLong(0)
  override var expiredPackets: Long by AtomicLong(0)
  override var filteredPackets: Long by AtomicLong(0)
  override var unvalidatedPeerPackets: Long by AtomicLong(0)
  override var unexpectedPongs: Long by AtomicLong(0)
  override var unexpectedENRResponses: Long by AtomicLong(0)
  override var unexpectedNeighbors: Long by AtomicLong(0)

  init {
    start()
  }

  fun start() = launch {
    server.handler { receiveDatagram(it) }.listen(bindAddress.port(), bindAddress.host()).await()
    val endpoint = Endpoint(
      advertiseAddress ?: (server.localAddress()).host(),
      advertiseUdpPort ?: server.localAddress().port(),
      advertiseTcpPort
    )
    enr = EthereumNodeRecord.toRLP(
      keyPair,
      seq,
      enrData,
      null,
      InetAddress.getByName(endpoint.address),
      endpoint.tcpPort,
      endpoint.udpPort
    )
    selfEndpoint = endpoint
    refreshLoop = launch {
      while (true) {
        delay(REFRESH_INTERVAL_MS)
        refresh()
      }
    }
    launch {
      AsyncCompletion.allOf(
        bootstrapURIs.map { uri ->
          asyncCompletion { bootstrapFrom(uri) }
        }
      ).thenRun {
        bootstrapped.complete()
      }.await()
    }

    logger.info("{}: started, listening on {}", serviceDescriptor, server.localAddress())
  }

  private suspend fun bootstrapFrom(uri: URI): Boolean {
    logger.info("Starting verification of bootnode $uri")
    val peer = peerRepository.get(uri)
    logger.info("Starting verification of bootnode $uri")
    try {
      val result = withTimeout(BOOTSTRAP_PEER_VERIFICATION_TIMEOUT_MS) {
        endpointVerification(peer.endpoint, peer).verifyWithRetries()
      } ?: return false
      if (result.peer != peer) {
        logger.warn(
          "{}: ignoring bootstrap peer {} - responding node used a different node-id",
          serviceDescriptor,
          uri
        )
        return false
      }
      logger.info("{}: verified bootstrap peer {}", serviceDescriptor, uri)
      addToRoutingTable(peer)
      findNodes(peer, nodeId).thenRun {
        logger.info("{}: completed bootstrapping from {}", serviceDescriptor, uri)
      }
      return true
    } catch (_: TimeoutCancellationException) {
      logger.warn("{}: timeout verifying bootstrap node {}", serviceDescriptor, uri)
      return false
    }
  }

  private fun receiveDatagram(packet: DatagramPacket) {
    // do quick sanity checks and discard bad packets before launching a co-routine
    if (packet.data().length() < Packet.MIN_SIZE) {
      logger.debug("{}: ignoring under-sized packet with source {}", serviceDescriptor, packet.sender())
      ++invalidPackets
      return
    }
    if (packet.data().length() > Packet.MAX_SIZE) {
      logger.debug("{}: ignoring over-sized packet with source {}", serviceDescriptor, packet.sender())
      ++invalidPackets
      return
    }

    val arrivalTime = timeSupplier()
    launch {
      try {
        receivePacket(packet.data(), packet.sender(), arrivalTime)
      } catch (e: Throwable) {
        logger.error("$serviceDescriptor: unexpected error during packet handling", e)
      }
    }
  }

  override suspend fun awaitBootstrap() = bootstrapped.await()

  override fun awaitBootstrapAsync(): AsyncCompletion = bootstrapped

  override suspend fun shutdown() {
    if (shutdown.compareAndSet(false, true)) {
      logger.info("{}: shutdown", serviceDescriptor)
      server.close().await()
      for (pending in awaitingPongs.values) {
        pending.complete(null)
      }
      awaitingPongs.clear()
      verifyingEndpoints.invalidateAll()
      verifyingEndpoints.cleanUp()
      findNodeStates.invalidateAll()
      findNodeStates.cleanUp()
      refreshLoop?.cancel()
    }
  }

  override fun shutdownAsync(): AsyncCompletion = asyncCompletion { shutdown() }

  override suspend fun lookup(target: SECP256K1.PublicKey): List<Peer> {
    val targetId = target.bytesArray()
    val results = neighbors(target).toMutableList()
    logger.debug("Initial neighbors query $results")

    // maybe add ourselves to the set
    val selfPeer = peerRepository.get(selfEndpoint!!.address, selfEndpoint!!.udpPort, nodeId)
    results.orderedInsert(selfPeer) { a, _ -> targetId.xorDistCmp(a.nodeId.bytesArray(), nodeId.bytesArray()) }
    results.removeAt(results.lastIndex)

    withTimeout(LOOKUP_RESPONSE_TIMEOUT_MS) {
      results.map { nodeToQuery ->
        async {
          val nodes = findNodes(nodeToQuery, target).await()
          for (node in ArrayList(nodes)) {
            val peer = peerRepository.get(selfEndpoint!!.address, selfEndpoint!!.udpPort, node.nodeId)
            if (!results.contains(peer)) {
              results.orderedInsert(peer) { a, _ -> targetId.xorDistCmp(a.nodeId.bytesArray(), node.nodeId.bytesArray()) }
              results.removeAt(results.lastIndex)
            }
          }
        }
      }
    }.awaitAll()
    return results
  }

  override fun lookupAsync(target: SECP256K1.PublicKey) = asyncResult { lookup(target) }

  private suspend fun refresh() {
    logger.debug("{}: table refresh triggered", serviceDescriptor)
    // TODO: instead of a random target, choose a target to optimally fill the peer table
    lookup(SECP256K1.KeyPair.random().publicKey())
  }

  private suspend fun receivePacket(datagram: Buffer, address: SocketAddress, arrivalTime: Long) {
    val packet: Packet
    try {
      packet = Packet.decodeFrom(Bytes.wrap(datagram.bytes))
    } catch (e: DecodingException) {
      logger.debug("{}: ignoring invalid packet from {}", serviceDescriptor, address)
      ++invalidPackets
      return
    }

    if (packet.nodeId == nodeId) {
      logger.debug("{}: ignoring packet from self", serviceDescriptor)
      ++selfPackets
      return
    }

    if (packet.isExpired(arrivalTime - PACKET_EXPIRATION_CHECK_GRACE_MS)) {
      logger.debug("{}: ignoring expired packet", serviceDescriptor)
      ++expiredPackets
      return
    }

    if (packetFilter?.invoke(packet.nodeId, address) == false) {
      logger.debug("{}: packet rejected by filter", serviceDescriptor)
      ++filteredPackets
      return
    }

    when (packet) {
      is PingPacket -> handlePing(packet, address, arrivalTime)
      is PongPacket -> handlePong(packet, address, arrivalTime)
      is FindNodePacket -> handleFindNode(packet, address, arrivalTime)
      is NeighborsPacket -> handleNeighbors(packet, address)
      is ENRRequestPacket -> handleENRRequest(packet, address, arrivalTime)
      is ENRResponsePacket -> handleENRResponse(packet, address, arrivalTime)
    }.let {} // guarantees "when" matching is exhaustive
  }

  private suspend fun handlePing(packet: PingPacket, from: SocketAddress, arrivalTime: Long) {
    // COMPATIBILITY: The ping packet should contain the canonical endpoint for the peer, yet it is often observed to
    // be incorrect (using private-subnet addresses, wildcard addresses, etc). So instead, respond to the source
    // address of the packet itself.
    val fromEndpoint = Endpoint(from, packet.from.tcpPort)
    val peer = peerRepository.get(from.host(), from.port(), packet.nodeId)
    // update the endpoint if the peer does not have one that's been proven
    var currentEndpoint = peer.updateEndpoint(fromEndpoint, arrivalTime, arrivalTime - ENDPOINT_PROOF_LONGEVITY_MS)
    if (currentEndpoint.tcpPort != packet.from.tcpPort) {
      currentEndpoint = peer.updateEndpoint(
        Endpoint(currentEndpoint.address, currentEndpoint.udpPort, packet.from.tcpPort),
        arrivalTime
      )
    }

    val pong = PongPacket.create(keyPair, timeSupplier(), currentEndpoint, packet.hash, seq)
    sendPacket(from, pong)
    // https://github.com/ethereum/devp2p/blob/master/discv4.md#ping-packet-0x01 also suggests sending a ping
    // packet if the peer is unknown, however sending two packets in response to a single incoming would allow a
    // traffic amplification attack
  }

  private suspend fun handlePong(packet: PongPacket, from: SocketAddress, arrivalTime: Long) {
    val pending = awaitingPongs.remove(packet.pingHash) ?: run {
      logger.debug("{}: received unexpected or late pong from {}", serviceDescriptor, from)
      ++unexpectedPongs
      return
    }

    val sender = pending.peer
    // COMPATIBILITY: If the node-id's don't match, the pong should probably be rejected. However, if a different
    // peer is listening at the same address, it will respond to the ping with its node-id. Instead of rejecting,
    // accept the pong and update the new peer record with the proven endpoint, preferring to keep its current
    // tcpPort and otherwise keeping the tcpPort of the original peer.
    val peer =
      if (sender.nodeId == packet.nodeId) sender else peerRepository.get(from.host(), from.port(), packet.nodeId)
    val endpoint = if (peer.verifyEndpoint(pending.endpoint, arrivalTime)) {
      pending.endpoint
    } else {
      val endpoint = peer.endpoint.tcpPort?.let { port ->
        Endpoint(pending.endpoint.address, pending.endpoint.udpPort, port)
      } ?: pending.endpoint
      peer.updateEndpoint(endpoint, arrivalTime)
      peer.verifyEndpoint(endpoint, arrivalTime)
      endpoint
    }

    if (sender.nodeId == packet.nodeId) {
      logger.debug("{}: verified peer endpoint {} (node-id: {})", serviceDescriptor, endpoint.address, peer.nodeId)
    } else {
      logger.debug(
        "{}: verified peer endpoint {} (node-id: {} - changed from {})",
        serviceDescriptor,
        endpoint.address,
        peer.nodeId,
        sender.nodeId
      )
    }

    pending.complete(VerificationResult(peer, endpoint))

    if (packet.enrSeq != null) {
      if (peer.enr == null || peer.enr!!.seq() < packet.enrSeq) {
        val now = timeSupplier()
        withTimeoutOrNull(ENR_REQUEST_TIMEOUT_MS) { enrRequest(endpoint, peer).verify(now) }
      }
    }
  }

  private suspend fun handleFindNode(packet: FindNodePacket, from: SocketAddress, arrivalTime: Long) {
    // if the peer has not been validated, delay sending neighbors until it is
    val peer = peerRepository.get(from.host(), from.port(), packet.nodeId)
    val (_, endpoint) = ensurePeerIsValid(peer, from, arrivalTime) ?: run {
      logger.debug("{}: received findNode from {} which cannot be validated", serviceDescriptor, from)
      ++unvalidatedPeerPackets
      return
    }

    logger.debug("{}: received findNode from {} for target-id {}", serviceDescriptor, from, packet.target)
    val nodes = neighbors(packet.target).map { p -> p.toNode() }

    val address = endpoint.udpSocketAddress
    NeighborsPacket.createRequired(keyPair, timeSupplier(), nodes).forEach { p ->
      logger.debug("{}: sending {} neighbors to {}", serviceDescriptor, p.nodes.size, address)
      sendPacket(address, p)
    }
  }

  private fun handleNeighbors(packet: NeighborsPacket, from: SocketAddress) {
    findNodeStates.getIfPresent(packet.nodeId)?.let { state ->
      for (node in packet.nodes) {
        launch {
          logger.debug("{}: received neighbour {} from {}", serviceDescriptor, node.endpoint.address, from)
          val neighbor = peerRepository.get(from.host(), from.port(), node.nodeId)
          val now = timeSupplier()
          neighbor.updateEndpoint(node.endpoint, now, now - ENDPOINT_PROOF_LONGEVITY_MS)

          withTimeoutOrNull(PEER_VERIFICATION_TIMEOUT_MS) {
            endpointVerification(node.endpoint, neighbor).verify(now)
          }?.let { result ->
            logger.debug(
              "{}: adding {} to the routing table (node-id: {})",
              serviceDescriptor,
              result.endpoint.address,
              result.peer.nodeId
            )
            addToRoutingTable(result.peer)
          }
        }
      }
      state.receive(packet.nodes)
    } ?: run {
      logger.debug("{}: received unexpected or late neighbors packet from {}", serviceDescriptor, from)
      ++unexpectedNeighbors
    }
  }

  private suspend fun handleENRRequest(packet: ENRRequestPacket, from: SocketAddress, arrivalTime: Long) {
    val peer = peerRepository.get(from.host(), from.port(), packet.nodeId)
    val (_, endpoint) = ensurePeerIsValid(peer, from, arrivalTime) ?: run {
      logger.debug("{}: received enrRequest from {} which cannot be validated", serviceDescriptor, from)
      ++unvalidatedPeerPackets
      return
    }

    logger.debug("{}: received enrRequest from {}", serviceDescriptor, from)

    val address = endpoint.udpSocketAddress
    sendPacket(address, ENRResponsePacket.create(keyPair, timeSupplier(), packet.hash, enr!!))
  }

  private suspend fun handleENRResponse(packet: ENRResponsePacket, from: SocketAddress, arrivalTime: Long) {
    val pending = awaitingENRs.remove(packet.requestHash) ?: run {
      logger.debug("{}: received unexpected or late enr response from {}", serviceDescriptor, from)
      ++unexpectedENRResponses
      return
    }
    packet.requestHash
    val sender = pending.peer
    // COMPATIBILITY: If the node-id's don't match, the pong should probably be rejected. However, if a different
    // peer is listening at the same address, it will respond to the ping with its node-id. Instead of rejecting,
    // accept the pong and update the new peer record with the proven endpoint, preferring to keep its current
    // tcpPort and otherwise keeping the tcpPort of the original peer.
    val peer =
      if (sender.nodeId == packet.nodeId) sender else peerRepository.get(from.host(), from.port(), packet.nodeId)

    val enr = EthereumNodeRecord.fromRLP(packet.enr)
    try {
      enr.validate()
    } catch (e: InvalidNodeRecordException) {
      logger.debug("Invalid ENR", e)
      return
    }

    peer.updateENR(enr, arrivalTime)

    pending.complete(ENRResult(peer, enr))
  }

  private fun addToRoutingTable(peer: Peer) {
    routingTable.add(peer)?.let { contested ->
      launch {
        contested.endpoint.let { endpoint ->
          withTimeoutOrNull(PEER_VERIFICATION_TIMEOUT_MS) { endpointVerification(endpoint, contested).verify() }
        } ?: routingTable.evict(contested)
      }
    }
  }

  private suspend fun ensurePeerIsValid(
    peer: Peer,
    address: SocketAddress,
    arrivalTime: Long
  ): VerificationResult? {
    val now = timeSupplier()
    peer.getEndpoint(now - ENDPOINT_PROOF_LONGEVITY_MS)?.let { endpoint ->
      // already valid
      return VerificationResult(peer, endpoint)
    }

    val endpoint = peer.updateEndpoint(
      Endpoint(address, peer.endpoint.tcpPort),
      arrivalTime,
      now - ENDPOINT_PROOF_LONGEVITY_MS
    )
    return withTimeoutOrNull(PEER_VERIFICATION_TIMEOUT_MS) { endpointVerification(endpoint, peer).verify(now) }
  }

  private fun endpointVerification(endpoint: Endpoint, peer: Peer) =
    verifyingEndpoints.get(endpoint.udpSocketAddress) { EndpointVerification(endpoint, peer) }

  // a representation of the state and current action for verifying an endpoint,
  // to avoid concurrent attempts to verify the same endpoint
  private inner class EndpointVerification(val endpoint: Endpoint, val peer: Peer) {
    private val deferred = CompletableDeferred<VerificationResult?>()

    @Volatile
    private var active: Job? = null
    private var nextPingMs: Long = 0
    private var retryDelay: Long = 0

    suspend fun verify(now: Long = timeSupplier()): VerificationResult? {
      if (!deferred.isCompleted) {
        // if not already actively pinging and enough time has passed since the last ping, send a single ping
        synchronized(this) {
          if (active?.isCompleted != false && now >= nextPingMs) {
            nextPingMs = now + RESEND_DELAY_MS
            launch { sendPing(now) }
          }
        }
      }
      return deferred.await()
    }

    suspend fun verifyWithRetries(): VerificationResult? {
      if (!deferred.isCompleted) {
        // if not already actively pinging, start pinging with retries
        synchronized(this) {
          if (active?.isCompleted != false) {
            active = launch {
              repeat(PING_RETRIES) {
                delay(nextPingMs - timeSupplier())
                nextPingMs = timeSupplier() + RESEND_DELAY_MS
                retryDelay += RESEND_DELAY_INCREASE_MS
                if (retryDelay > RESEND_MAX_DELAY_MS) {
                  retryDelay = RESEND_MAX_DELAY_MS
                }
                sendPing()
              }
            }
          }
        }
      }
      return deferred.await()
    }

    private suspend fun sendPing(now: Long = timeSupplier()) {
      val pingPacket = PingPacket.create(keyPair, now, selfEndpoint!!, endpoint, seq)

      // create local references to be captured in the closure, rather than the whole packet instance
      val hash = pingPacket.hash
      val timeout = pingPacket.expiration - now

      // very unlikely that there is another ping packet created with the same hash yet a different EndpointVerification
      // instance, but if there is then the first will be waiting on a deferred that never completes and will
      // eventually time out
      if (awaitingPongs.put(hash, this) != this) {
        launch {
          delay(timeout)
          awaitingPongs.remove(hash)
        }
        sendPacket(endpoint.udpSocketAddress, pingPacket)
      }
    }

    fun complete(result: VerificationResult?): Boolean {
      active?.cancel()
      return deferred.complete(result)
    }
  }

  private data class VerificationResult(
    /** The peer that responded to the verification request. */
    val peer: Peer,
    /**
     * The endpoint that was verified.
     *
     * This will typically be the same as peer.endpoint, but may not be due to concurrent updates.
     */
    val endpoint: Endpoint
  )

  private fun enrRequest(endpoint: Endpoint, peer: Peer) =
    requestingENRs.get(endpoint.udpSocketAddress) { ENRRequest(endpoint, peer) }

  // a representation of the state and current action for querying an ENR from a peer,
  // to avoid concurrent attempts to request the same information.
  private inner class ENRRequest(val endpoint: Endpoint, val peer: Peer) {
    private val deferred = CompletableDeferred<ENRResult?>()

    @Volatile
    private var active: Job? = null
    private var nextENRRequest: Long = 0

    suspend fun verify(now: Long = timeSupplier()): ENRResult? {
      if (!deferred.isCompleted) {
        // if not already actively requesting and enough time has passed since the last request, send a single request
        synchronized(this) {
          if (active?.isCompleted != false && now >= nextENRRequest) {
            nextENRRequest = now + RESEND_DELAY_MS
            launch { sendENRRequest(now) }
          }
        }
      }
      return deferred.await()
    }

    private suspend fun sendENRRequest(now: Long = timeSupplier()) {
      val enrRequestPacket = ENRRequestPacket.create(keyPair, now)

      // create local references to be captured in the closure, rather than the whole packet instance
      val hash = enrRequestPacket.hash
      val timeout = enrRequestPacket.expiration - now

      // very unlikely that there is another ping packet created with the same hash yet a different ENRRequest
      // instance, but if there is then the first will be waiting on a deferred that never completes and will
      // eventually time out
      if (awaitingENRs.put(hash, this) != this) {
        launch {
          delay(timeout)
          awaitingENRs.remove(hash)
        }
        sendPacket(endpoint.udpSocketAddress, enrRequestPacket)
      }
    }

    fun complete(result: ENRResult?): Boolean {
      active?.cancel()
      return deferred.complete(result)
    }
  }

  private data class ENRResult(
    val peer: Peer,
    val enr: EthereumNodeRecord
  )

  private suspend fun findNodes(peer: Peer, target: SECP256K1.PublicKey): AsyncResult<List<Node>> {
    if (peer.nodeId == nodeId) {
      // for queries to self, respond directly
      return AsyncResult.completed(neighbors(target).map { p -> p.toNode() })
    }
    return findNodeStates.get(peer.nodeId) { FindNodeState(peer) }.findNodes(target)
  }

  private fun neighbors(target: SECP256K1.PublicKey) = routingTable.nearest(target, DEVP2P_BUCKET_SIZE)

  private data class FindNodeRequest(val target: SECP256K1.PublicKey)

  private inner class FindNodeState(val peer: Peer) {

    private val nodesCollected = Collections.synchronizedList(ArrayList<Node>())
    private val result = AsyncResult.incomplete<List<Node>>()
    private val start = timeSupplier()

    @Volatile
    private var lastReceive: Long = 0

    private suspend fun send(request: FindNodeRequest) {
      try {
        val endpoint = peer.endpoint
        var now = timeSupplier()
        lastReceive = now
        val findNodePacket = FindNodePacket.create(keyPair, now, request.target)
        sendPacket(endpoint.udpSocketAddress, findNodePacket)
        logger.debug("{}: sent findNode to {} for {}", serviceDescriptor, endpoint.udpSocketAddress, request.target)

        // issue a "get" on the state cache, to indicate that this state is still in use
        val state = findNodeStates.getIfPresent(peer.nodeId)
        if (state != this) {
          logger.warn("{}: findNode state for {} has been replaced")
          close()
        }
      } catch (e: TimeoutCancellationException) {
        logger.debug(
          "$serviceDescriptor: Timeout while sending FindNode requests for peer ${peer.nodeId}",
          e
        )
      } catch (e: Exception) {
        logger.error(
          "$serviceDescriptor: Error while sending FindNode requests for peer ${peer.nodeId}",
          e
        )
      }
    }

    suspend fun findNodes(target: SECP256K1.PublicKey): AsyncResult<List<Node>> {
      send(FindNodeRequest(target))
      return result
    }

    fun receive(nodes: List<Node>) {
      lastReceive = timeSupplier()
      nodesCollected.addAll(nodes)
      if (lastReceive - start > FIND_NODES_QUERY_GAP_MS) {
        close()
      }
    }

    fun close() {
      result.complete(nodesCollected)
    }
  }

  private suspend fun sendPacket(address: SocketAddress, packet: Packet) {
    server.send(Buffer.buffer(packet.encode().toArrayUnsafe()), address.port(), address.host()).await()
  }
}
