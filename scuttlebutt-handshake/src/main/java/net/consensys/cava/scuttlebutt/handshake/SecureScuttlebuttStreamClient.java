/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.scuttlebutt.handshake;

import net.consensys.cava.bytes.Bytes;

/**
 * Interface used to encrypt and decrypt messages to and from a server.
 */
public interface SecureScuttlebuttStreamClient {

  /**
   * Prepares a message to be sent to the server
   * 
   * @param message the message to encrypt and format
   * @return the message, encrypted and ready to send
   */
  Bytes sendToServer(Bytes message);

  /**
   * Prepares a goodbye message to be sent to the server
   * 
   * @return the goodbye message
   */
  Bytes sendGoodbyeToServer();

  /**
   * Adds message bytes to the reader stream, returning the bytes that could be decrypted.
   * 
   * @param message the message to decrypt
   * @return the message, decrypted and ready for consumption, or null if the message provided were an incomplete
   *         message.
   * @throws StreamException if the message cannot be decrypted
   */
  Bytes readFromServer(Bytes message);
}
