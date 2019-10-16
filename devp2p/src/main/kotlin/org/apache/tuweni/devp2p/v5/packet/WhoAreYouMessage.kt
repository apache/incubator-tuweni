package org.apache.tuweni.devp2p.v5.packet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.v5.MessageDecoder
import org.apache.tuweni.rlp.RLP
import java.nio.ByteBuffer

class WhoAreYouMessage(
  src: Bytes,
  dest: Bytes,
  val authTag: Bytes = authTag(),
  val idNonce: Bytes = idNonce()
): UdpMessage(src, dest) {

  override fun encode(encryptionKey: Bytes, encryptionNonce: Bytes): ByteBuffer {
    val magic = magic(dest)
    val rlpBody = RLP.encodeList { w ->
      w.writeByteArray(authTag.toArray())
      w.writeByteArray(idNonce.toArray())
    }
    val messageBytes = Bytes.wrap(magic, rlpBody) // TODO: ENR seq number
    return ByteBuffer.wrap(messageBytes.toArray())
  }

  companion object: MessageDecoder<WhoAreYouMessage> {
    override fun create(content: Bytes, src: Bytes, nodeId: Bytes): WhoAreYouMessage {
      return RLP.decodeList(content) { r ->
        val authTag = Bytes.wrap(r.readByteArray())
        val idNonce = Bytes.wrap(r.readByteArray())
        return@decodeList WhoAreYouMessage(src, nodeId, authTag, idNonce)
      }
    }
  }

}
