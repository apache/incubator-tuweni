// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

import org.apache.tuweni.toml.internal.TomlParser.HourContext;
import org.apache.tuweni.toml.internal.TomlParser.MinuteContext;
import org.apache.tuweni.toml.internal.TomlParser.SecondContext;
import org.apache.tuweni.toml.internal.TomlParser.SecondFractionContext;
import org.apache.tuweni.toml.internal.TomlParserBaseVisitor;

import java.time.LocalTime;

import org.antlr.v4.runtime.tree.ErrorNode;

final class LocalTimeVisitor extends TomlParserBaseVisitor<LocalTime> {

  private LocalTime time = LocalTime.MIN;

  @Override
  public LocalTime visitHour(HourContext ctx) {
    String text = ctx.getText();
    if (text.length() != 2) {
      throw new TomlParseError("Invalid hour (valid range 00..23)", new TomlPosition(ctx));
    }
    int hour;
    try {
      hour = Integer.parseInt(text);
    } catch (NumberFormatException e) {
      throw new TomlParseError("Invalid hour", new TomlPosition(ctx), e);
    }
    if (hour < 0 || hour > 23) {
      throw new TomlParseError("Invalid hour (valid range 00..23)", new TomlPosition(ctx));
    }
    time = time.withHour(hour);
    return time;
  }

  @Override
  public LocalTime visitMinute(MinuteContext ctx) {
    String text = ctx.getText();
    if (text.length() != 2) {
      throw new TomlParseError("Invalid minutes (valid range 00..59)", new TomlPosition(ctx));
    }
    int minute;
    try {
      minute = Integer.parseInt(text);
    } catch (NumberFormatException e) {
      throw new TomlParseError("Invalid minutes", new TomlPosition(ctx), e);
    }
    if (minute < 0 || minute > 59) {
      throw new TomlParseError("Invalid minutes (valid range 00..59)", new TomlPosition(ctx));
    }
    time = time.withMinute(minute);
    return time;
  }

  @Override
  public LocalTime visitSecond(SecondContext ctx) {
    String text = ctx.getText();
    if (text.length() != 2) {
      throw new TomlParseError("Invalid seconds (valid range 00..59)", new TomlPosition(ctx));
    }
    int second;
    try {
      second = Integer.parseInt(text);
    } catch (NumberFormatException e) {
      throw new TomlParseError("Invalid seconds", new TomlPosition(ctx), e);
    }
    if (second < 0 || second > 59) {
      throw new TomlParseError("Invalid seconds (valid range 00..59)", new TomlPosition(ctx));
    }
    time = time.withSecond(second);
    return time;
  }

  @Override
  public LocalTime visitSecondFraction(SecondFractionContext ctx) {
    String text = ctx.getText();
    if (text.isEmpty() || text.length() > 9) {
      throw new TomlParseError("Invalid nanoseconds (valid range 0..999999999)", new TomlPosition(ctx));
    }
    if (text.length() < 9) {
      text = text + "000000000".substring(text.length());
    }
    int nano;
    try {
      nano = Integer.parseInt(text);
    } catch (NumberFormatException e) {
      throw new TomlParseError("Invalid nanoseconds", new TomlPosition(ctx), e);
    }
    time = time.withNano(nano);
    return time;
  }

  @Override
  public LocalTime visitErrorNode(ErrorNode node) {
    return null;
  }

  @Override
  protected LocalTime aggregateResult(LocalTime aggregate, LocalTime nextResult) {
    return aggregate == null ? null : nextResult;
  }

  @Override
  protected LocalTime defaultResult() {
    return time;
  }
}
