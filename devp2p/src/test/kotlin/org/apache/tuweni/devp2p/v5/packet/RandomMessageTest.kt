// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.v5.packet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.Message
import org.apache.tuweni.devp2p.v5.RandomMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class RandomMessageTest {

  @Test
  fun encodeCreatesValidBytesSequence() {
    val expectedEncodingResult =
      "0xb53ccf732982b8e950836d1e02898c8b38cfdbfdf86bc65c8826506b454e14618ea73612a0f5582c130ff666"

    val data = Bytes.fromHexString(expectedEncodingResult)
    val message = RandomMessage(Message.authTag(), data)

    val encodingResult = message.toRLP()
    assertEquals(encodingResult.toHexString(), expectedEncodingResult)

    val decodingResult = RandomMessage.create(Message.authTag(), encodingResult)

    assertEquals(decodingResult.data, data)
  }

  @Test
  fun randomDataGivesRandom44Bytes() {
    val firstResult = RandomMessage.randomData()

    assertEquals(Message.RANDOM_DATA_LENGTH, firstResult.size())

    val secondResult = RandomMessage.randomData()

    assertNotEquals(secondResult, firstResult)
  }
}
