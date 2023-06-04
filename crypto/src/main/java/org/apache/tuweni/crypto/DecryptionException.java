// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto;

/**
 * Exception thrown when decryption fails.
 */
public class DecryptionException extends RuntimeException {

  public DecryptionException(Throwable t) {
    super(t);
  }
}
