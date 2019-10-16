package org.apache.tuweni.devp2p.v5.packet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.crypto.sodium.SHA256Hash
import java.nio.ByteBuffer

abstract class UdpMessage(
  val src: Bytes,
  val dest: Bytes,
  val auth: Bytes? = null,
  val encryptionKey: Bytes? = null
) {

  abstract fun encode(encryptionKey: Bytes = Bytes.EMPTY, encryptionNonce: Bytes = Bytes.EMPTY): ByteBuffer

  open fun getMessageType(): Int {
    throw UnsupportedOperationException("Message don't identified with type")
  }

  companion object {

    const val MAX_UDP_MESSAGE_SIZE = 1280
    const val AUTH_TAG_LENGTH: Int = 12
    const val RANDOM_DATA_LENGTH: Int = 44
    const val ID_NONCE_LENGTH: Int = 32
    const val TAG_LENGTH: Int = 32

    private val WHO_ARE_YOU: Bytes = Bytes.wrap("WHOAREYOU".toByteArray())


    fun isMagic(value: Bytes, nodeId: Bytes): Boolean {
      val calculatedMagic = magic(nodeId)
      return value == calculatedMagic
    }

    fun magic(dest: Bytes): Bytes {
      val concatView = Bytes.wrap(dest, WHO_ARE_YOU)
      val input = SHA256Hash.Input.fromBytes(concatView)
      return Bytes.wrap(SHA256Hash.hash(input).bytesArray())
    }

    fun tag(src: Bytes, dest: Bytes): Bytes {
      val input = SHA256Hash.Input.fromBytes(dest)
      val encodedDestKey = SHA256Hash.hash(input).bytesArray()
      return Bytes.wrap(encodedDestKey).xor(src)
    }

    fun getSourceFromTag(tag: Bytes, dest: Bytes): Bytes {
      val input = SHA256Hash.Input.fromBytes(dest)
      val encodedDestKey = SHA256Hash.hash(input).bytesArray()
      return Bytes.wrap(encodedDestKey).xor(tag)
    }

    fun authTag(): Bytes = Bytes.random(AUTH_TAG_LENGTH)

    fun randomData(): Bytes = Bytes.random(RANDOM_DATA_LENGTH)

    fun idNonce(): Bytes = Bytes.random(ID_NONCE_LENGTH)

  }

}
