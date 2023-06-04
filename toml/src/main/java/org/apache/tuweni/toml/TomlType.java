// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.toml;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;

enum TomlType {
  STRING("string", String.class),
  INTEGER("integer", Long.class),
  FLOAT("float", Double.class),
  BOOLEAN("boolean", Boolean.class),
  OFFSET_DATE_TIME("offset date-time", OffsetDateTime.class),
  LOCAL_DATE_TIME("local date-time", LocalDateTime.class),
  LOCAL_DATE("local date", LocalDate.class),
  LOCAL_TIME("local time", LocalTime.class),
  ARRAY("array", TomlArray.class),
  TABLE("table", TomlTable.class);

  private final String name;
  private final Class<?> clazz;

  TomlType(String name, Class<?> clazz) {
    this.name = name;
    this.clazz = clazz;
  }

  static Optional<TomlType> typeFor(Object obj) {
    return typeForClass(obj.getClass());
  }

  static Optional<TomlType> typeForClass(Class<?> clazz) {
    return Arrays.stream(values()).filter(t -> t.clazz.isAssignableFrom(clazz)).findAny();
  }

  static String typeNameFor(Object obj) {
    return typeNameForClass(obj.getClass());
  }

  static String typeNameForClass(Class<?> clazz) {
    return typeForClass(clazz).map(t -> t.name).orElseGet(clazz::getSimpleName);
  }

  public String typeName() {
    return name;
  }
}
