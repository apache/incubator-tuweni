// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

import java.util.List;

/**
 * The result from parsing a TOML document.
 */
public interface TomlParseResult extends TomlTable {

  /**
   * Returns true if the TOML document contained errors.
   * 
   * @return {@code true} if the TOML document contained errors.
   */
  default boolean hasErrors() {
    return !(errors().isEmpty());
  }

  /**
   * The errors that occurred during parsing.
   *
   * @return A list of errors.
   */
  List<TomlParseError> errors();
}
