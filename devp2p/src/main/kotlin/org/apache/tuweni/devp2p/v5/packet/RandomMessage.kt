package org.apache.tuweni.devp2p.v5.packet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.v5.MessageDecoder
import org.apache.tuweni.rlp.RLP
import java.nio.ByteBuffer

class RandomMessage(
  src: Bytes,
  dest: Bytes,
  val authTag: Bytes = authTag()
): UdpMessage(src, dest) {

  override fun encode(): ByteBuffer {
    val authTagBytes = RLP.encodeByteArray(authTag.toArray())

    val tag = tag(src, dest)
    val messageBytes = Bytes.wrap(tag, authTagBytes, randomData())
    return ByteBuffer.wrap(messageBytes.toArray())
  }

  companion object: MessageDecoder<RandomMessage> {
    override fun create(content: Bytes, src: Bytes, nodeId: Bytes): RandomMessage {
      return RLP.decode(content) { r ->
        val authTag = Bytes.wrap(r.readByteArray()).slice(0, AUTH_TAG_LENGTH)
        return@decode RandomMessage(src, nodeId, authTag)
      }
    }
  }

}
