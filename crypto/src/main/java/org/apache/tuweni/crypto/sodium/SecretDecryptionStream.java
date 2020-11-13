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
 * Used to decrypt a sequence of messages, or a single message split into arbitrary chunks.
 */
public interface SecretDecryptionStream extends Destroyable {

  /**
   * Pull a message from this secret stream.
   *
   * @param cipherText The encrypted message.
   * @return The clear text.
   */
  default Bytes pull(Bytes cipherText) {
    return Bytes.wrap(pull(cipherText.toArrayUnsafe()));
  }

  /**
   * Pull a message from this secret stream.
   *
   * @param cipherText The encrypted message.
   * @return The clear text.
   */
  byte[] pull(byte[] cipherText);

  /**
   * Returns true if the stream is complete
   * 
   * @return {@code true} if no more messages should be decrypted by this stream
   */
  boolean isComplete();
}
