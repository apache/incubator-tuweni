/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.crypto.sodium;

import org.apache.tuweni.bytes.Bytes;

import javax.security.auth.Destroyable;

/**
 * Used to encrypt a sequence of messages, or a single message split into arbitrary chunks.
 */
public interface SecretEncryptionStream extends Destroyable {

  /**
   * Returns the header for the stream
   * 
   * @return The header for the stream.
   */
  default Bytes header() {
    return Bytes.wrap(headerArray());
  }

  /**
   * Returns the header for the stream
   * 
   * @return The header for the stream.
   */
  byte[] headerArray();

  /**
   * Push a message to this secret stream.
   *
   * @param clearText The message to encrypt.
   * @return The encrypted message.
   */
  default Bytes push(Bytes clearText) {
    return push(clearText, false);
  }

  /**
   * Push a message to this secret stream.
   *
   * @param clearText The message to encrypt.
   * @return The encrypted message.
   */
  default byte[] push(byte[] clearText) {
    return push(clearText, false);
  }

  /**
   * Push the final message to this secret stream.
   *
   * @param clearText The message to encrypt.
   * @return The encrypted message.
   */
  default Bytes pushLast(Bytes clearText) {
    return push(clearText, true);
  }

  /**
   * Push the final message to this secret stream.
   *
   * @param clearText The message to encrypt.
   * @return The encrypted message.
   */
  default byte[] pushLast(byte[] clearText) {
    return push(clearText, true);
  }

  /**
   * Push a message to this secret stream.
   *
   * @param clearText The message to encrypt.
   * @param isFinal {@code true} if this is the final message that will be sent on this stream.
   * @return The encrypted message.
   */
  default Bytes push(Bytes clearText, boolean isFinal) {
    return Bytes.wrap(push(clearText.toArrayUnsafe(), isFinal));
  }

  /**
   * Push a message to this secret stream.
   *
   * @param clearText The message to encrypt.
   * @param isFinal {@code true} if this is the final message that will be sent on this stream.
   * @return The encrypted message.
   */
  byte[] push(byte[] clearText, boolean isFinal);
}
