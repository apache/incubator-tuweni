// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

import org.apache.tuweni.toml.internal.TomlParser.QuotedKeyContext;
import org.apache.tuweni.toml.internal.TomlParser.UnquotedKeyContext;
import org.apache.tuweni.toml.internal.TomlParserBaseVisitor;

import java.util.ArrayList;
import java.util.List;

final class KeyVisitor extends TomlParserBaseVisitor<List<String>> {

  private final List<String> keys = new ArrayList<>();

  @Override
  public List<String> visitUnquotedKey(UnquotedKeyContext ctx) {
    keys.add(ctx.getText());
    return keys;
  }

  @Override
  public List<String> visitQuotedKey(QuotedKeyContext ctx) {
    StringBuilder builder = ctx.accept(new QuotedStringVisitor());
    keys.add(builder.toString());
    return keys;
  }

  @Override
  protected List<String> aggregateResult(List<String> aggregate, List<String> nextResult) {
    return aggregate == null ? null : nextResult;
  }

  @Override
  protected List<String> defaultResult() {
    return keys;
  }
}
