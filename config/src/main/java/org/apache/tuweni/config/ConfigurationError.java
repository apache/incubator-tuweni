// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.config;

import org.jetbrains.annotations.Nullable;

/** Provides details regarding an error in the configuration. */
public final class ConfigurationError extends RuntimeException {

  @Nullable private final DocumentPosition position;

  /**
   * Construct an error.
   *
   * @param message The error message.
   */
  public ConfigurationError(String message) {
    this(null, message);
  }

  /**
   * Construct an error.
   *
   * @param message The error message.
   * @param cause The cause of the error.
   */
  public ConfigurationError(String message, @Nullable Throwable cause) {
    this(null, message, cause);
  }

  /**
   * Construct an error.
   *
   * @param position The location of the error in the configuration document.
   * @param message The error message.
   */
  public ConfigurationError(@Nullable DocumentPosition position, String message) {
    super(message);
    this.position = position;
  }

  /**
   * Construct an error.
   *
   * @param position The location of the error in the configuration document.
   * @param message The error message.
   * @param cause The cause of the error.
   */
  public ConfigurationError(
      @Nullable DocumentPosition position, String message, @Nullable Throwable cause) {
    super(message, cause);
    this.position = position;
  }

  /**
   * Provides the position in the input where the error occurred.
   *
   * @return The position in the input where the error occurred, or {@code null} if no position
   *     information is available.
   */
  @Nullable
  public DocumentPosition position() {
    return position;
  }

  public String getMessageWithoutPosition() {
    return super.getMessage();
  }

  @Override
  public String getMessage() {
    if (position == null) {
      return super.getMessage();
    }
    return super.getMessage() + " (" + position + ")";
  }
}
