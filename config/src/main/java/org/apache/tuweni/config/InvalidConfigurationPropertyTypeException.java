// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.config;

import org.jetbrains.annotations.Nullable;

/** An exception thrown when an invalid type is encountered. */
public final class InvalidConfigurationPropertyTypeException extends RuntimeException {

  @Nullable private final DocumentPosition position;

  InvalidConfigurationPropertyTypeException(@Nullable DocumentPosition position, String message) {
    super(message);
    this.position = position;
  }

  InvalidConfigurationPropertyTypeException(
      @Nullable DocumentPosition position, String message, @Nullable Throwable cause) {
    super(message, cause);
    this.position = position;
  }

  /**
   * Provides the position of the property in the configuration document.
   *
   * @return The position of the property in the configuration document, or {@code null} if there is
   *     no position available.
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
