// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

import static org.apache.tuweni.toml.TomlPosition.positionAt;

import org.apache.tuweni.toml.internal.TomlLexer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.IntervalSet;

final class AccumulatingErrorListener extends BaseErrorListener implements ErrorReporter {

  private final List<TomlParseError> errors = new ArrayList<>();

  @Override
  public void syntaxError(
      Recognizer<?, ?> recognizer,
      Object offendingSymbol,
      int line,
      int charPosition,
      String msg,
      RecognitionException e) {

    TomlPosition position = positionAt(line, charPosition + 1);

    if (e instanceof InputMismatchException || e instanceof NoViableAltException) {
      String message = getMessage(e.getOffendingToken(), getExpected(e));
      reportError(message, position);
      return;
    }

    if (offendingSymbol instanceof Token && recognizer instanceof Parser) {
      String message =
          getMessage(
              (Token) offendingSymbol, getExpected(((Parser) recognizer).getExpectedTokens()));
      reportError(message, position);
      return;
    }

    reportError(msg, position);
  }

  @Override
  public void reportError(TomlParseError error) {
    errors.add(error);
  }

  private void reportError(String message, TomlPosition position) {
    reportError(new TomlParseError(message, position));
  }

  List<TomlParseError> errors() {
    return errors;
  }

  private String getMessage(Token token, String expected) {
    return "Unexpected " + getTokenName(token) + ", expected " + expected;
  }

  private static String getTokenName(Token token) {
    int tokenType = token.getType();
    switch (tokenType) {
      case TomlLexer.NewLine:
        return "end of line";
      case TomlLexer.EOF:
        return "end of input";
      default:
        return "'" + Toml.tomlEscape(token.getText()) + '\'';
    }
  }

  private static String getExpected(RecognitionException e) {
    IntervalSet expectedTokens = e.getExpectedTokens();
    return getExpected(expectedTokens);
  }

  private static String getExpected(IntervalSet expectedTokens) {
    List<String> sortedNames =
        expectedTokens.getIntervals().stream()
            .flatMap(i -> IntStream.rangeClosed(i.a, i.b).boxed())
            .flatMap(TokenName::namesForToken)
            .sorted()
            .distinct()
            .map(TokenName::displayName)
            .collect(Collectors.toList());

    StringBuilder builder = new StringBuilder();
    int count = sortedNames.size();
    for (int i = 0; i < count; ++i) {
      builder.append(sortedNames.get(i));
      if (i < (count - 2)) {
        builder.append(", ");
      } else if (i == (count - 2)) {
        if (count >= 3) {
          builder.append(',');
        }
        builder.append(" or ");
      }
    }

    return builder.toString();
  }
}
