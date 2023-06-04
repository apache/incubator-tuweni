// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlp;

/**
 * Indicates that invalid RLP encoding was encountered.
 */
public class InvalidRLPEncodingException extends RLPException {
  public InvalidRLPEncodingException(String message) {
    super(message);
  }
}
