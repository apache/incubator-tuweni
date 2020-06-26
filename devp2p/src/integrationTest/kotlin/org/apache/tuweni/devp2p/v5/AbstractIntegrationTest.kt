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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.internal.DefaultAuthenticationProvider
import org.apache.tuweni.devp2p.v5.internal.DefaultPacketCodec
import org.apache.tuweni.devp2p.v5.internal.DefaultUdpConnector
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import org.apache.tuweni.devp2p.v5.storage.DefaultENRStorage
import org.apache.tuweni.devp2p.v5.storage.RoutingTable
import org.apache.tuweni.devp2p.v5.topic.TicketHolder
import org.apache.tuweni.devp2p.v5.topic.TopicTable
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress
import java.net.InetSocketAddress

@ExtendWith(BouncyCastleExtension::class)
abstract class AbstractIntegrationTest {

  @UseExperimental(ExperimentalCoroutinesApi::class)
  protected suspend fun createNode(
    port: Int = 9090,
    bootList: List<String> = emptyList(),
    enrStorage: ENRStorage = DefaultENRStorage(),
    keyPair: SECP256K1.KeyPair = SECP256K1.KeyPair.random(),
    enr: Bytes = EthereumNodeRecord.toRLP(keyPair, ip = InetAddress.getLoopbackAddress(), udp = port),
    routingTable: RoutingTable = RoutingTable(enr),
    address: InetSocketAddress = InetSocketAddress(InetAddress.getLoopbackAddress(), port),
    authenticationProvider: AuthenticationProvider = DefaultAuthenticationProvider(keyPair, routingTable),
    topicTable: TopicTable = TopicTable(),
    ticketHolder: TicketHolder = TicketHolder(),
    packetCodec: PacketCodec = DefaultPacketCodec(
      keyPair,
      routingTable,
      authenticationProvider = authenticationProvider
    ),
    connector: UdpConnector = DefaultUdpConnector(
      address,
      keyPair,
      enr,
      enrStorage,
      nodesTable = routingTable,
      packetCodec = packetCodec,
      authenticationProvider = authenticationProvider,
      topicTable = topicTable,
      ticketHolder = ticketHolder
    ),
    service: NodeDiscoveryService =
      DefaultNodeDiscoveryService.open(
        enrStorage = enrStorage,
        bootstrapENRList = bootList,
        connector = connector,
        coroutineContext = Dispatchers.Unconfined
      )
  ): TestNode {
    service.start()
    return TestNode(
      bootList,
      port,
      enrStorage,
      keyPair,
      enr,
      address,
      routingTable,
      authenticationProvider,
      packetCodec,
      connector,
      service,
      topicTable,
      ticketHolder
    )
  }

  protected suspend fun handshake(initiator: TestNode, recipient: TestNode): Boolean {
    initiator.enrStorage.set(recipient.enr)
    initiator.routingTable.add(recipient.enr)
    val message = RandomMessage()
    initiator.connector.send(recipient.address, message, recipient.nodeId)
    delay(1000)
    return (null != recipient.authenticationProvider.findSessionKey(initiator.nodeId.toHexString()))
  }

  internal suspend fun send(initiator: TestNode, recipient: TestNode, message: UdpMessage) {
    if (message is RandomMessage || message is WhoAreYouMessage) {
      throw IllegalArgumentException("Can't send handshake initiation message")
    }
    initiator.connector.send(recipient.address, message, recipient.nodeId)
  }

  internal suspend inline fun <reified T : UdpMessage> sendAndAwait(
    initiator: TestNode,
    recipient: TestNode,
    message: UdpMessage
  ): T {
    val listener = object : MessageObserver {
      var result: Channel<T> = Channel()

      override fun observe(message: UdpMessage) {
        if (message is T) {
          result.offer(message)
        }
      }
    }

    initiator.connector.attachObserver(listener)
    send(initiator, recipient, message)
    val result = listener.result.receive()
    initiator.connector.detachObserver(listener)
    return result
  }
}

class TestNode(
  val bootList: List<String>,
  val port: Int,
  val enrStorage: ENRStorage,
  val keyPair: SECP256K1.KeyPair,
  val enr: Bytes,
  val address: InetSocketAddress,
  val routingTable: RoutingTable,
  val authenticationProvider: AuthenticationProvider,
  val packetCodec: PacketCodec,
  val connector: UdpConnector,
  val service: NodeDiscoveryService,
  val topicTable: TopicTable,
  val ticketHolder: TicketHolder,
  val nodeId: Bytes = Hash.sha2_256(enr)
)
