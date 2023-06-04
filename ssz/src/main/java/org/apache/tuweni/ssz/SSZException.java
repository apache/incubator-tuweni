// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ssz;

/**
 * Base type for all SSZ encoding and decoding exceptions.
 */
public class SSZException extends RuntimeException {
  public SSZException(String message) {
    super(message);
  }

  public SSZException(Throwable cause) {
    super(cause);
  }

  public SSZException(String message, Throwable cause) {
    super(message, cause);
  }
}
