// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

import static org.apache.tuweni.toml.TomlPosition.positionAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class MutableTomlArrayTest {

  @Test
  void emptyArrayContainsAllTypes() {
    TomlArray array = new MutableTomlArray();
    assertTrue(array.isEmpty());
    assertEquals(0, array.size());
    assertTrue(array.containsStrings());
    assertTrue(array.containsLongs());
    assertTrue(array.containsDoubles());
    assertTrue(array.containsBooleans());
    assertTrue(array.containsOffsetDateTimes());
    assertTrue(array.containsLocalDateTimes());
    assertTrue(array.containsLocalDates());
    assertTrue(array.containsLocalTimes());
    assertTrue(array.containsArrays());
    assertTrue(array.containsTables());
  }

  @Test
  void arrayContainsTypeAfterAddingItem() {
    MutableTomlArray array = new MutableTomlArray().append("foo", positionAt(2, 3));
    assertFalse(array.isEmpty());
    assertEquals(1, array.size());
    assertTrue(array.containsStrings());
    assertFalse(array.containsLongs());
    assertFalse(array.containsDoubles());
    assertFalse(array.containsBooleans());
    assertFalse(array.containsOffsetDateTimes());
    assertFalse(array.containsLocalDateTimes());
    assertFalse(array.containsLocalDates());
    assertFalse(array.containsLocalTimes());
    assertFalse(array.containsArrays());
    assertFalse(array.containsTables());
  }

  @Test
  void cannotAppendUnsupportedType() {
    MutableTomlArray array = new MutableTomlArray();
    assertThrows(IllegalArgumentException.class, () -> array.append(this, positionAt(1, 1)));
    assertThrows(NullPointerException.class, () -> array.append(null, positionAt(1, 1)));
  }

  @Test
  void cannotAppendDifferentTypes() {
    MutableTomlArray array = new MutableTomlArray();
    array.append("Foo", positionAt(1, 1));
    assertThrows(TomlInvalidTypeException.class, () -> array.append(1L, positionAt(1, 1)));
    array.append("Bar", positionAt(1, 1));
    assertEquals(2, array.size());
  }

  @Test
  void shouldReturnNullForUnknownIndex() {
    MutableTomlArray array = new MutableTomlArray();
    assertThrows(IndexOutOfBoundsException.class, () -> array.get(0));
  }

  @Test
  void shouldReturnInputPosition() {
    MutableTomlArray array = new MutableTomlArray();
    array.append("Foo", positionAt(4, 3));
    array.append("Bar", positionAt(9, 5));
    assertEquals(positionAt(4, 3), array.inputPositionOf(0));
    assertEquals(positionAt(9, 5), array.inputPositionOf(1));
    assertThrows(IndexOutOfBoundsException.class, () -> array.get(2));
  }

  @Test
  void shouldGetDouble() {
    MutableTomlArray array = new MutableTomlArray().append(23.5d, positionAt(1, 1));
    assertEquals(23.5d, array.getDouble(0));
  }

  @Test
  void shouldGetLong() {
    MutableTomlArray array = new MutableTomlArray().append(23L, positionAt(1, 1));
    assertEquals(23L, array.getLong(0));
  }

  @Test
  void shouldGetBoolean() {
    MutableTomlArray array = new MutableTomlArray().append(false, positionAt(1, 1));
    assertEquals(false, array.getBoolean(0));
  }

  @Test
  void shouldGetOffSetDateTime() {
    OffsetDateTime time = OffsetDateTime.now(ZoneId.of("America/Los_Angeles"));
    MutableTomlArray array = new MutableTomlArray().append(time, positionAt(1, 1));
    assertEquals(time, array.getOffsetDateTime(0));
  }

  @Test
  void shouldGetLocalDateTime() {
    LocalDateTime time = LocalDateTime.now(ZoneId.of("America/Los_Angeles"));
    MutableTomlArray array = new MutableTomlArray().append(time, positionAt(1, 1));
    assertEquals(time, array.getLocalDateTime(0));
  }

  @Test
  void shouldGetLocalDate() {
    LocalDate time = LocalDate.now(ZoneId.of("America/Los_Angeles"));
    MutableTomlArray array = new MutableTomlArray().append(time, positionAt(1, 1));
    assertEquals(time, array.getLocalDate(0));
  }

  @Test
  void shouldGetLocalTime() {
    LocalTime time = LocalTime.now(ZoneId.of("America/Los_Angeles"));
    MutableTomlArray array = new MutableTomlArray().append(time, positionAt(1, 1));
    assertEquals(time, array.getLocalTime(0));
  }

  @Test
  void toJson() {
    MutableTomlArray array = new MutableTomlArray().append("foo", positionAt(1, 1)).append("bar", positionAt(10, 1));
    assertEquals(
        "["
            + System.lineSeparator()
            + "  \"foo\","
            + System.lineSeparator()
            + "  \"bar\""
            + System.lineSeparator()
            + "]"
            + System.lineSeparator(),
        array.toJson());
  }
}
