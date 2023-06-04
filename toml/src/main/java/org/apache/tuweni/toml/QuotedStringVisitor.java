// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

import org.apache.tuweni.toml.internal.TomlParser.BasicUnescapedContext;
import org.apache.tuweni.toml.internal.TomlParser.EscapedContext;
import org.apache.tuweni.toml.internal.TomlParser.LiteralBodyContext;
import org.apache.tuweni.toml.internal.TomlParser.MlBasicUnescapedContext;
import org.apache.tuweni.toml.internal.TomlParser.MlLiteralBodyContext;
import org.apache.tuweni.toml.internal.TomlParserBaseVisitor;

final class QuotedStringVisitor extends TomlParserBaseVisitor<StringBuilder> {

  private final StringBuilder builder = new StringBuilder();

  @Override
  public StringBuilder visitLiteralBody(LiteralBodyContext ctx) {
    return builder.append(ctx.getText());
  }

  @Override
  public StringBuilder visitMlLiteralBody(MlLiteralBodyContext ctx) {
    return builder.append(ctx.getText());
  }

  @Override
  public StringBuilder visitBasicUnescaped(BasicUnescapedContext ctx) {
    return builder.append(ctx.getText());
  }

  @Override
  public StringBuilder visitMlBasicUnescaped(MlBasicUnescapedContext ctx) {
    return builder.append(ctx.getText());
  }

  @Override
  public StringBuilder visitEscaped(EscapedContext ctx) {
    String text = ctx.getText();
    if (text.isEmpty()) {
      return builder;
    }
    assert (text.charAt(0) == '\\');
    if (text.length() == 1) {
      return builder.append('\\');
    }
    switch (text.charAt(1)) {
      case '"':
        return builder.append('"');
      case '\\':
        return builder.append('\\');
      case 'b':
        return builder.append('\b');
      case 'f':
        return builder.append('\f');
      case 'n':
        return builder.append('\n');
      case 'r':
        return builder.append('\r');
      case 't':
        return builder.append('\t');
      case 'u':
        assert (text.length() == 6);
        return builder.append(convertUnicodeEscape(text.substring(2), ctx));
      case 'U':
        assert (text.length() == 10);
        return builder.append(convertUnicodeEscape(text.substring(2), ctx));
      default:
        throw new TomlParseError("Invalid escape sequence '" + text + "'", new TomlPosition(ctx));
    }
  }

  private char[] convertUnicodeEscape(String hexChars, EscapedContext ctx) {
    try {
      return Character.toChars(Integer.parseInt(hexChars, 16));
    } catch (IllegalArgumentException e) {
      throw new TomlParseError("Invalid unicode escape sequence", new TomlPosition(ctx));
    }
  }

  @Override
  protected StringBuilder aggregateResult(StringBuilder aggregate, StringBuilder nextResult) {
    return aggregate == null ? null : nextResult;
  }

  @Override
  protected StringBuilder defaultResult() {
    return builder;
  }
}
