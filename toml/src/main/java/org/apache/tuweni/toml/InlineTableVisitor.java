// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

import org.apache.tuweni.toml.internal.TomlParser.KeyContext;
import org.apache.tuweni.toml.internal.TomlParser.KeyvalContext;
import org.apache.tuweni.toml.internal.TomlParser.ValContext;
import org.apache.tuweni.toml.internal.TomlParserBaseVisitor;

import java.util.List;

final class InlineTableVisitor extends TomlParserBaseVisitor<MutableTomlTable> {

  private final MutableTomlTable table = new MutableTomlTable();

  @Override
  public MutableTomlTable visitKeyval(KeyvalContext ctx) {
    KeyContext keyContext = ctx.key();
    ValContext valContext = ctx.val();
    if (keyContext != null && valContext != null) {
      List<String> path = keyContext.accept(new KeyVisitor());
      if (path != null && !path.isEmpty()) {
        Object value = valContext.accept(new ValueVisitor());
        if (value != null) {
          table.set(path, value, new TomlPosition(ctx));
        }
      }
    }
    return table;
  }

  @Override
  protected MutableTomlTable aggregateResult(MutableTomlTable aggregate, MutableTomlTable nextResult) {
    return table;
  }

  @Override
  protected MutableTomlTable defaultResult() {
    return table;
  }
}
