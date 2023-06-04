// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.config;

import static org.apache.tuweni.config.ConfigurationErrors.noErrors;
import static org.apache.tuweni.config.ConfigurationErrors.singleError;

final class PropertyValidators {
  private PropertyValidators() {}

  static final PropertyValidator<Object> IS_PRESENT =
      (key, position, value) -> {
        if (value == null) {
          return singleError(position, "Required property '" + key + "' is missing");
        }
        return noErrors();
      };
}
