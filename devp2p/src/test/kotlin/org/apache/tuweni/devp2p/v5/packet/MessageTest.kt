// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.v5.packet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.devp2p.v5.Message
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class MessageTest {

  @Test
  fun magicCreatesSha256OfDestNodeIdAndConstantString() {
    val destId = Bytes.fromHexString("0xA5CFE10E0EFC543CBE023560B2900E2243D798FAFD0EA46267DDD20D283CE13C")
    val expected = Bytes.fromHexString("0x98EB6D611291FA21F6169BFF382B9369C33D997FE4DC93410987E27796360640")

    val result = Message.magic(destId)

    assertEquals(expected, result)
  }

  @Test
  fun tagHashesSourceAndDestNodeIdCorrectly() {
    val srcId = Bytes32.fromHexString("0x98EB6D611291FA21F6169BFF382B9369C33D997FE4DC93410987E27796360640")
    val destId = Bytes32.fromHexString("0xA5CFE10E0EFC543CBE023560B2900E2243D798FAFD0EA46267DDD20D283CE13C")
    val expected = Bytes.fromHexString("0xB7A0D7CA8BD37611315DA0882FF479DE14B442FD30AE0EFBE6FC6344D55DC632")

    val result = Message.tag(srcId, destId)

    assertEquals(expected, result)
  }

  @Test
  fun getSourceFromTagFetchesSrcNodeId() {
    val srcId = Bytes32.fromHexString("0x98EB6D611291FA21F6169BFF382B9369C33D997FE4DC93410987E27796360640")
    val destId = Bytes.fromHexString("0xA5CFE10E0EFC543CBE023560B2900E2243D798FAFD0EA46267DDD20D283CE13C")
    val tag = Message.tag(srcId, destId)

    val result = Message.getSourceFromTag(tag, destId)

    assertEquals(srcId, result)
  }

  @Test
  fun authTagGivesRandom12Bytes() {
    val firstResult = Message.authTag()

    assertEquals(Message.AUTH_TAG_LENGTH, firstResult.size())

    val secondResult = Message.authTag()

    assertNotEquals(secondResult, firstResult)
  }

  @Test
  fun idNonceGivesRandom32Bytes() {
    val firstResult = Message.idNonce()

    assertEquals(Message.ID_NONCE_LENGTH, firstResult.size())

    val secondResult = Message.idNonce()

    assertNotEquals(secondResult, firstResult)
  }
}
