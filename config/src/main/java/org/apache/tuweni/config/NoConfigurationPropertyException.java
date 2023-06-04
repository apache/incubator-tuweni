// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.config;

/**
 * An exception thrown when a requested configuration property is not found.
 *
 * <p>
 * This exception can be avoided by using a schema that provides a default value or asserts that a value has been
 * provided in the configuration.
 */
public final class NoConfigurationPropertyException extends RuntimeException {

  NoConfigurationPropertyException(String message) {
    super(message);
  }
}
