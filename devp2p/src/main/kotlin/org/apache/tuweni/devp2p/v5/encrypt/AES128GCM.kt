/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.devp2p.v5.encrypt

import org.apache.tuweni.bytes.Bytes
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Util dedicated for AES-GCM encoding with key size equal 16 bytes
 */
object AES128GCM {

  private const val ALGO_NAME: String = "AES"
  private const val CIPHER_NAME: String = "AES/GCM/NoPadding"
  private const val KEY_SIZE: Int = 128


  /**
   * AES128GCM encryption function
   *
   * @param key 16-byte encryption key
   * @param nonce initialization vector
   * @param message content for encryption
   * @param data encryption metadata
   */
  fun encrypt(key: Bytes, nonce: Bytes, message: Bytes, data: Bytes): Bytes {
    val nonceBytes = nonce.toArray()
    val keySpec = SecretKeySpec(key.toArray(), ALGO_NAME)
    val cipher = Cipher.getInstance(CIPHER_NAME)
    val parameterSpec = GCMParameterSpec(KEY_SIZE, nonceBytes)

    cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec)

    cipher.updateAAD(data.toArray())

    val encryptedText = Bytes.wrap(cipher.doFinal(message.toArray()))

    val wrappedNonce = Bytes.wrap(nonceBytes)
    val nonceSize = Bytes.ofUnsignedInt(nonceBytes.size.toLong())
    return Bytes.wrap(nonceSize, wrappedNonce, encryptedText)
  }

  /**
   * AES128GCM decryption function
   *
   * @param encryptedContent content for decryption
   * @param key 16-byte encryption key
   * @param data encryption metadata
   */
  fun decrypt(encryptedContent: Bytes, key: Bytes, data: Bytes): Bytes {
    val buffer = ByteBuffer.wrap(encryptedContent.toArray())
    val nonceLength = buffer.int
    val nonce = ByteArray(nonceLength)
    buffer.get(nonce)
    val encryptedText = ByteArray(buffer.remaining())
    buffer.get(encryptedText)

    val keySpec = SecretKeySpec(key.toArray(), ALGO_NAME)

    val parameterSpec = GCMParameterSpec(KEY_SIZE, nonce)
    val cipher = Cipher.getInstance(CIPHER_NAME)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec)

    cipher.updateAAD(data.toArray())

    return Bytes.wrap(cipher.doFinal(encryptedText))
  }
}
