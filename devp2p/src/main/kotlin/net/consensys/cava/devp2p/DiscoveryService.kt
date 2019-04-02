/*
 * Copyright 2018 ConsenSys AG.
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
package org.apache.tuweni.devp2p

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.CoroutineLatch
import org.apache.tuweni.concurrent.coroutines.asyncCompletion
import org.apache.tuweni.concurrent.coroutines.asyncResult
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.kademlia.orderedInsert
import org.apache.tuweni.kademlia.xorDistCmp
import org.apache.tuweni.net.coroutines.CommonCoroutineGroup
import org.apache.tuweni.net.coroutines.CoroutineChannelGroup
import org.apache.tuweni.net.coroutines.CoroutineDatagramChannel
import org.logl.LogMessage.patternFormat
import org.logl.LoggerProvider
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

internal const val PACKET_EXPIRATION_PERIOD_MS: Long = (20 * 1000) // 20 seconds
internal const val PACKET_EXPIRATION_CHECK_GRACE_MS: Long = (5 * 1000) // 5 seconds
internal const val PEER_VERIFICATION_TIMEOUT_MS: Long = (22 * 1000) // 22 seconds (packet expiration + a little)
internal const val PEER_VERIFICATION_RETRY_DELAY_MS: Long = (5 * 60 * 1000) // 5 minutes
internal const val BOOTSTRAP_PEER_VERIFICATION_TIMEOUT_MS: Long = (2 * 60 * 1000) // 2 minutes
internal const val REFRESH_INTERVAL_MS: Long = (60 * 1000) // 1 minute
internal const val PING_RETRIES: Int = 20
internal const val RESEND_DELAY_MS: Long = 1000 // 1 second
internal const val RESEND_DELAY_INCREASE_MS: Long = 500 // 500 milliseconds
internal const val RESEND_MAX_DELAY_MS: Long = (30 * 1000) // 30 seconds
internal const val ENDPOINT_PROOF_LONGEVITY_MS: Long = (12 * 60 * 60 * 1000) // 12 hours
internal const val FIND_NODES_CACHE_EXPIRY: Long = (3 * 60 * 1000) // 3 minutes
internal const val FIND_NODES_QUERY_GAP_MS: Long = (30 * 1000) // 30 seconds
internal const val LOOKUP_RESPONSE_TIMEOUT_MS: Long = 500 // 500 milliseconds

/**
 * An Ethereum ÐΞVp2p discovery service.
 *
 * @author Chris Leishman - https://cleishm.github.io/
 */
interface DiscoveryService {

  companion object {
    internal val CURRENT_TIME_SUPPLIER: () -> Long = { System.currentTimeMillis() }
    internal val DEFAULT_BUFFER_ALLOCATOR = { ByteBuffer.allocate(Packet.MAX_SIZE) }

    /**
     * Start the discovery service.
     *
     * @param keyPair the local node's keypair
     * @param port the port to listen on (defaults to `0`, which will cause a random free port to be chosen)
     * @param host the host name or IP address of the interface to bind to (defaults to `null`, which will cause the
     *         service to listen on all interfaces
     * @param bootstrapURIs the URIs for bootstrap nodes
     * @param peerRepository a [PeerRepository] for obtaining [Peer] instances
     * @param advertiseAddress the IP address to advertise to peers, or `null` if the address of the first bound
     *         interface should be used.
     * @param advertiseUdpPort the UDP port to advertise to peers, or `null` if the bound port should to be used.
     * @param advertiseTcpPort the TCP port to advertise to peers, or `null` if it should be the same as the UDP port.
     * @param routingTable a [PeerRoutingTable] which handles the ÐΞVp2p routing table
     * @param packetFilter a filter for incoming packets
     * @param loggerProvider a provider for a logger
     * @param channelGroup the [CoroutineChannelGroup] for network channels created by this service
     * @param bufferAllocator a [ByteBuffer] allocator, which must return buffers of size 1280 bytes or larger
     * @param timeSupplier a function supplying the current time, in milliseconds since the epoch
     */
    @JvmOverloads
    fun open(
      keyPair: SECP256K1.KeyPair,
      port: Int = 0,
      host: String? = null,
      bootstrapURIs: List<URI> = emptyList(),
      peerRepository: PeerRepository = EphemeralPeerRepository(),
      advertiseAddress: InetAddress? = null,
      advertiseUdpPort: Int? = null,
      advertiseTcpPort: Int? = null,
      routingTable: PeerRoutingTable = DevP2PPeerRoutingTable(keyPair.publicKey()),
      packetFilter: ((SECP256K1.PublicKey, InetSocketAddress) -> Boolean)? = null,
      loggerProvider: LoggerProvider = LoggerProvider.nullProvider(),
      channelGroup: CoroutineChannelGroup = CommonCoroutineGroup,
      bufferAllocator: () -> ByteBuffer = DEFAULT_BUFFER_ALLOCATOR,
      timeSupplier: () -> Long = CURRENT_TIME_SUPPLIER
    ): DiscoveryService {
      val bindAddress = if (host == null) InetSocketAddress(port) else InetSocketAddress(host, port)
      return open(
        keyPair,
        bindAddress,
        bootstrapURIs,
        peerRepository,
        advertiseAddress,
        advertiseUdpPort,
        advertiseTcpPort,
        routingTable,
        packetFilter,
        loggerProvider,
        channelGroup,
        bufferAllocator,
        timeSupplier
      )
    }

    /**
     * Start the discovery service.
     *
     * @param keyPair the local node's keypair
     * @param bindAddress the address to listen on
     * @param bootstrapURIs the URIs for bootstrap nodes
     * @param peerRepository a [PeerRepository] for obtaining [Peer] instances
     * @param advertiseAddress the IP address to advertise for incoming packets
     * @param advertiseUdpPort the UDP port to advertise to peers, or `null` if the bound port should to be used.
     * @param advertiseTcpPort the TCP port to advertise to peers, or `null` if it should be the same as the UDP port.
     * @param routingTable a [PeerRoutingTable] which handles the ÐΞVp2p routing table
     * @param packetFilter a filter for incoming packets
     * @param loggerProvider a provider for a logger
     * @param channelGroup the [CoroutineChannelGroup] for network channels created by this service
     * @param bufferAllocator a [ByteBuffer] allocator, which must return buffers of size 1280 bytes or larger
     * @param timeSupplier a function supplying the current time, in milliseconds since the epoch
     */
    @JvmOverloads
    fun open(
      keyPair: SECP256K1.KeyPair,
      bindAddress: InetSocketAddress,
      bootstrapURIs: List<URI> = emptyList(),
      peerRepository: PeerRepository = EphemeralPeerRepository(),
      advertiseAddress: InetAddress? = null,
      advertiseUdpPort: Int? = null,
      advertiseTcpPort: Int? = null,
      routingTable: PeerRoutingTable = DevP2PPeerRoutingTable(keyPair.publicKey()),
      packetFilter: ((SECP256K1.PublicKey, InetSocketAddress) -> Boolean)? = null,
      loggerProvider: LoggerProvider = LoggerProvider.nullProvider(),
      channelGroup: CoroutineChannelGroup = CommonCoroutineGroup,
      bufferAllocator: () -> ByteBuffer = DEFAULT_BUFFER_ALLOCATOR,
      timeSupplier: () -> Long = CURRENT_TIME_SUPPLIER
    ): DiscoveryService {
      return CoroutineDiscoveryService(
        keyPair, bindAddress, bootstrapURIs, advertiseAddress, advertiseUdpPort, advertiseTcpPort, peerRepository,
        routingTable, packetFilter, loggerProvider, channelGroup, bufferAllocator, timeSupplier
      )
    }
  }

  /**
   * `true` if the service has been shutdown
   */
  val isShutdown: Boolean

  /**
   * `true` if the service has terminated
   */
  val isTerminated: Boolean

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
   * @return the number of bootstrap peers successfully added
   */
  suspend fun awaitBootstrap(): Int

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
   * Request shutdown of this service. The service will terminate at a later time (see [DiscoveryService.awaitTermination]).
   */
  fun shutdown()

  /**
   * Suspend until this service has terminated.
   */
  suspend fun awaitTermination()

  /**
   * Provide a completion that will complete when the service has terminated.
   *
   * @return A completion that will complete when the service has terminated.
   */
  fun awaitTerminationAsync(): AsyncCompletion

  /**
   * Shutdown this service immediately.
   */
  fun shutdownNow()

  val invalidPackets: Long
  val selfPackets: Long
  val expiredPackets: Long
  val filteredPackets: Long
  val unvalidatedPeerPackets: Long
  val unexpectedPongs: Long
  val unexpectedNeighbors: Long
}

internal class CoroutineDiscoveryService(
  private val keyPair: SECP256K1.KeyPair,
  bindAddress: InetSocketAddress,
  bootstrapURIs: List<URI> = emptyList(),
  advertiseAddress: InetAddress? = null,
  advertiseUdpPort: Int? = null,
  advertiseTcpPort: Int? = null,
  private val peerRepository: PeerRepository = EphemeralPeerRepository(),
  private val routingTable: PeerRoutingTable = DevP2PPeerRoutingTable(keyPair.publicKey()),
  private val packetFilter: ((SECP256K1.PublicKey, InetSocketAddress) -> Boolean)? = null,
  loggerProvider: LoggerProvider = LoggerProvider.nullProvider(),
  channelGroup: CoroutineChannelGroup = CommonCoroutineGroup,
  private val bufferAllocator: () -> ByteBuffer = DiscoveryService.DEFAULT_BUFFER_ALLOCATOR,
  private val timeSupplier: () -> Long = DiscoveryService.CURRENT_TIME_SUPPLIER,
  private val channel: CoroutineDatagramChannel = CoroutineDatagramChannel.open(channelGroup)
) : DiscoveryService, CoroutineScope {

  private val logger = loggerProvider.getLogger(DiscoveryService::class.java)
  private val serviceDescriptor = "ÐΞVp2p discovery " + System.identityHashCode(this)

  private val selfEndpoint: Endpoint

  private val job = Job()
  // override the default exception handler, which dumps to stderr
  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Default + CoroutineExceptionHandler { _, _ -> }

  private val activityLatch = CoroutineLatch(1)
  private val bootstrapperCount: Deferred<Int>
  private val refreshLoop: Job

  override val isShutdown: Boolean
    get() = !channel.isOpen

  override val isTerminated: Boolean
    get() = activityLatch.isOpen

  override val localPort: Int
    get() = channel.localPort

  override val nodeId: SECP256K1.PublicKey
    get() = keyPair.publicKey()

  private val verifyingEndpoints: Cache<InetSocketAddress, EndpointVerification> =
    CacheBuilder.newBuilder().expireAfterAccess(PEER_VERIFICATION_RETRY_DELAY_MS, TimeUnit.MILLISECONDS).build()
  private val awaitingPongs = ConcurrentHashMap<Bytes32, EndpointVerification>()
  private val findNodeStates: Cache<SECP256K1.PublicKey, FindNodeState> =
    CacheBuilder.newBuilder().expireAfterAccess(FIND_NODES_CACHE_EXPIRY, TimeUnit.MILLISECONDS)
      .removalListener<SECP256K1.PublicKey, FindNodeState> { it.value.close() }
      .build()

  override var invalidPackets: Long by AtomicLong(0)
  override var selfPackets: Long by AtomicLong(0)
  override var expiredPackets: Long by AtomicLong(0)
  override var filteredPackets: Long by AtomicLong(0)
  override var unvalidatedPeerPackets: Long by AtomicLong(0)
  override var unexpectedPongs: Long by AtomicLong(0)
  override var unexpectedNeighbors: Long by AtomicLong(0)

  init {
    channel.bind(bindAddress)

    selfEndpoint = Endpoint(
      advertiseAddress ?: channel.getAdvertisableAddress()!!,
      advertiseUdpPort ?: channel.localPort,
      advertiseTcpPort
    )

    val bootstrapping = bootstrapURIs.map { uri ->
      activityLatch.countUp()
      async {
        try {
          bootstrapFrom(uri)
        } finally {
          activityLatch.countDown()
        }
      }
    }
    bootstrapperCount = async {
      bootstrapping.awaitAll().sumBy { success -> if (success) 1 else 0 }
    }

    refreshLoop = launch {
      activityLatch.countUp()
      try {
        while (true) {
          delay(REFRESH_INTERVAL_MS)
          refresh()
        }
      } finally {
        activityLatch.countDown()
      }
    }

    logger.info("{}: started, listening on {}", serviceDescriptor, channel.localAddress)
    launch {
      try {
        receivePackets()
      } finally {
        for (pending in awaitingPongs.values) {
          pending.complete(null)
        }
        awaitingPongs.clear()
        verifyingEndpoints.invalidateAll()
        verifyingEndpoints.cleanUp()
        findNodeStates.invalidateAll()
        findNodeStates.cleanUp()
        activityLatch.countDown()
      }
      logger.info("{}: terminated", serviceDescriptor)
    }
  }

  private suspend fun bootstrapFrom(uri: URI): Boolean {
    val (bootstrapNodeId, endpoint) = parseEnodeUri(uri)
    val peer = peerRepository.get(bootstrapNodeId)
    val now = timeSupplier()
    peer.updateEndpoint(endpoint, now)
    try {
      val result = withTimeout(BOOTSTRAP_PEER_VERIFICATION_TIMEOUT_MS) {
        endpointVerification(endpoint, peer).verifyWithRetries()
      } ?: return false
      if (result.peer != peer) {
        logger.warn(
          "{}: ignoring bootstrap peer {} - responding node used a different node-id",
          serviceDescriptor, uri
        )
        return false
      }
      logger.info("{}: verified bootstrap peer {}", serviceDescriptor, uri)
      addToRoutingTable(peer)
      findNodes(peer, nodeId)
      logger.info("{}: completed bootstrapping from {}", serviceDescriptor, uri)
      return true
    } catch (_: TimeoutCancellationException) {
      logger.warn("{}: timeout verifying bootstrap node {}", serviceDescriptor, uri)
      return false
    }
  }

  private suspend fun receivePackets() {
    while (channel.isOpen) {
      val datagram = bufferAllocator()
      val address = try {
        channel.receive(datagram)
      } catch (e: ClosedChannelException) {
        break
      }
      datagram.flip()

      // do quick sanity checks and discard bad packets before launching a co-routine
      if (datagram.limit() < Packet.MIN_SIZE) {
        logger.debug("{}: ignoring under-sized packet with source {}", serviceDescriptor, address)
        ++invalidPackets
        continue
      }
      if (datagram.limit() > Packet.MAX_SIZE) {
        logger.debug("{}: ignoring over-sized packet with source {}", serviceDescriptor, address)
        ++invalidPackets
        continue
      }

      activityLatch.countUp()
      val arrivalTime = timeSupplier()
      val job = launch {
        try {
          receivePacket(datagram, address, arrivalTime)
        } catch (e: Throwable) {
          logger.error(patternFormat("{}: unexpected error during packet handling", serviceDescriptor), e)
        }
      }
      job.invokeOnCompletion { activityLatch.countDown() }
      yield()
    }
  }

  override suspend fun awaitBootstrap(): Int = bootstrapperCount.await()

  override fun shutdown() {
    if (channel.isOpen) {
      logger.info("{}: shutdown", serviceDescriptor)
    }
    channel.close()
    refreshLoop.cancel()
  }

  override suspend fun awaitTermination() {
    activityLatch.await()
  }

  override fun awaitTerminationAsync(): AsyncCompletion = asyncCompletion { awaitTermination() }

  override fun shutdownNow() {
    job.cancel()
  }

  @UseExperimental(ObsoleteCoroutinesApi::class)
  override suspend fun lookup(target: SECP256K1.PublicKey): List<Peer> {
    val targetId = target.bytesArray()
    val results = neighbors(target).toMutableList()

    // maybe add ourselves to the set
    val selfPeer = peerRepository.get(nodeId)
    results.orderedInsert(selfPeer) { a, _ -> targetId.xorDistCmp(a.nodeId.bytesArray(), nodeId.bytesArray()) }
    results.removeAt(results.lastIndex)

    val queried = mutableSetOf(selfPeer)

    while (true) {
      val toQuery = results.filterNot { p -> queried.contains(p) }.take(3).toList()
      if (toQuery.isEmpty()) {
        return results
      }
      val nodes = Channel<Node>(capacity = Channel.UNLIMITED)
      toQuery.forEach { p -> findNodes(p, target, nodes) }
      while (true) {
        // stop if no more responses are received after the given time
        val node = withTimeoutOrNull(LOOKUP_RESPONSE_TIMEOUT_MS) { nodes.receiveOrNull() } ?: break
        val peer = peerRepository.get(node.nodeId)
        if (!results.contains(peer)) {
          results.orderedInsert(peer) { a, _ -> targetId.xorDistCmp(a.nodeId.bytesArray(), node.nodeId.bytesArray()) }
          results.removeAt(results.lastIndex)
        }
      }
      queried.addAll(toQuery)
      nodes.close()
    }
  }

  override fun lookupAsync(target: SECP256K1.PublicKey) = asyncResult { lookup(target) }

  private suspend fun refresh() {
    logger.debug("{}: table refresh triggered", serviceDescriptor)
    // TODO: instead of a random target, choose a target to optimally fill the peer table
    lookup(SECP256K1.KeyPair.random().publicKey())
  }

  private suspend fun receivePacket(datagram: ByteBuffer, address: SocketAddress, arrivalTime: Long) {
    if (address !is InetSocketAddress) {
      throw IOException("Datagram received from non-inet socket address: " + address.javaClass)
    }

    val packet: Packet
    try {
      packet = Packet.decodeFrom(datagram)
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
    }.let {} // guarantees "when" matching is exhaustive
  }

  private suspend fun handlePing(packet: PingPacket, from: InetSocketAddress, arrivalTime: Long) {
    // COMPATIBILITY: The ping packet should contain the canonical endpoint for the peer, yet it is often observed to
    // be incorrect (using private-subnet addresses, wildcard addresses, etc). So instead, respond to the source
    // address of the packet itself.
    val fromEndpoint = Endpoint(from, packet.from.tcpPort)
    val peer = peerRepository.get(packet.nodeId)
    // update the endpoint if the peer does not have one that's been proven
    var currentEndpoint = peer.updateEndpoint(fromEndpoint, arrivalTime, arrivalTime - ENDPOINT_PROOF_LONGEVITY_MS)
    if (currentEndpoint.tcpPort != packet.from.tcpPort) {
      currentEndpoint = peer.updateEndpoint(
        Endpoint(currentEndpoint.address, currentEndpoint.udpPort, packet.from.tcpPort),
        arrivalTime
      )
    }

    val pong = PongPacket.create(keyPair, timeSupplier(), currentEndpoint, packet.hash)
    sendPacket(from, pong)
    // https://github.com/ethereum/devp2p/blob/master/discv4.md#ping-packet-0x01 also suggests sending a ping
    // packet if the peer is unknown, however sending two packets in response to a single incoming would allow a
    // traffic amplification attack
  }

  private suspend fun handlePong(packet: PongPacket, from: InetSocketAddress, arrivalTime: Long) {
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
    val peer = if (sender.nodeId == packet.nodeId) sender else peerRepository.get(packet.nodeId)
    val endpoint = if (peer.verifyEndpoint(pending.endpoint, arrivalTime)) {
      pending.endpoint
    } else {
      val endpoint = peer.endpoint?.tcpPort?.let { port ->
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
        "{}: verified peer endpoint {} (node-id: {} - changed from {})", serviceDescriptor,
        endpoint.address, peer.nodeId, sender.nodeId
      )
    }

    pending.complete(VerificationResult(peer, endpoint))
  }

  private suspend fun handleFindNode(packet: FindNodePacket, from: InetSocketAddress, arrivalTime: Long) {
    // if the peer has not been validated, delay sending neighbors until it is
    val peer = peerRepository.get(packet.nodeId)
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

  private fun handleNeighbors(packet: NeighborsPacket, from: InetSocketAddress) {
    findNodeStates.getIfPresent(packet.nodeId)?.let { state ->
      for (node in packet.nodes) {
        launch {
          logger.debug("{}: received neighbour {} from {}", serviceDescriptor, node.endpoint.address, from)
          val neighbor = peerRepository.get(node.nodeId)
          val now = timeSupplier()
          neighbor.updateEndpoint(node.endpoint, now, now - ENDPOINT_PROOF_LONGEVITY_MS)

          withTimeoutOrNull(PEER_VERIFICATION_TIMEOUT_MS) {
            endpointVerification(node.endpoint, neighbor).verify(now)
          }?.let { result ->
            logger.debug(
              "{}: adding {} to the routing table (node-id: {})", serviceDescriptor,
              result.endpoint.address, result.peer.nodeId
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

  private fun addToRoutingTable(peer: Peer) {
    routingTable.add(peer)?.let { contested ->
      launch {
        contested.endpoint?.let { endpoint ->
          withTimeoutOrNull(PEER_VERIFICATION_TIMEOUT_MS) { endpointVerification(endpoint, contested).verify() }
        } ?: routingTable.evict(contested)
      }
    }
  }

  private suspend fun ensurePeerIsValid(
    peer: Peer,
    address: InetSocketAddress,
    arrivalTime: Long
  ): VerificationResult? {
    val now = timeSupplier()
    peer.getEndpoint(now - ENDPOINT_PROOF_LONGEVITY_MS)?.let { endpoint ->
      // already valid
      return VerificationResult(peer, endpoint)
    }

    val endpoint = peer.updateEndpoint(
      Endpoint(address, peer.endpoint?.tcpPort),
      arrivalTime, now - ENDPOINT_PROOF_LONGEVITY_MS
    )
    return withTimeoutOrNull(PEER_VERIFICATION_TIMEOUT_MS) { endpointVerification(endpoint, peer).verify(now) }
  }

  private fun endpointVerification(endpoint: Endpoint, peer: Peer) =
    verifyingEndpoints.get(endpoint.udpSocketAddress) { EndpointVerification(endpoint, peer) }

  // a representation of the state and current action for verifying and endpoint,
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
      val pingPacket = PingPacket.create(keyPair, now, selfEndpoint, endpoint)

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

  @UseExperimental(ObsoleteCoroutinesApi::class)
  private suspend fun findNodes(peer: Peer, target: SECP256K1.PublicKey) {
    // consume all received nodes (and discard), thus suspending until completed
    Channel<Node>(capacity = Channel.CONFLATED).also { findNodes(peer, target, it) }.consumeEach { }
  }

  private suspend fun findNodes(peer: Peer, target: SECP256K1.PublicKey, channel: SendChannel<Node>) {
    if (peer.nodeId == nodeId) {
      // for queries to self, respond directly
      neighbors(target).map { p -> channel.send(p.toNode()) }
      return
    }
    findNodeStates.get(peer.nodeId) { FindNodeState(peer) }.findNodes(target, channel)
  }

  private fun neighbors(target: SECP256K1.PublicKey) = routingTable.nearest(target, DEVP2P_BUCKET_SIZE)

  private data class FindNodeRequest(val target: SECP256K1.PublicKey, val results: SendChannel<Node>)

  private inner class FindNodeState(val peer: Peer) {
    // the protocol doesn't have a correlation mechanism between findNode requests and the associated responses,
    // so requests have to be queued and a delay between them used to try and determine when no more responses
    // will be sent
    private val targets = Channel<FindNodeRequest>(capacity = Channel.UNLIMITED)
    private val job: Job
    @Volatile
    private var results: SendChannel<Node>? = null
    @Volatile
    private var lastReceive: Long = 0

    init {
      job = launch { sendLoop() }
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private suspend fun sendLoop() {
      try {
        while (true) {
          val request = targets.receive()
          if (request.results.isClosedForSend) {
            continue
          }
          val endpoint = peer.endpoint
          if (endpoint == null) {
            request.results.close()
            continue
          }
          var now = timeSupplier()
          results = request.results
          lastReceive = now
          val findNodePacket = FindNodePacket.create(keyPair, now, request.target)
          sendPacket(endpoint.udpSocketAddress, findNodePacket)
          logger.debug("{}: sent findNode to {} for {}", serviceDescriptor, endpoint.udpSocketAddress, request.target)

          // wait for results, only moving onto the next when packets stop arriving for a reasonable period
          do {
            delay(lastReceive - now + FIND_NODES_QUERY_GAP_MS)
            now = timeSupplier()
          } while (now - lastReceive < FIND_NODES_QUERY_GAP_MS)
          results?.close()
          results = null

          // issue a "get" on the state cache, to indicate that this state is still in use
          val state = findNodeStates.getIfPresent(peer.nodeId)
          if (state != this) {
            logger.warn("{}: findNode state for {} has been replaced")
            close()
          }
        }
      } catch (_: ClosedReceiveChannelException) {
        // ignore
      } catch (_: CancellationException) {
        // ignore
      } catch (_: ClosedChannelException) {
        // ignore
      } catch (e: Exception) {
        logger.error(
          patternFormat("{}: Error while sending FindNode requests for peer {}", serviceDescriptor, peer.nodeId),
          e
        )
      }
    }

    suspend fun findNodes(target: SECP256K1.PublicKey, channel: SendChannel<Node>) {
      targets.send(FindNodeRequest(target, channel))
    }

    fun receive(nodes: List<Node>) {
      results?.let { channel ->
        lastReceive = timeSupplier()
        try {
          nodes.forEach { node -> channel.offer(node) }
        } catch (_: ClosedSendChannelException) {
          results = null
        }
      }
    }

    fun close() {
      job.cancel()
      targets.close()
      results?.close()
      while (true) {
        val request = targets.poll() ?: break
        request.results.close()
      }
    }
  }

  private suspend fun sendPacket(address: InetSocketAddress, packet: Packet) {
    val buffer = bufferAllocator()
    packet.encodeTo(buffer)
    buffer.flip()
    channel.send(buffer, address)
  }
}
