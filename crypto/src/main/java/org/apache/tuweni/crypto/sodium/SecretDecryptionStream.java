// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import org.apache.tuweni.bytes.Bytes;

import javax.security.auth.Destroyable;

/** Used to decrypt a sequence of messages, or a single message split into arbitrary chunks. */
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
