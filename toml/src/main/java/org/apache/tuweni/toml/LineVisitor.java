// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

import static org.apache.tuweni.toml.TomlVersion.V0_4_0;

import org.apache.tuweni.toml.internal.TomlParser.ArrayTableContext;
import org.apache.tuweni.toml.internal.TomlParser.KeyContext;
import org.apache.tuweni.toml.internal.TomlParser.KeyvalContext;
import org.apache.tuweni.toml.internal.TomlParser.StandardTableContext;
import org.apache.tuweni.toml.internal.TomlParser.ValContext;
import org.apache.tuweni.toml.internal.TomlParserBaseVisitor;

import java.util.List;

final class LineVisitor extends TomlParserBaseVisitor<MutableTomlTable> {

  private final MutableTomlTable table = new MutableTomlTable();
  private final ErrorReporter errorReporter;
  private final TomlVersion version;
  private MutableTomlTable currentTable = table;

  LineVisitor(ErrorReporter errorReporter, TomlVersion version) {
    this.errorReporter = errorReporter;
    this.version = version;
  }

  @Override
  public MutableTomlTable visitKeyval(KeyvalContext ctx) {
    KeyContext keyContext = ctx.key();
    ValContext valContext = ctx.val();
    if (keyContext == null || valContext == null) {
      return table;
    }
    try {
      List<String> path = keyContext.accept(new KeyVisitor());
      if (path == null || path.isEmpty()) {
        return table;
      }
      // TOML 0.4.0 doesn't support dotted keys
      if (!version.after(V0_4_0) && path.size() > 1) {
        throw new TomlParseError("Dotted keys are not supported", new TomlPosition(keyContext));
      }
      Object value = valContext.accept(new ValueVisitor());
      if (value != null) {
        currentTable.set(path, value, new TomlPosition(ctx));
      }
      return table;
    } catch (TomlParseError e) {
      errorReporter.reportError(e);
      return table;
    }
  }

  @Override
  public MutableTomlTable visitStandardTable(StandardTableContext ctx) {
    KeyContext keyContext = ctx.key();
    if (keyContext == null) {
      errorReporter.reportError(new TomlParseError("Empty table key", new TomlPosition(ctx)));
      return table;
    }
    List<String> path = keyContext.accept(new KeyVisitor());
    if (path == null) {
      return table;
    }
    try {
      currentTable = table.createTable(path, new TomlPosition(ctx));
    } catch (TomlParseError e) {
      errorReporter.reportError(e);
    }
    return table;
  }

  @Override
  public MutableTomlTable visitArrayTable(ArrayTableContext ctx) {
    KeyContext keyContext = ctx.key();
    if (keyContext == null) {
      errorReporter.reportError(new TomlParseError("Empty table key", new TomlPosition(ctx)));
      return table;
    }
    List<String> path = keyContext.accept(new KeyVisitor());
    if (path == null) {
      return table;
    }
    try {
      currentTable = table.createArrayTable(path, new TomlPosition(ctx));
    } catch (TomlParseError e) {
      errorReporter.reportError(e);
    }
    return table;
  }

  @Override
  protected MutableTomlTable aggregateResult(MutableTomlTable aggregate, MutableTomlTable nextResult) {
    return aggregate == null ? null : nextResult;
  }

  @Override
  protected MutableTomlTable defaultResult() {
    return table;
  }
}
