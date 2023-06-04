// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

/**
 * An error that occurred while parsing.
 */
public final class TomlParseError extends RuntimeException {

  private final TomlPosition position;

  TomlParseError(String message, TomlPosition position) {
    super(message);
    this.position = position;
  }

  TomlParseError(String message, TomlPosition position, Throwable cause) {
    super(message, cause);
    this.position = position;
  }

  /**
   * Provides the position in the input where the error occurred.
   * 
   * @return The position in the input where the error occurred.
   */
  public TomlPosition position() {
    return position;
  }

  public String getMessageWithoutPosition() {
    return super.getMessage();
  }

  @Override
  public String getMessage() {
    return super.getMessage() + " (" + position + ")";
  }
}
