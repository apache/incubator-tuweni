// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.v5.packet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.NodesMessage
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress

@ExtendWith(BouncyCastleExtension::class)
class NodesMessageTest {

  @Test
  fun encodeCreatesValidBytesSequence() {
    val requestId = Bytes.fromHexString("0xC6E32C5E89CAA754")
    val total = 10
    val nodeRecords = listOf(
      EthereumNodeRecord.create(SECP256K1.KeyPair.random(), ip = InetAddress.getLoopbackAddress(), udp = 9090),
      EthereumNodeRecord.create(SECP256K1.KeyPair.random(), ip = InetAddress.getLoopbackAddress(), udp = 9091),
      EthereumNodeRecord.create(SECP256K1.KeyPair.random(), ip = InetAddress.getLoopbackAddress(), udp = 9092),
    )
    val message = NodesMessage(requestId, total, nodeRecords)

    val encodingResult = message.toRLP()

    val decodingResult = NodesMessage.create(encodingResult)

    assertEquals(decodingResult.requestId, requestId)
    assertEquals(decodingResult.total, 10)
    assertEquals(decodingResult.nodeRecords[0].udp(), 9090)
    assertEquals(decodingResult.nodeRecords[1].udp(), 9091)
    assertEquals(decodingResult.nodeRecords[2].udp(), 9092)
  }
}
