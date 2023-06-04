// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

/**
 * An exception that is thrown when an error occurs using the native sodium library.
 */
public final class SodiumException extends RuntimeException {

  /**
   * @param message The exception message.
   */
  public SodiumException(String message) {
    super(message);
  }
}
