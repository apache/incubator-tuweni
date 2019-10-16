package org.apache.tuweni.devp2p.v5.packet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.encrypt.AES128GCM
import org.apache.tuweni.rlp.RLP
import java.nio.ByteBuffer

class FindNodeMessage(
  src: Bytes,
  dest: Bytes,
  auth: Bytes,
  val requestId: Bytes = Bytes.random(8),
  val distance: Long = 0
): UdpMessage(src, dest, auth) {

  override fun encode(encryptionKey: Bytes, encryptionNonce: Bytes): ByteBuffer {
    val tag = tag(src, dest)
    val encoded = RLP.encodeList { writer ->
      writer.writeValue(tag(src, dest))
      writer.writeValue(auth!!)
      val payload = Bytes.wrap(Bytes.ofUnsignedShort(getMessageType()), requestId, Bytes.ofUnsignedLong(distance))
      val encryptionMeta = if (RLP.isList(auth)) Bytes.wrap(tag, auth) else tag
      val encrypted = AES128GCM.encrypt(encryptionKey, encryptionNonce, payload, encryptionMeta)
      writer.writeValue(encrypted)
    }
    return ByteBuffer.wrap(encoded.toArray())
  }

  override fun getMessageType(): Int = 3

}
