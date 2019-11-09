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
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.MessageHandler
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.packet.FindNodeMessage
import org.apache.tuweni.devp2p.v5.packet.PongMessage
import java.net.InetSocketAddress

class PongMessageHandler : MessageHandler<PongMessage> {

  override fun handle(message: PongMessage, address: InetSocketAddress, srcNodeId: Bytes, connector: UdpConnector) {
    val enrBytes = connector.getAwaitingPongRecord(srcNodeId) ?: return
    val enr = EthereumNodeRecord.fromRLP(enrBytes)
    if (enr.seq != message.enrSeq) {
      val request = FindNodeMessage(message.requestId)
      connector.send(address, request, srcNodeId)
    }
  }
}
