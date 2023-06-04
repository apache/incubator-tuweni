// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.config;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/** Factory methods for collections of {@link ConfigurationError}. */
public final class ConfigurationErrors {
  private ConfigurationErrors() {}

  /**
   * Provides an empty list of errors.
   *
   * @return An empty list of errors.
   */
  public static List<ConfigurationError> noErrors() {
    return Collections.emptyList();
  }

  /**
   * Create a single error.
   *
   * @param message The error message.
   * @return A list containing a single error.
   */
  public static List<ConfigurationError> singleError(String message) {
    return Collections.singletonList(new ConfigurationError(message));
  }

  /**
   * Create a single error.
   *
   * @param message The error message.
   * @param cause The cause of the error.
   * @return A list containing a single error.
   */
  public static List<ConfigurationError> singleError(String message, @Nullable Throwable cause) {
    return Collections.singletonList(new ConfigurationError(message, cause));
  }

  /**
   * Create a single error.
   *
   * @param position The location of the error in the configuration document.
   * @param message The error message.
   * @return A list containing a single error.
   */
  public static List<ConfigurationError> singleError(
      @Nullable DocumentPosition position, String message) {
    return Collections.singletonList(new ConfigurationError(position, message));
  }

  /**
   * Create a single error.
   *
   * @param position The location of the error in the configuration document.
   * @param message The error message.
   * @param cause The cause of the error.
   * @return A list containing a single error.
   */
  public static List<ConfigurationError> singleError(
      @Nullable DocumentPosition position, String message, @Nullable Throwable cause) {
    return Collections.singletonList(new ConfigurationError(position, message, cause));
  }
}
