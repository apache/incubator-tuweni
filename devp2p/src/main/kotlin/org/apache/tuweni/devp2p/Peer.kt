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

import org.apache.tuweni.crypto.SECP256K1

/**
 * An Ethereum P2P network peer.
 */
interface Peer {

  /**
   * The nodeId for this peer.
   */
  val nodeId: SECP256K1.PublicKey

  /**
   * The endpoint for communicating with this peer, if known.
   */
  val endpoint: Endpoint

  /**
   * The Ethereum Node Record associated with the peer, if known.
   */
  val enr: EthereumNodeRecord?

  /**
   * The last time the current endpoint of this peer was verified, in milliseconds since the epoch.
   *
   * Endpoint is verified by a ping/pong cycle: https://github.com/ethereum/devp2p/blob/master/discv4.md#endpoint-proof
   */
  val lastVerified: Long?

  /**
   * The time this peer was last seen at its current endpoint, in milliseconds since the epoch.
   */
  val lastSeen: Long?

  /**
   * Get the endpoint for this peer, if it has been verified on or after a specified time.
   *
   * @param ifVerifiedOnOrAfter the earliest time, in milliseconds since the epoch, when the
   *         endpoint of this peer must have been verified
   * @return the endpoint of this peer, if it has been verified on or after the specified time
   */
  fun getEndpoint(ifVerifiedOnOrAfter: Long): Endpoint?

  /**
   * Update the peer with a new endpoint.
   *
   * If successful, the [endpoint] property will be set, the [lastSeen] timestamp will be updated. If the IP address
   * or UDP port of the endpoint was changed, then the [lastVerified] timestamp will be cleared.
   *
   * @param endpoint the endpoint for the peer
   * @param time the time this endpoint information was determined, in milliseconds since the epoch
   * @param ifVerifiedBefore the latest allowable time, in milliseconds since the epoch, when this peer was last
   *         verified at its current endpoint. If this peers endpoint was verified after this time, the endpoint
   *         will not be updated. If `null`, then no check will be made and the endpoint will always be updated.
   * @return the resulting endpoint of the peer
   */
  fun updateEndpoint(endpoint: Endpoint, time: Long, ifVerifiedBefore: Long? = null): Endpoint

  /**
   * Set the [lastVerified] and [lastSeen] time to the provided time, if the endpoint matches.
   *
   * Will only update [lastVerified] and/or [lastSeen] if the new time is more recent than their current values.
   *
   * @param endpoint the endpoint that was verified, which must match this peer's endpoint
   * @param time the time when the endpoint was verified, in milliseconds since the epoch
   * @return `true` if the endpoint matched the endpoint of this peer
   */
  fun verifyEndpoint(endpoint: Endpoint, time: Long): Boolean

  /**
   * Set the [lastSeen] time to the current time.
   *
   * Will only update [lastSeen] if the new time is more recent than their current values.
   *
   * @param time the time when the endpoint was verified, in milliseconds since the epoch
   * @throws IllegalStateException if there is no endpoint for this peer
   */
  fun seenAt(time: Long)

  /**
   * Update the peer's ENR.
   *
   * Will only update if the [seq] is larger than the one associated with the peer.
   *
   * @param record the ENR record associated with the peer
   * @param time the time this endpoint information was determined, in milliseconds since the epoch
   */
  fun updateENR(record: EthereumNodeRecord, time: Long)

  fun uri(): String {
    return "enode://${nodeId.toHexString()}@${endpoint.address}:${endpoint.tcpPort
      ?: 30303}?discPort=${endpoint.udpPort}"
  }
}
