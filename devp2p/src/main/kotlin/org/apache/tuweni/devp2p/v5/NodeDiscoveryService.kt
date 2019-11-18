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
import kotlinx.coroutines.launch
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.internal.DefaultUdpConnector
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.storage.DefaultENRStorage
import org.apache.tuweni.io.Base64URLSafe
import java.net.InetSocketAddress
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/**
 * Service executes network discovery, according to discv5 specification
 * (https://github.com/ethereum/devp2p/blob/master/discv5/discv5.md)
 */
interface NodeDiscoveryService {

  /**
   * Initializes node discovery
   */
  suspend fun start()

  /**
   * Executes service shut down
   */
  suspend fun terminate()
}

internal class DefaultNodeDiscoveryService(
  private val keyPair: SECP256K1.KeyPair,
  private val localPort: Int,
  private val bindAddress: InetSocketAddress = InetSocketAddress(localPort),
  private val bootstrapENRList: List<String> = emptyList(),
  private val enrSeq: Long = Instant.now().toEpochMilli(),
  private val selfENR: Bytes = EthereumNodeRecord.toRLP(
    keyPair,
    enrSeq,
    emptyMap(),
    bindAddress.address,
    null,
    bindAddress.port
  ),
  private val enrStorage: ENRStorage = DefaultENRStorage(),
  private val connector: UdpConnector = DefaultUdpConnector(bindAddress, keyPair, selfENR, enrStorage),
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : NodeDiscoveryService, CoroutineScope {

  override suspend fun start() {
    connector.start()
    launch { bootstrap() }
  }

  override suspend fun terminate() {
    connector.terminate()
  }

  private suspend fun bootstrap() {
    bootstrapENRList.forEach {
      if (it.startsWith("enr:")) {
        val encodedEnr = it.substringAfter("enr:")
        val rlpENR = Base64URLSafe.decode(encodedEnr)
        val enr = EthereumNodeRecord.fromRLP(rlpENR)

        val randomMessage = RandomMessage()
        val address = InetSocketAddress(enr.ip(), enr.udp())

        val destNodeId = Hash.sha2_256(rlpENR)
        enrStorage.set(rlpENR)
        connector.getNodesTable().add(rlpENR)
        connector.send(address, randomMessage, destNodeId)
      }
    }
  }
}
