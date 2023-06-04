// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

/** An exception thrown when an invalid type is encountered. */
public class TomlInvalidTypeException extends RuntimeException {

  TomlInvalidTypeException(String message) {
    super(message);
  }
}
