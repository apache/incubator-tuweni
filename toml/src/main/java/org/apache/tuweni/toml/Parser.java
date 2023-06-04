// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

import org.apache.tuweni.toml.internal.TomlLexer;
import org.apache.tuweni.toml.internal.TomlParser;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

final class Parser {
  private Parser() {}

  static TomlParseResult parse(CharStream stream, TomlVersion version) {
    TomlLexer lexer = new TomlLexer(stream);
    TomlParser parser = new TomlParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    AccumulatingErrorListener errorListener = new AccumulatingErrorListener();
    parser.addErrorListener(errorListener);
    ParseTree tree = parser.toml();
    TomlTable table = tree.accept(new LineVisitor(errorListener, version));

    return new TomlParseResult() {
      @Override
      public int size() {
        return table.size();
      }

      @Override
      public boolean isEmpty() {
        return table.isEmpty();
      }

      @Override
      public Set<String> keySet() {
        return table.keySet();
      }

      @Override
      public Set<List<String>> keyPathSet(boolean includeTables) {
        return table.keyPathSet(includeTables);
      }

      @Override
      @Nullable
      public Object get(List<String> path) {
        return table.get(path);
      }

      @Override
      @Nullable
      public TomlPosition inputPositionOf(List<String> path) {
        return table.inputPositionOf(path);
      }

      @Override
      public Map<String, Object> toMap() {
        return table.toMap();
      }

      @Override
      public List<TomlParseError> errors() {
        return errorListener.errors();
      }
    };
  }

  static List<String> parseDottedKey(String dottedKey) {
    TomlLexer lexer = new TomlLexer(CharStreams.fromString(dottedKey));
    lexer.mode(TomlLexer.KeyMode);
    TomlParser parser = new TomlParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    AccumulatingErrorListener errorListener = new AccumulatingErrorListener();
    parser.addErrorListener(errorListener);
    List<String> keyList = parser.tomlKey().accept(new KeyVisitor());
    List<TomlParseError> errors = errorListener.errors();
    if (!errors.isEmpty()) {
      TomlParseError e = errors.get(0);
      throw new IllegalArgumentException("Invalid key: " + e.getMessage(), e);
    }
    return keyList;
  }
}
