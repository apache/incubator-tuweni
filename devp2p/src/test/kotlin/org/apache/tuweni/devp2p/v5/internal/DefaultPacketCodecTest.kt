package org.apache.tuweni.devp2p.v5.internal

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.PacketCodec
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.junit.jupiter.api.Test

class DefaultPacketCodecTest {

  private val nodeId: Bytes = Bytes.fromHexString("0x98EB6D611291FA21F6169BFF382B9369C33D997FE4DC93410987E27796360640")
  private val codec: PacketCodec = DefaultPacketCodec(nodeId)


  @Test
  fun decodeTransformsMessageIntoValidMessage() {
    val srcId = Bytes.fromHexString("0xA5CFE10E0EFC543CBE023560B2900E2243D798FAFD0EA46267DDD20D283CE13C")
    val authTag = UdpMessage.authTag()
    val message = RandomMessage(srcId, nodeId, authTag)

    val result = message.encode()

    val decodedResult = codec.decode(result) as RandomMessage

    assert(decodedResult.src == srcId)
    assert(decodedResult.dest == nodeId)
    assert(decodedResult.authTag == authTag)
  }

}
