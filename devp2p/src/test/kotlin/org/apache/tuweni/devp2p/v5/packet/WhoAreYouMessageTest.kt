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
package org.apache.tuweni.devp2p.v5.packet

import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.RoutingTable
import org.apache.tuweni.devp2p.v5.Session
import org.apache.tuweni.devp2p.v5.WhoAreYouMessage
import org.apache.tuweni.devp2p.v5.topic.TopicTable
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress
import java.net.InetSocketAddress

@ExtendWith(BouncyCastleExtension::class)
class WhoAreYouMessageTest {

  @Test
  fun decodeSelf() {
    val bytes = Bytes.fromHexString("0x282E641D415A892C05FD03F0AE716BDD92D1569116FDC7C7D3DB39AC5F79B0F7EF8CE56EDC7BB967899B4C48EEA6A0E838C9091B71DADB98C59508306275AE37A1916EF2517E77CFE09FA006909FE880")
    WhoAreYouMessage.create(magic = bytes.slice(0,32), content = bytes.slice(32))
  }

  @Test
  fun decodeSelfFromSessionRead() {
    val keyPair = SECP256K1.KeyPair.random()
    val selfEnr = EthereumNodeRecord.create(keyPair, ip = InetAddress.getLoopbackAddress(), udp = 12344)
    val nodeId = Hash.keccak256(selfEnr.publicKey().bytes())
    val session = Session(keyPair, selfEnr, InetSocketAddress(InetAddress.getLoopbackAddress(), 12345),
      nodeId, { _, _ -> }, { selfEnr }, RoutingTable(selfEnr), TopicTable(), { _ -> false }, Dispatchers.Default)
    val bytes = Bytes.fromHexString("0x282E641D415A892C05FD03F0AE716BDD92D1569116FDC7C7D3DB39AC5F79B0F7EF8CE56EDC7BB967899B4C48EEA6A0E838C9091B71DADB98C59508306275AE37A1916EF2517E77CFE09FA006909FE880")
    val message = session.decode(bytes) as WhoAreYouMessage
    val otherMessage = WhoAreYouMessage.create(magic = bytes.slice(0,32), content = bytes.slice(32))
    assertEquals(message.enrSeq, otherMessage.enrSeq)
    assertEquals(message.idNonce, otherMessage.idNonce)
  }
}
