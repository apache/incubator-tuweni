package org.apache.tuweni.devp2p.v5.internal

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.v5.PacketCodec
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import org.apache.tuweni.rlp.RLP
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

class DefaultPacketCodec(
  val nodeId: Bytes
): PacketCodec {

  override fun decode(buffer: ByteBuffer): UdpMessage {
    val bytes = Bytes.wrapByteBuffer(buffer)
    val tag = bytes.slice(0, UdpMessage.TAG_LENGTH)
    val content = bytes.slice(UdpMessage.TAG_LENGTH)
    if (RLP.isList(content)) {
      if (UdpMessage.isMagic(tag, nodeId)) {
        return WhoAreYouMessage.create(content, nodeId, nodeId)
      }
    } else {
      val srcId = UdpMessage.getSourceFromTag(tag, nodeId)
      return RandomMessage.create(content, srcId, nodeId)
    }
    throw IllegalArgumentException("Unknown message inbound...")
  }

}
