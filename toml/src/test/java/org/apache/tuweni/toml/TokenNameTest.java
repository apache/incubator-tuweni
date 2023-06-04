// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.toml.internal.TomlLexer;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TokenNameTest {

  @Test
  void shouldHaveTokenNameForAllTokens() {
    List<String> missing = new ArrayList<>();
    for (int i = 0; i < TomlLexer.VOCABULARY.getMaxTokenType(); ++i) {
      if (!TokenName.namesForToken(i).findFirst().isPresent()) {
        missing.add(TomlLexer.VOCABULARY.getSymbolicName(i));
      }
    }
    assertTrue(missing.isEmpty(), () -> "No TokenName's for " + String.join(", ", missing));
  }
}
