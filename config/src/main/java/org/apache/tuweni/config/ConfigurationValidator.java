// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.config;

import java.util.List;

/**
 * A validator for a configuration.
 *
 * <p>
 * Validators of this type are invoked during verification after all property validators. However, errors returned by
 * property validators do not prevent this validator being evaluated, so properties of the configuration may be missing
 * or invalid.
 */
public interface ConfigurationValidator {

  /**
   * Validate a configuration.
   *
   * @param configuration The value associated with the configuration entry.
   * @return A list of error messages. If no errors are found, an empty list should be returned.
   */
  List<ConfigurationError> validate(Configuration configuration);
}
