package org.apache.tuweni.devp2p.v5.packet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.crypto.sodium.SHA256Hash
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.rlp.RLP
import java.nio.ByteBuffer

class AuthMessage(
  src: Bytes,
  dest: Bytes,
  private val idNonce: Bytes,
  private val secretKey: SECP256K1.SecretKey
): UdpMessage(src, dest) {

  override fun encode(): ByteBuffer {
    val signValueBytes = Bytes.wrap(DISCOVERY_ID_NONCE, idNonce)
    val signValueInput = SHA256Hash.Input.fromBytes(signValueBytes)
    val signValue = SHA256Hash.hash(signValueInput).bytesArray()
    val secretKey = Signature.SecretKey.fromBytes(secretKey.bytesArray())
    val signature = Signature.sign(signValue, secretKey)
    val rlpContent = RLP.encodeList { writer ->
      writer.writeInt(VERSION)
      writer.writeByteArray(signature)
      // TODO: Consider ENR sequence
    }

    return ByteBuffer.wrap(rlpContent.toArray())
  }

  companion object {
      private val DISCOVERY_ID_NONCE: Bytes = Bytes.wrap("discovery-id-nonce".toByteArray())
  }

}
