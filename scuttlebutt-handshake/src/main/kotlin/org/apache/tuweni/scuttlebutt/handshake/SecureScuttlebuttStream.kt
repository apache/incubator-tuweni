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
package org.apache.tuweni.scuttlebutt.handshake

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.MutableBytes
import org.apache.tuweni.crypto.sodium.SHA256Hash
import org.apache.tuweni.crypto.sodium.SecretBox
import java.util.Arrays
import java.util.stream.Collectors

internal class SecureScuttlebuttStream(
  clientToServerKey: SHA256Hash.Hash?,
  clientToServerNonce: Bytes,
  serverToClientKey: SHA256Hash.Hash?,
  serverToClientNonce: Bytes
) : SecureScuttlebuttStreamClient, SecureScuttlebuttStreamServer {
  private val clientToServerKey: SecretBox.Key
  private val clientToServerNonce: MutableBytes
  private val serverToClientKey: SecretBox.Key
  private val serverToClientNonce: MutableBytes

  @Synchronized
  override fun sendToServer(message: Bytes): Bytes {
    return encrypt(message, clientToServerKey, clientToServerNonce)
  }

  @Synchronized
  override fun sendGoodbyeToServer(): Bytes {
    return sendToServer(Bytes.wrap(ByteArray(18)))
  }

  @Synchronized
  override fun readFromServer(message: Bytes): Bytes {
    return decrypt(message, serverToClientKey, serverToClientNonce, false)
  }

  @Synchronized
  override fun sendToClient(message: Bytes): Bytes {
    return encrypt(message, serverToClientKey, serverToClientNonce)
  }

  @Synchronized
  override fun sendGoodbyeToClient(): Bytes {
    return sendToClient(Bytes.wrap(ByteArray(18)))
  }

  @Synchronized
  override fun readFromClient(message: Bytes): Bytes {
    return decrypt(message, clientToServerKey, clientToServerNonce, true)
  }

  private var clientToServerBuffer = Bytes.EMPTY
  private var serverToClientBuffer = Bytes.EMPTY

  init {
    this.clientToServerKey = SecretBox.Key.fromHash(clientToServerKey!!)
    this.serverToClientKey = SecretBox.Key.fromHash(serverToClientKey!!)
    this.clientToServerNonce = clientToServerNonce.mutableCopy()
    this.serverToClientNonce = serverToClientNonce.mutableCopy()
  }

  private fun decrypt(message: Bytes, key: SecretBox.Key, nonce: MutableBytes, isClientToServer: Boolean): Bytes {
    var index = 0
    val decryptedMessages = mutableListOf<Bytes>()
    val messageWithBuffer: Bytes = if (isClientToServer) {
      Bytes.concatenate(clientToServerBuffer, message)
    } else {
      Bytes.concatenate(serverToClientBuffer, message)
    }
    while (index < messageWithBuffer.size()) {
      val decryptedMessage = decryptMessage(messageWithBuffer.slice(index), key, nonce) ?: break
      decryptedMessages.add(decryptedMessage)
      index += decryptedMessage.size() + 34
    }
    if (isClientToServer) {
      clientToServerBuffer = messageWithBuffer.slice(index)
    } else {
      serverToClientBuffer = messageWithBuffer.slice(index)
    }
    return Bytes.concatenate(*decryptedMessages.toTypedArray())
  }

  private fun decryptMessage(message: Bytes, key: SecretBox.Key, nonce: MutableBytes): Bytes? {
    if (message.size() < 34) {
      return null
    }
    var headerNonce: SecretBox.Nonce? = null
    var bodyNonce: SecretBox.Nonce? = null
    return try {
      val snapshotNonce = nonce.mutableCopy()
      headerNonce = SecretBox.Nonce.fromBytes(snapshotNonce)
      bodyNonce = SecretBox.Nonce.fromBytes(snapshotNonce.increment())
      val decryptedHeader = SecretBox.decrypt(message.slice(0, 34), key, headerNonce)
        ?: throw StreamException("Failed to decrypt message header")
      val bodySize = (decryptedHeader[0].toInt() and 0xFF shl 8) + (decryptedHeader[1].toInt() and 0xFF)
      if (message.size() < bodySize + 34) {
        return null
      }
      val body = message.slice(34, bodySize)
      val decryptedBody =
        SecretBox.decrypt(Bytes.concatenate(decryptedHeader.slice(2), body), key, bodyNonce)
          ?: throw StreamException("Failed to decrypt message")
      nonce.increment().increment()
      decryptedBody
    } finally {
      destroyIfNonNull(headerNonce)
      destroyIfNonNull(bodyNonce)
    }
  }

  private fun encrypt(message: Bytes, clientToServerKey: SecretBox.Key, clientToServerNonce: MutableBytes): Bytes {
    val bytes = breakIntoParts(message)
    val segments = bytes
      .stream()
      .map { slice: Bytes ->
        encryptMessage(
          slice,
          clientToServerKey,
          clientToServerNonce
        )
      }
      .collect(Collectors.toList())
    return Bytes.concatenate(segments)
  }

  private fun breakIntoParts(message: Bytes): ArrayList<Bytes> {
    val original = message.toArray()
    val chunk = 4096
    val result = ArrayList<Bytes>()
    var i = 0
    while (i < original.size) {
      val bytes = Arrays.copyOfRange(original, i, Math.min(original.size, i + chunk))
      val wrap = Bytes.wrap(bytes)
      result.add(wrap)
      i += chunk
    }
    return result
  }

  private fun encryptMessage(message: Bytes, key: SecretBox.Key, nonce: MutableBytes): Bytes {
    var headerNonce: SecretBox.Nonce? = null
    var bodyNonce: SecretBox.Nonce? = null
    return try {
      headerNonce = SecretBox.Nonce.fromBytes(nonce)
      bodyNonce = SecretBox.Nonce.fromBytes(nonce.increment())
      nonce.increment()
      val encryptedBody = SecretBox.encrypt(message, key, bodyNonce)
      val bodySize = encryptedBody.size() - 16
      val encodedBodySize = Bytes.ofUnsignedInt(bodySize.toLong()).slice(2)
      val header = SecretBox.encrypt(Bytes.concatenate(encodedBodySize, encryptedBody.slice(0, 16)), key, headerNonce)
      Bytes.concatenate(header, encryptedBody.slice(16))
    } finally {
      destroyIfNonNull(headerNonce)
      destroyIfNonNull(bodyNonce)
    }
  }

  private fun destroyIfNonNull(nonce: SecretBox.Nonce?) {
    nonce?.destroy()
  }
}
