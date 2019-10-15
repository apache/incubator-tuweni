package org.apache.tuweni.devp2p.v5.packet

import org.apache.tuweni.bytes.Bytes
import org.junit.jupiter.api.Test

class RandomMessageTest {

  @Test
  fun encodeAndDecodeCorrectlyTransformsData() {
    val srcId = Bytes.fromHexString("0x98EB6D611291FA21F6169BFF382B9369C33D997FE4DC93410987E27796360640")
    val destId = Bytes.fromHexString("0xA5CFE10E0EFC543CBE023560B2900E2243D798FAFD0EA46267DDD20D283CE13C")
    val authTag = UdpMessage.authTag()

    val message = RandomMessage(srcId, destId, authTag)

    val encodedResult = message.encode()

    val content = Bytes.wrapByteBuffer(encodedResult).slice(UdpMessage.TAG_LENGTH)
    val decodedMessage = RandomMessage.create(content, srcId, destId)

    assert(decodedMessage.authTag == authTag)
  }

}
