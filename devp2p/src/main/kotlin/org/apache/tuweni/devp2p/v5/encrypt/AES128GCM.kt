package org.apache.tuweni.devp2p.v5.encrypt

import org.apache.tuweni.bytes.Bytes
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AES128GCM {

  fun encrypt(key: Bytes, nonce: Bytes, message: Bytes, data: Bytes): Bytes {
    val result = encrypt(nonce.toArray(), key.toArray(), data.toArray(), message.toArray())
    return Bytes.wrap(result)
  }

  fun encrypt(key: ByteArray, nonce: ByteArray, message: ByteArray, data: ByteArray): ByteArray {
    val keySpec = SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val parameterSpec = GCMParameterSpec(128, nonce)

    cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec)

    cipher.updateAAD(data)

    val encriptedText = Bytes.wrap(cipher.doFinal(message))

    val wrappedNonce = Bytes.wrap(nonce)
    val nonceSize = Bytes.ofUnsignedInt(nonce.size.toLong())
    return Bytes.wrap(nonceSize, wrappedNonce, encriptedText).toArray()
  }

  fun decrypt(encryptedContent: Bytes, key: Bytes, data: Bytes): Bytes {
    val result = decrypt(encryptedContent.toArray(), key.toArray(), data.toArray())
    return Bytes.wrap(result)
  }

  fun decrypt(encryptedContent: ByteArray, key: ByteArray, data: ByteArray): ByteArray {
    val buffer = ByteBuffer.wrap(encryptedContent)
    val nonceLength = buffer.int
    val nonce = ByteArray(nonceLength)
    buffer.get(nonce)
    val encriptedText = ByteArray(buffer.remaining())
    buffer.get(encriptedText)

    val keySpec = SecretKeySpec(key, "AES")

    val parameterSpec = GCMParameterSpec(128, nonce)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec)

    cipher.updateAAD(data)

    return cipher.doFinal(encriptedText)
  }

}
