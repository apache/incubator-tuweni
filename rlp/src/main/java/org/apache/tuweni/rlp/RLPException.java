// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlp;

/** Base type for all RLP encoding and decoding exceptions. */
public class RLPException extends RuntimeException {
  public RLPException(String message) {
    super(message);
  }

  public RLPException(Throwable cause) {
    super(cause);
  }

  public RLPException(String message, Throwable cause) {
    super(message, cause);
  }
}
