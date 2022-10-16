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
  fun encrypt(privateKey: Bytes, nonce: Bytes, message: Bytes, additionalAuthenticatedData: Bytes): Bytes {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(
      Cipher.ENCRYPT_MODE,
      SecretKeySpec(privateKey.toArrayUnsafe(), "AES"),
      GCMParameterSpec(128, nonce.toArrayUnsafe())
    )
    cipher.updateAAD(additionalAuthenticatedData.toArrayUnsafe())
    val result = Bytes.wrap(cipher.doFinal(message.toArrayUnsafe()))
    return result
  }

  /**
   * AES128GCM decryption function
   *
   * @param privateKey the key to use for decryption
   * @param nonce the nonce of the encrypted data
   * @param encoded the encrypted content
   * @param additionalAuthenticatedData the AAD that should be decrypted alongside
   * @return the decrypted data
   */
  fun decrypt(privateKey: Bytes, nonce: Bytes, encoded: Bytes, additionalAuthenticatedData: Bytes): Bytes {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(
      Cipher.DECRYPT_MODE,
      SecretKeySpec(privateKey.toArrayUnsafe(), "AES"),
      GCMParameterSpec(128, nonce.toArrayUnsafe())
    )
    cipher.updateAAD(additionalAuthenticatedData.toArrayUnsafe())
    return Bytes.wrap(cipher.doFinal(encoded.toArrayUnsafe()))
  }
}
