// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
