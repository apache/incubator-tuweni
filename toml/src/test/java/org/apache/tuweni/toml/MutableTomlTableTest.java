/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.toml;

import static org.apache.tuweni.toml.TomlPosition.positionAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MutableTomlTableTest {

  @Test
  void emptyTableIsEmpty() {
    TomlTable table = new MutableTomlTable();
    assertTrue(table.isEmpty());
    assertEquals(0, table.size());
  }

  @Test
  void getMissingPropertyReturnsNull() {
    MutableTomlTable table = new MutableTomlTable();
    table.set("bar", "one", positionAt(1, 1));
    table.set("foo.baz", "two", positionAt(1, 1));
    assertNull(table.get("baz"));
    assertNull(table.get("foo.bar"));
    assertNull(table.get("foo.bar.baz"));
  }

  @Test
  void getStringProperty() {
    MutableTomlTable table = new MutableTomlTable();
    table.set("foo.bar", "one", positionAt(1, 1));
    assertTrue(table.isString("foo.bar"));
    assertEquals("one", table.getString("foo.bar"));
  }

  @Test
  void shouldCreateParentTables() {
    MutableTomlTable table = new MutableTomlTable();
    table.set("foo.bar", "one", positionAt(1, 1));
    assertTrue(table.isTable("foo"));
    assertNotNull(table.getTable("foo"));
  }

  @Test
  void cannotReplaceProperty() {
    MutableTomlTable table = new MutableTomlTable();
    table.set("foo.bar", "one", positionAt(1, 3));
    TomlParseError e = assertThrows(TomlParseError.class, () -> {
      table.set("foo.bar", "two", positionAt(2, 5));
    });

    assertEquals("foo.bar previously defined at line 1, column 3", e.getMessage());
  }

  @ParameterizedTest
  @MethodSource("quotesComplexKeyInErrorSupplier")
  void quotesComplexKeysInError(List<String> path, String expected) {
    MutableTomlTable table = new MutableTomlTable();
    table.set(path, "one", positionAt(1, 3));
    TomlParseError e = assertThrows(TomlParseError.class, () -> {
      table.set(path, "two", positionAt(2, 5));
    });
    assertEquals(expected + " previously defined at line 1, column 3", e.getMessage());
  }

  private static Stream<Arguments> quotesComplexKeyInErrorSupplier() {
    return Stream
        .of(
            Arguments.of(Arrays.asList("", "bar"), "\"\".bar"),
            Arguments.of(Arrays.asList("foo ", "bar"), "\"foo \".bar"),
            Arguments.of(Arrays.asList("foo\n", "bar"), "\"foo\\n\".bar"));
  }

  @Test
  void cannotTreatNonTableAsTable() {
    MutableTomlTable table = new MutableTomlTable();
    table.set("foo.bar", "one", positionAt(5, 3));
    TomlParseError e = assertThrows(TomlParseError.class, () -> {
      table.set("foo.bar.baz", "two", positionAt(2, 5));
    });

    assertEquals("foo.bar is not a table (previously defined at line 5, column 3)", e.getMessage());
  }

  @Test
  void ignoresWhitespaceInUnquotedKeys() {
    MutableTomlTable table = new MutableTomlTable();
    table.set("foo.bar", 4, positionAt(5, 3));
    assertEquals(Long.valueOf(4), table.getLong(" foo . bar"));
    table.set(Arrays.asList(" Bar ", " B A Z "), 9, positionAt(5, 3));
    assertEquals(Long.valueOf(9), table.getLong("' Bar '.  \" B A Z \""));
  }

  @Test
  void throwsForInvalidKey() {
    MutableTomlTable table = new MutableTomlTable();
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
      table.get("foo.=bar");
    });
    assertEquals("Invalid key: Unexpected '=', expected a-z, A-Z, 0-9, ', or \"", e.getMessage());
  }

  @Test
  void shouldReturnInputPosition() {
    MutableTomlTable table = new MutableTomlTable();
    table.set("bar", "one", positionAt(4, 3));
    table.set("foo.baz", "two", positionAt(15, 2));
    assertEquals(positionAt(4, 3), table.inputPositionOf("bar"));
    assertEquals(positionAt(15, 2), table.inputPositionOf("foo.baz"));
    assertNull(table.inputPositionOf("baz"));
    assertNull(table.inputPositionOf("foo.bar"));
    assertNull(table.inputPositionOf("foo.bar.baz"));
  }

  @Test
  void shouldReturnKeySet() {
    MutableTomlTable table = new MutableTomlTable();
    table.set("bar", "one", positionAt(4, 3));
    table.set("foo.baz", "two", positionAt(15, 2));
    assertEquals(new HashSet<>(Arrays.asList("bar", "foo")), table.keySet());
  }

  @Test
  void shouldReturnDottedKeySet() {
    MutableTomlTable table = new MutableTomlTable();
    table.set("bar", "one", positionAt(4, 3));
    table.set("foo.baz", "two", positionAt(15, 2));
    table.set("foo.buz.bar", "three", positionAt(15, 2));
    assertEquals(
        new HashSet<>(Arrays.asList("bar", "foo", "foo.baz", "foo.buz", "foo.buz.bar")),
        table.dottedKeySet(true));
    assertEquals(new HashSet<>(Arrays.asList("bar", "foo.baz", "foo.buz.bar")), table.dottedKeySet());
  }

  @Test
  void shouldSerializeToJSON() {
    MutableTomlTable table = new MutableTomlTable();
    table.set("bar", "one", positionAt(2, 1));
    table.set("foo.baz", "two", positionAt(3, 2));
    table.set("foo.buz", MutableTomlArray.EMPTY, positionAt(3, 2));
    table.set("foo.foo", MutableTomlTable.EMPTY, positionAt(3, 2));
    MutableTomlArray array = new MutableTomlArray();
    array.append("hello\nthere", positionAt(5, 2));
    array.append("goodbye", positionAt(5, 2));
    table.set("foo.blah", array, positionAt(5, 2));
    table.set("buz", OffsetDateTime.parse("1937-07-18T03:25:43-04:00"), positionAt(5, 2));
    table.set("glad", LocalDateTime.parse("1937-07-18T03:25:43"), positionAt(5, 2));
    table.set("zoo", LocalDate.parse("1937-07-18"), positionAt(5, 2));
    table.set("alpha", LocalTime.parse("03:25:43"), positionAt(5, 2));
    String expected = "{\n"
        + "  \"alpha\" : \"03:25:43\",\n"
        + "  \"bar\" : \"one\",\n"
        + "  \"buz\" : \"1937-07-18T03:25:43-04:00\",\n"
        + "  \"foo\" : {\n"
        + "    \"baz\" : \"two\",\n"
        + "    \"blah\" : [\n"
        + "      \"hello\\nthere\",\n"
        + "      \"goodbye\"\n"
        + "    ],\n"
        + "    \"buz\" : [],\n"
        + "    \"foo\" : {}\n"
        + "  },\n"
        + "  \"glad\" : \"1937-07-18T03:25:43\",\n"
        + "  \"zoo\" : \"1937-07-18\"\n"
        + "}\n";
    assertEquals(expected.replace("\n", System.lineSeparator()), table.toJson());
  }

  @Test
  void shouldSerializeToJSONForEscapeCharacter() {
    MutableTomlTable table = new MutableTomlTable();
    table.set("foo", "one'two", positionAt(1, 1));
    table.set("bar", "hello\"there", positionAt(2, 1));
    table.set("buz", "good\tbye", positionAt(3, 1));
    table.set("bug", "alpha\bbeta", positionAt(4, 1));
    table.set("egg", "bread\rham", positionAt(5, 1));
    table.set("pet", "dog\fcat", positionAt(6, 1));
    table.set("red", "black\nwhite", positionAt(7, 1));
    String expected = "{\n"
        + "  \"bar\" : \"hello\\\"there\",\n"
        + "  \"bug\" : \"alpha\\bbeta\",\n"
        + "  \"buz\" : \"good\\tbye\",\n"
        + "  \"egg\" : \"bread\\rham\",\n"
        + "  \"foo\" : \"one'two\",\n"
        + "  \"pet\" : \"dog\\fcat\",\n"
        + "  \"red\" : \"black\\nwhite\"\n"
        + "}\n";
    assertEquals(expected.replace("\n", System.lineSeparator()), table.toJson());
  }
}
