// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

interface ErrorReporter {
  void reportError(TomlParseError error);
}
