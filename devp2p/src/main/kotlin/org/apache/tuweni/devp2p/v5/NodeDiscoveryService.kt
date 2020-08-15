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
package org.apache.tuweni.devp2p.v5

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.coroutines.asyncCompletion
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.io.Base64URLSafe
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/**
 * A creator of discovery service objects.
 */
object DiscoveryService {

  /**
   * Creates a new discovery service, generating the node ENR and configuring the UDP connector.
   * @param keyPair the key pair identifying the node running the service.
   * @param bindAddress the address to bind the node to.
   * @param enrSeq the sequence of the ENR of the node
   * @param bootstrapENRList the list of other nodes to connect to on bootstrap.
   * @param enrStorage the permanent storage of ENRs. Defaults to an in-memory store.
   * @param coroutineContext the coroutine context associated with the store.
   */
  @JvmStatic
  @JvmOverloads
  fun open(
    keyPair: SECP256K1.KeyPair,
    localPort: Int,
    bindAddress: InetSocketAddress = InetSocketAddress(localPort),
    enrSeq: Long = Instant.now().toEpochMilli(),
    bootstrapENRList: List<String> = emptyList(),
    enrStorage: ENRStorage = DefaultENRStorage(),
    coroutineContext: CoroutineContext = Dispatchers.Default
  ): NodeDiscoveryService {
    val selfENR = EthereumNodeRecord.toRLP(
      keyPair,
      enrSeq,
      emptyMap(),
      emptyMap(),
      bindAddress.address,
      null,
      bindAddress.port
    )
    val connector = UdpConnector(bindAddress, keyPair, selfENR, enrStorage)
    return DefaultNodeDiscoveryService.open(bootstrapENRList, enrStorage, connector, coroutineContext)
  }
}
/**
 * Service executes network discovery, according to discv5 specification
 * (https://github.com/ethereum/devp2p/blob/master/discv5/discv5.md)
 */
interface NodeDiscoveryService : CoroutineScope {

  /**
   * Starts the node discovery service.
   */
  suspend fun start()

  /**
   * Stops the node discovery service.
   */
  suspend fun terminate()

  /**
   * Starts the discovery service, providing a handle to the completion of the start operation.
   */
  fun startAsync() = asyncCompletion { start() }

  /**
   * Stops the node discovery service, providing a handle to the completion of the shutdown operation.
   */
  fun terminateAsync() = asyncCompletion { terminate() }
}

internal class DefaultNodeDiscoveryService(
  private val bootstrapENRList: List<String>,
  private val enrStorage: ENRStorage,
  private val connector: UdpConnector,
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : NodeDiscoveryService {

  companion object {

    private val logger = LoggerFactory.getLogger(DefaultNodeDiscoveryService::class.java)
    /**
     * Creates a new discovery service with the UDP service provided.
     * @param bootstrapENRList the list of other nodes to connect to on bootstrap.
     * @param enrStorage the permanent storage of ENRs. Defaults to an in-memory store.
     * @param connector the UDP service providing network access.
     * @param coroutineContext the coroutine context associated with the store.
     */
    @JvmStatic
    @JvmOverloads
    fun open(
      bootstrapENRList: List<String> = emptyList(),
      enrStorage: ENRStorage = DefaultENRStorage(),
      connector: UdpConnector,
      coroutineContext: CoroutineContext = Dispatchers.Default
    ): NodeDiscoveryService {
      return DefaultNodeDiscoveryService(bootstrapENRList, enrStorage, connector, coroutineContext)
    }
  }

  @ObsoleteCoroutinesApi
  override suspend fun start() {
    connector.start()
    bootstrap()
  }

  override suspend fun terminate() {
    connector.terminate()
  }

  suspend fun addPeer(rlpENR: Bytes) {
    val enr: EthereumNodeRecord = EthereumNodeRecord.fromRLP(rlpENR)
    val randomMessage = RandomMessage()
    val address = InetSocketAddress(enr.ip(), enr.udp())

    val destNodeId = Hash.sha2_256(rlpENR)
    logger.trace("About to add new peer {}", address)
    enrStorage.set(rlpENR)
    connector.getNodesTable().add(rlpENR)
    connector.send(address, randomMessage, destNodeId)
  }

  private suspend fun bootstrap() {
    bootstrapENRList.forEach {
      logger.trace("Connecting to bootstrap peer {}", it)
      var encodedEnr = it
      if (it.startsWith("enr:")) {
        encodedEnr = it.substringAfter("enr:")
      }
      val rlpENR = Base64URLSafe.decode(encodedEnr)
      addPeer(rlpENR)
    }
  }
}
