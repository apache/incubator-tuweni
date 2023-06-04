// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto;

/**
 * Exception thrown when encryption fails.
 */
public class EncryptionException extends RuntimeException {

  public EncryptionException(Throwable t) {
    super(t);
  }
}
