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
package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.Hash

/**
 * Discovery message sent over UDP.
 */
internal interface Message {

  companion object {

    const val MAX_UDP_MESSAGE_SIZE = 1280
    const val TAG_LENGTH: Int = 32
    const val AUTH_TAG_LENGTH: Int = 12
    const val RANDOM_DATA_LENGTH: Int = 44
    const val ID_NONCE_LENGTH: Int = 32
    const val REQUEST_ID_LENGTH: Int = 8

    private val WHO_ARE_YOU: Bytes = Bytes.wrap("WHOAREYOU".toByteArray())

    fun magic(dest: Bytes): Bytes {
      return Hash.sha2_256(Bytes.wrap(dest, WHO_ARE_YOU))
    }

    fun tag(src: Bytes32, dest: Bytes): Bytes32 {
      val encodedDestKey = Hash.sha2_256(dest)
      return encodedDestKey.xor(src)
    }

    fun getSourceFromTag(tag: Bytes, dest: Bytes): Bytes {
      val encodedDestKey = Hash.sha2_256(dest)
      return Bytes.wrap(encodedDestKey).xor(tag)
    }

    fun requestId(): Bytes = Bytes.random(REQUEST_ID_LENGTH)

    fun authTag(): Bytes = Bytes.random(AUTH_TAG_LENGTH)

    fun idNonce(): Bytes = Bytes.random(ID_NONCE_LENGTH)
  }

  fun toRLP(): Bytes

  fun type(): MessageType
}

internal enum class MessageType(val code: Int) {
  RANDOM(0),
  WHOAREYOU(0),
  FINDNODE(3),
  NODES(4),
  PING(1),
  PONG(2),
  REGTOPIC(5),
  REGCONFIRM(7),
  TICKET(6),
  TOPICQUERY(8);

  fun byte(): Byte = code.toByte()

  companion object {
    fun valueOf(code: Int): MessageType {
      for (messageType in MessageType.values()) {
        if (messageType.code == code) {
          return messageType
        }
      }
      throw IllegalArgumentException("No known message with code $code")
    }
  }
}
