/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.toml;

import net.consensys.cava.toml.internal.TomlParser.ArrayContext;
import net.consensys.cava.toml.internal.TomlParser.ArrayValuesContext;
import net.consensys.cava.toml.internal.TomlParser.BinIntContext;
import net.consensys.cava.toml.internal.TomlParser.DecIntContext;
import net.consensys.cava.toml.internal.TomlParser.FalseBoolContext;
import net.consensys.cava.toml.internal.TomlParser.HexIntContext;
import net.consensys.cava.toml.internal.TomlParser.InlineTableContext;
import net.consensys.cava.toml.internal.TomlParser.InlineTableValuesContext;
import net.consensys.cava.toml.internal.TomlParser.LocalDateContext;
import net.consensys.cava.toml.internal.TomlParser.LocalDateTimeContext;
import net.consensys.cava.toml.internal.TomlParser.LocalTimeContext;
import net.consensys.cava.toml.internal.TomlParser.OctIntContext;
import net.consensys.cava.toml.internal.TomlParser.OffsetDateTimeContext;
import net.consensys.cava.toml.internal.TomlParser.RegularFloatContext;
import net.consensys.cava.toml.internal.TomlParser.RegularFloatInfContext;
import net.consensys.cava.toml.internal.TomlParser.RegularFloatNaNContext;
import net.consensys.cava.toml.internal.TomlParser.StringContext;
import net.consensys.cava.toml.internal.TomlParser.TrueBoolContext;
import net.consensys.cava.toml.internal.TomlParserBaseVisitor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.ParserRuleContext;

final class ValueVisitor extends TomlParserBaseVisitor<Object> {

  private static final Pattern zeroFloat = Pattern.compile("[+-]?0+(\\.[+-]?0*)?([eE].*)?");

  @Override
  public Object visitString(StringContext ctx) {
    return ctx.accept(new QuotedStringVisitor()).toString();
  }

  @Override
  public Object visitDecInt(DecIntContext ctx) {
    return toLong(ctx.getText().replaceAll("_", ""), 10, ctx);
  }

  @Override
  public Object visitHexInt(HexIntContext ctx) {
    return toLong(ctx.getText().substring(2).replaceAll("_", ""), 16, ctx);
  }

  @Override
  public Object visitOctInt(OctIntContext ctx) {
    return toLong(ctx.getText().substring(2).replaceAll("_", ""), 8, ctx);
  }

  @Override
  public Object visitBinInt(BinIntContext ctx) {
    return toLong(ctx.getText().substring(2).replaceAll("_", ""), 2, ctx);
  }

  private Long toLong(String s, int radix, ParserRuleContext ctx) {
    try {
      return Long.valueOf(s, radix);
    } catch (NumberFormatException e) {
      throw new TomlParseError("Integer is too large", new TomlPosition(ctx));
    }
  }

  @Override
  public Object visitRegularFloat(RegularFloatContext ctx) {
    return toDouble(ctx.getText().replaceAll("_", ""), ctx);
  }

  @Override
  public Object visitRegularFloatInf(RegularFloatInfContext ctx) {
    return (ctx.getText().startsWith("-")) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
  }

  @Override
  public Object visitRegularFloatNaN(RegularFloatNaNContext ctx) {
    return Double.NaN;
  }

  private Double toDouble(String s, ParserRuleContext ctx) {
    try {
      Double value = Double.valueOf(s);
      if (value == Double.POSITIVE_INFINITY || value == Double.NEGATIVE_INFINITY) {
        throw new TomlParseError("Float is too large", new TomlPosition(ctx));
      }
      if (value == 0d && !zeroFloat.matcher(s).matches()) {
        throw new TomlParseError("Float is too small", new TomlPosition(ctx));
      }
      return value;
    } catch (NumberFormatException e) {
      throw new TomlParseError("Invalid floating point number: " + e.getMessage(), new TomlPosition(ctx));
    }
  }

  @Override
  public Object visitTrueBool(TrueBoolContext ctx) {
    return Boolean.TRUE;
  }

  @Override
  public Object visitFalseBool(FalseBoolContext ctx) {
    return Boolean.FALSE;
  }

  @Override
  public Object visitOffsetDateTime(OffsetDateTimeContext ctx) {
    LocalDate date = ctx.date().accept(new LocalDateVisitor());
    LocalTime time = ctx.time().accept(new LocalTimeVisitor());
    ZoneOffset offset = ctx.timeOffset().accept(new ZoneOffsetVisitor());
    return OffsetDateTime.of(date, time, offset);
  }

  @Override
  public Object visitLocalDateTime(LocalDateTimeContext ctx) {
    LocalDate date = ctx.date().accept(new LocalDateVisitor());
    LocalTime time = ctx.time().accept(new LocalTimeVisitor());
    return LocalDateTime.of(date, time);
  }

  @Override
  public Object visitLocalDate(LocalDateContext ctx) {
    return ctx.date().accept(new LocalDateVisitor());
  }

  @Override
  public Object visitLocalTime(LocalTimeContext ctx) {
    return ctx.time().accept(new LocalTimeVisitor());
  }

  @Override
  public Object visitArray(ArrayContext ctx) {
    ArrayValuesContext valuesContext = ctx.arrayValues();
    if (valuesContext == null) {
      return MutableTomlArray.EMPTY;
    }
    return valuesContext.accept(new ArrayVisitor());
  }

  @Override
  public Object visitInlineTable(InlineTableContext ctx) {
    InlineTableValuesContext valuesContext = ctx.inlineTableValues();
    if (valuesContext == null) {
      return MutableTomlTable.EMPTY;
    }
    return valuesContext.accept(new InlineTableVisitor());
  }

  @Override
  protected Object aggregateResult(Object aggregate, Object nextResult) {
    return nextResult;
  }

  @Override
  protected Object defaultResult() {
    return null;
  }
}
