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
package org.apache.tuweni.ethclientui.controller

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.ethclient.EthereumClient
import org.apache.tuweni.peer.repository.Identity
import org.apache.tuweni.units.bigints.UInt256
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import kotlin.streams.toList

data class BlockHashAndNumber(val hash: Bytes32, val number: UInt256)

data class State(val peerCounts: Map<String, Long>, val bestBlocks: Map<String, BlockHashAndNumber>)

data class Peer(
  val id: Identity,
  val networkID: UInt256?,
  val bestHash: Bytes32?,
  val totalDifficulty: UInt256?,
  val lastContacted: Instant?
)
data class Peers(val peers: List<Peer>)

@RestController
@RequestMapping("/rest/state")
class StateService {

  @Autowired
  var client: EthereumClient? = null

  @GetMapping(value = [""], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun get(): State {
    val peerCounts = client!!.peerRepositories.entries.map {
      Pair(it.key, it.value.activeConnections().count())
    }
    val bestBlocks = client!!.storageRepositories.entries.map {
      runBlocking {
        val bestBlock = it.value.retrieveChainHeadHeader()
        Pair(it.key, BlockHashAndNumber(bestBlock.hash, bestBlock.number))
      }
    }
    return State(mapOf(*peerCounts.toTypedArray()), mapOf(*bestBlocks.toTypedArray()))
  }

  @GetMapping(value = ["{id}/peers"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getPeers(@PathVariable id: String): Peers {
    val repository = client!!.peerRepositories[id]
      ?: throw ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "entity not found"
      )
    val peers = Peers(
      repository.activeConnections().map {
        it.peer().id()
        val peer = Peer(
          it.peer().id(),
          it.status()?.networkID,
          it.status()?.bestHash,
          it.status()?.totalDifficulty,
          it.peer().lastContacted()
        )
        peer
      }.toList()
    )

    return peers
  }
}
