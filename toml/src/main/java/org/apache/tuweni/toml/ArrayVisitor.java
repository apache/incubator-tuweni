// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

import org.apache.tuweni.toml.internal.TomlParser.ArrayValueContext;
import org.apache.tuweni.toml.internal.TomlParser.ValContext;
import org.apache.tuweni.toml.internal.TomlParserBaseVisitor;

final class ArrayVisitor extends TomlParserBaseVisitor<MutableTomlArray> {

  private final MutableTomlArray array = new MutableTomlArray(true);

  @Override
  public MutableTomlArray visitArrayValue(ArrayValueContext ctx) {
    ValContext valContext = ctx.val();
    if (valContext != null) {
      Object value = valContext.accept(new ValueVisitor());
      if (value != null) {
        TomlPosition position = new TomlPosition(ctx);
        try {
          array.append(value, position);
        } catch (TomlInvalidTypeException e) {
          throw new TomlParseError(e.getMessage(), position);
        }
      }
    }
    return array;
  }

  @Override
  protected MutableTomlArray aggregateResult(MutableTomlArray aggregate, MutableTomlArray nextResult) {
    return aggregate == null ? null : nextResult;
  }

  @Override
  protected MutableTomlArray defaultResult() {
    return array;
  }
}
