// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.v5.packet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.FindNodeMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FindNodeMessageTest {

  @Test
  fun encodeCreatesValidBytesSequence() {
    val expectedEncodingResult = "0xca88c6e32c5e89caa75480"

    val requestId = Bytes.fromHexString("0xC6E32C5E89CAA754")
    val message = FindNodeMessage(requestId)

    val encodingResult = message.toRLP()
    assertEquals(encodingResult.toHexString(), expectedEncodingResult)

    val decodingResult = FindNodeMessage.create(encodingResult)

    assertEquals(decodingResult.requestId, requestId)
    assertEquals(decodingResult.distance, 0)
  }
}
