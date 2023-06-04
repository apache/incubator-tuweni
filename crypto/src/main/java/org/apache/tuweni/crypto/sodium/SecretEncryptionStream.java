// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import org.apache.tuweni.bytes.Bytes;

import javax.security.auth.Destroyable;

/** Used to encrypt a sequence of messages, or a single message split into arbitrary chunks. */
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
