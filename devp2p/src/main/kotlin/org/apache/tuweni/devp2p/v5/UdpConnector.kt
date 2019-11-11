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

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.storage.RoutingTable
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.devp2p.v5.misc.HandshakeInitParameters
import org.apache.tuweni.devp2p.v5.misc.TrackingMessage
import org.apache.tuweni.devp2p.v5.topic.TicketHolder
import org.apache.tuweni.devp2p.v5.topic.TopicRegistrar
import org.apache.tuweni.devp2p.v5.topic.TopicTable
import java.net.InetSocketAddress

/**
 * Module, used for network communication. It accepts and sends incoming messages and also provides peer information,
 * like node's ENR, key pair
 */
interface UdpConnector {

  /**
   * Bootstraps receive loop for incoming message handling
   */
  fun start()

  /**
   * Shut downs both udp receive loop and sender socket
   */
  fun terminate()

  /**
   * Sends udp message by socket address
   *
   * @param address receiver address
   * @param message message to send
   * @param destNodeId destination node identifier
   * @param handshakeParams optional parameter to create handshake
   */
  fun send(
    address: InetSocketAddress,
    message: UdpMessage,
    destNodeId: Bytes,
    handshakeParams: HandshakeInitParameters? = null
  )

  /**
   * Gives information about connector, whether receive channel is working
   *
   * @return availability information
   */
  fun available(): Boolean

  /**
   * Gives information about connector, whether receive loop is working
   *
   * @return availability information
   */
  fun started(): Boolean

  /**
   * Provides node's key pair
   *
   * @return node's key pair
   */
  fun getNodeKeyPair(): SECP256K1.KeyPair

  /**
   * Provides node's ENR in RLP encoded representation
   *
   * @return node's RLP encoded ENR
   */
  fun getEnrBytes(): Bytes

  /**
   * Provides node's ENR
   *
   * @return node's ENR
   */
  fun getEnr(): EthereumNodeRecord

  /**
   * Attach observer for listening processed messages
   *
   * @param observer instance, proceeding observation
   */
  fun attachObserver(observer: MessageObserver)

  /**
   * Remove observer for listening processed message
   *
   * @param observer observer for removal
   */
  fun detachObserver(observer: MessageObserver)

  /**
   * Get kademlia routing table
   *
   * @return kademlia table
   */
  fun getNodesTable(): RoutingTable

  /**
   * Retrieve enr of pinging node
   *
   * @param node identifier
   *
   * @return node record
   */
  fun getAwaitingPongRecord(nodeId: Bytes): Bytes?

  /**
   * Retrieve last sent message, in case if it unauthorized and node can resend with authentication header
   *
   * @param authTag message's authentication tag
   *
   * @return message, including node identifier
   */
  fun getPendingMessage(authTag: Bytes): TrackingMessage

  /**
   * Provides enr storage of known nodes
   *
   * @return nodes storage
   */
  fun getNodeRecords(): ENRStorage

  /**
   * Provides node's topic table
   *
   * @return node's topic table
   */
  fun getTopicTable(): TopicTable

  /**
   * Provides node's ticket holder
   *
   * @return node's ticket holder
   */
  fun getTicketHolder(): TicketHolder

  /**
   * Provides node's topic registrar
   *
   * @return node's topic registrar
   */
  fun getTopicRegistrar(): TopicRegistrar

  /**
   * Provides node's session initiator key
   *
   * @return node's session initiator key
   */
  fun getSessionInitiatorKey(nodeId: Bytes): Bytes
}
