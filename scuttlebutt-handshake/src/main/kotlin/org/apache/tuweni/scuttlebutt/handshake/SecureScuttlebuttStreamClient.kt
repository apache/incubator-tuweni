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
 * Interface used to encrypt and decrypt messages to and from a server.
 */
interface SecureScuttlebuttStreamClient {
  /**
   * Prepares a message to be sent to the server
   *
   * @param message the message to encrypt and format
   * @return the message, encrypted and ready to send
   */
  fun sendToServer(message: Bytes): Bytes

  /**
   * Prepares a goodbye message to be sent to the server
   *
   * @return the goodbye message
   */
  fun sendGoodbyeToServer(): Bytes

  /**
   * Adds message bytes to the reader stream, returning the bytes that could be decrypted.
   *
   * @param message the message to decrypt
   * @return the message, decrypted and ready for consumption, or null if the message provided were an incomplete
   * message.
   * @throws StreamException if the message cannot be decrypted
   */
  fun readFromServer(message: Bytes): Bytes
}
