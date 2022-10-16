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
package org.apache.tuweni.hobbits

import org.apache.tuweni.bytes.Bytes
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8

internal val PREAMBLE = "EWP".toByteArray(UTF_8)
internal val MESSAGE_HEADER_LENGTH = PREAMBLE.size + java.lang.Integer.BYTES * 3 + 1

/**
 * Hobbits message.
 *
 * @param version Hobbits version
 * @param protocol message protocol
 * @param headers headers to send
 * @param body the body of the message
 */
class Message(
  val version: Int = 3,
  val protocol: Protocol,
  val headers: Bytes,
  val body: Bytes
) {

  companion object {

    /**
     * Reads a message from a byte buffer.
     * @param message the message bytes
     * @return the message interpreted by the codec, or null if the message is too short.
     * @throws IllegalArgumentException if the message doesn't start with the correct preamble.
     */
    @JvmStatic
    fun readMessage(message: Bytes): Message? {
      if (message.size() < MESSAGE_HEADER_LENGTH) {
        return null
      }
      if (message.slice(0, PREAMBLE.size) != Bytes.wrap(PREAMBLE)) {
        throw IllegalArgumentException("Message doesn't start with correct preamble")
      }
      val version = message.getInt(PREAMBLE.size)
      val protocol = Protocol.fromByte(message.get(PREAMBLE.size + 4))
      val headersLength = message.getInt(PREAMBLE.size + 4 + 1)
      val bodyLength = message.getInt(PREAMBLE.size + 4 + 1 + 4)

      if (message.size() < MESSAGE_HEADER_LENGTH + headersLength + bodyLength) {
        return null
      }

      val headers = message.slice(MESSAGE_HEADER_LENGTH, headersLength)
      val body = message.slice(MESSAGE_HEADER_LENGTH + headersLength, bodyLength)

      return Message(version, protocol, headers, body)
    }
  }

  /**
   * Writes a message into bytes.
   * @return the bytes of the message
   */
  fun toBytes(): Bytes {
    val buffer = ByteBuffer.allocate(MESSAGE_HEADER_LENGTH)
    buffer.put(PREAMBLE)
    buffer.putInt(version)
    buffer.put(protocol.code)
    buffer.putInt(headers.size())
    buffer.putInt(body.size())

    return Bytes.concatenate(Bytes.wrap(buffer.array()), headers, body)
  }

  /**
   * Provides the size of the message
   * @return the size of the message
   */
  fun size(): Int {
    return MESSAGE_HEADER_LENGTH + headers.size() + body.size()
  }

  /**
   * Transforms the message into a string, with a request line following by headers and body in hex representation.
   */
  override fun toString(): String {
    val requestLine = "EWP $version $protocol ${headers.size()} ${body.size()}\n"
    return requestLine + headers.toHexString() + "\n" + body.toHexString()
  }
}

/**
 * Subprotocols supported by the hobbits protocol.
 * @param code the byte identifying the subprotocol
 */
enum class Protocol(val code: Byte) {
  /**
   * Gossip protocol message
   */
  GOSSIP(1),

  /**
   * Ping message
   */
  PING(2),

  /**
   * RPC message
   */
  RPC(0);

  companion object {
    /**
     * Finds a protocol from a byte, or throws an error if no protocol exists for that code.
     * @param b the byte to interpret
     * @return a Protocol
     */
    fun fromByte(b: Byte): Protocol {
      return when (b) {
        RPC.code -> RPC
        GOSSIP.code -> GOSSIP
        PING.code -> PING
        else -> throw IllegalArgumentException("Unsupported protocol code $b")
      }
    }
  }
}
