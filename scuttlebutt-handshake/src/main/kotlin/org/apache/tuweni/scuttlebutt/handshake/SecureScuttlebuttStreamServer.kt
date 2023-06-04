// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
