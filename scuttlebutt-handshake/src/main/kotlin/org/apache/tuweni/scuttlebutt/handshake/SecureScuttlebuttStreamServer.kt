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

/**
 * Interface used to encrypt and decrypt messages to and from a client.
 */
interface SecureScuttlebuttStreamServer {
  /**
   * Prepares a message to be sent to the client
   *
   * @param message the message to encrypt and format
   * @return the message, encrypted and ready to send
   */
  fun sendToClient(message: Bytes): Bytes

  /**
   * Prepares a goodbye message to be sent to the client
   *
   * @return the goodbye message
   */
  fun sendGoodbyeToClient(): Bytes

  /**
   * Adds message bytes to the reader stream, returning the bytes that could be decrypted.
   *
   * @param message the message to decrypt
   * @return the message, decrypted and ready for consumption, or null if the bytes provided were an incomplete message.
   * @throws StreamException if the message cannot be decrypted
   */
  fun readFromClient(message: Bytes): Bytes

  companion object {
    /**
     * Checks if a message is a goodbye message, indicating the end of the connection
     *
     * @param message the message to interpret
     *
     * @return true if the message is a goodbye message, or false otherwise
     */
    fun isGoodbye(message: Bytes): Boolean {
      return message.size() == 18 && message.numberOfLeadingZeroBytes() == 18
    }
  }
}
