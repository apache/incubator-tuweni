// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ssz;

/** Indicates the end of the SSZ source has been reached unexpectedly. */
public class EndOfSSZException extends SSZException {
  public EndOfSSZException() {
    super("End of SSZ source reached");
  }
}
