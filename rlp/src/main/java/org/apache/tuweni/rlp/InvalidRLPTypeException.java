// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlp;

/** Indicates that an unexpected type was encountered when decoding RLP. */
public class InvalidRLPTypeException extends RLPException {
  public InvalidRLPTypeException(String message) {
    super(message);
  }
}
