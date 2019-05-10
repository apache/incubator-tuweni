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

import com.google.common.base.Splitter
import org.apache.tuweni.bytes.Bytes
import java.nio.charset.StandardCharsets

/**
 * Hobbits message.
 *
 */
class Message(
  val protocol: String = "EWP",
  val version: String = "0.2",
  val command: String,
  val headers: Bytes,
  val body: Bytes
) {

  companion object {

    /**
     * Reads a message from a byte buffer.
     * @param message the message bytes
     * @return the message interpreted by the codec, or null if the message is too short.
     */
    @JvmStatic
    fun readMessage(message: Bytes): Message? {
      var requestLineBytes: Bytes? = null
      for (i in 0 until message.size()) {
        if (message.get(i) == '\n'.toByte()) {
          requestLineBytes = message.slice(0, i)
          break
        }
      }
      if (requestLineBytes == null) {
        return null
      }
      val requestLine = String(requestLineBytes.toArrayUnsafe(), StandardCharsets.UTF_8)
      val segments = Splitter.on(" ").split(requestLine).iterator()

      val protocol = segments.next()
      if (!segments.hasNext()) {
        return null
      }
      val version = segments.next()
      if (!segments.hasNext()) {
        return null
      }
      val command = segments.next()
      if (!segments.hasNext()) {
        return null
      }
      val headersLength = segments.next().toInt()
      if (!segments.hasNext()) {
        return null
      }
      val bodyLength = segments.next().toInt()

      if (message.size() < requestLineBytes.size() + 1 + headersLength + bodyLength) {
        return null
      }

      val headers = message.slice(requestLineBytes.size() + 1, headersLength)
      val body = message.slice(requestLineBytes.size() + headersLength + 1, bodyLength)

      return Message(protocol, version, command, headers, body)
    }
  }

  /**
   * Writes a message into bytes.
   */
  fun toBytes(): Bytes {
    val requestLine = "$protocol $version $command ${headers.size()} ${body.size()}\n"
    return Bytes.concatenate(Bytes.wrap(requestLine.toByteArray()), headers, body)
  }
}
