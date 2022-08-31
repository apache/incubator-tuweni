/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.config;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Factory methods for collections of {@link ConfigurationError}.
 */
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
  public static List<ConfigurationError> singleError(@Nullable DocumentPosition position, String message) {
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
      @Nullable DocumentPosition position,
      String message,
      @Nullable Throwable cause) {
    return Collections.singletonList(new ConfigurationError(position, message, cause));
  }
}
