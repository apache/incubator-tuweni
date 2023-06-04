// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlp;

/** Indicates the end of the RLP source has been reached unexpectedly. */
public class EndOfRLPException extends RLPException {
  public EndOfRLPException() {
    super("End of RLP source reached");
  }
}
