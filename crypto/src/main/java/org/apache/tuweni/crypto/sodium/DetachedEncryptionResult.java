// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import org.apache.tuweni.bytes.Bytes;

/** The result from a detached encryption. */
public interface DetachedEncryptionResult {

  /**
   * Provides the cipher text
   *
   * @return The cipher text.
   */
  Bytes cipherText();

  /**
   * Provides the cipher text
   *
   * @return The cipher text.
   */
  byte[] cipherTextArray();

  /**
   * Provides the message authentication code
   *
   * @return The message authentication code.
   */
  Bytes mac();

  /**
   * Provides the message authentication code
   *
   * @return The message authentication code.
   */
  byte[] macArray();
}
