// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.v5.packet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.TicketMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TicketMessageTest {

  @Test
  fun encodeCreatesValidBytesSequence() {
    val requestId = Bytes.fromHexString("0xC6E32C5E89CAA754")
    val message = TicketMessage(requestId, Bytes.random(32), 1000)

    val encodingResult = message.toRLP()

    val decodingResult = TicketMessage.create(encodingResult)

    assertEquals(decodingResult.requestId, requestId)
    assertEquals(decodingResult.ticket, message.ticket)
    assertEquals(decodingResult.waitTime, message.waitTime)
  }
}
