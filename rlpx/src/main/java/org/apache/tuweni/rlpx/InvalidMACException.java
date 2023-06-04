// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx;

/** Exception thrown when the message contents do not match the Message Authentication Code. */
public class InvalidMACException extends RuntimeException {

  InvalidMACException(Throwable t) {
    super(t);
  }

  InvalidMACException(String msg) {
    super(msg);
  }
}
