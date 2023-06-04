// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ssz;

/** Indicates that an unexpected type was encountered when decoding SSZ. */
public class InvalidSSZTypeException extends SSZException {
  public InvalidSSZTypeException(String message) {
    super(message);
  }
}
