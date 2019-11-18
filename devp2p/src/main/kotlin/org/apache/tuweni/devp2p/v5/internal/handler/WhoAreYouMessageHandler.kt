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
package org.apache.tuweni.devp2p.v5.internal.handler

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.MessageHandler
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.misc.HandshakeInitParameters
import org.apache.tuweni.devp2p.v5.packet.FindNodeMessage
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import java.net.InetSocketAddress

class WhoAreYouMessageHandler : MessageHandler<WhoAreYouMessage> {

  override suspend fun handle(
    message: WhoAreYouMessage,
    address: InetSocketAddress,
    srcNodeId: Bytes,
    connector: UdpConnector
  ) {
    // Retrieve enr
    val trackingMessage = connector.getPendingMessage(message.authTag)
    trackingMessage?.let {
      val rlpEnr = connector.getNodeRecords().find(trackingMessage.nodeId)
      rlpEnr?.let {
        val handshakeParams = HandshakeInitParameters(message.idNonce, message.authTag, rlpEnr)

        val response = if (trackingMessage.message is RandomMessage) FindNodeMessage() else trackingMessage.message
        connector.send(address, response, trackingMessage.nodeId, handshakeParams)
      }
    }
  }
}
