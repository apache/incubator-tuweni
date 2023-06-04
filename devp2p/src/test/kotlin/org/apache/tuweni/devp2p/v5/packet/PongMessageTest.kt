// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.v5.packet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.PongMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PongMessageTest {

  @Test
  fun encodeCreatesValidBytesSequence() {
    val requestId = Bytes.fromHexString("0xC6E32C5E89CAA754")
    val message = PongMessage(requestId, 0, "127.0.0.1", 9090)

    val encodingResult = message.toRLP()

    val decodingResult = PongMessage.create(encodingResult)

    assertEquals(decodingResult.requestId, requestId)
    assertEquals(decodingResult.enrSeq, message.enrSeq)
    assertEquals(decodingResult.recipientIp, message.recipientIp)
    assertEquals(decodingResult.recipientPort, message.recipientPort)
  }
}
