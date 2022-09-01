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
package org.apache.tuweni.config;

import static org.apache.tuweni.toml.Toml.joinKeyPath;
import static org.apache.tuweni.toml.Toml.parseDottedKey;

import org.apache.tuweni.toml.TomlArray;
import org.apache.tuweni.toml.TomlInvalidTypeException;
import org.apache.tuweni.toml.TomlParseResult;
import org.apache.tuweni.toml.TomlPosition;
import org.apache.tuweni.toml.TomlTable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jetbrains.annotations.Nullable;

final class TomlBackedConfiguration implements Configuration {

  private final TomlTable toml;
  private final Schema schema;
  private final List<ConfigurationError> errors;

  TomlBackedConfiguration(TomlParseResult toml, @Nullable Schema schema) {
    List<ConfigurationError> errors = new ArrayList<>();
    toml
        .errors()
        .forEach(err -> errors.add(new ConfigurationError(documentPosition(err.position()), err.getMessage(), err)));
    if (schema != null) {
      schema.validate(new TomlBackedConfiguration(toml, null)).forEach(errors::add);
    } else {
      schema = Schema.EMPTY;
    }

    this.toml = toml;
    this.schema = schema;
    this.errors = errors;
  }

  TomlBackedConfiguration(TomlTable toml, @Nullable Schema schema) {
    List<ConfigurationError> errors = new ArrayList<>();
    if (schema != null) {
      schema.validate(new TomlBackedConfiguration(toml, null)).forEach(errors::add);
    } else {
      schema = Schema.EMPTY;
    }

    this.toml = toml;
    this.schema = schema;
    this.errors = errors;
  }

  @Override
  public List<ConfigurationError> errors() {
    return errors;
  }

  @Override
  public void toToml(Appendable appendable) throws IOException {
    new TomlSerializer(this, schema).writeTo(appendable);
  }

  @Override
  public Set<String> keySet() {
    Set<String> keys = new HashSet<>();
    keys.addAll(toml.dottedKeySet());
    keys.addAll(schema.defaultsKeySet());
    return keys;
  }

  @Override
  public Set<String> keySet(String prefix) {
    Set<String> keys = new HashSet<>();
    keys.addAll(toml.dottedKeySet().stream().filter((key) -> key.startsWith(prefix)).collect(Collectors.toSet()));
    keys.addAll(schema.defaultsKeySet(prefix));
    return keys;
  }

  @Override
  public Configuration getConfigurationSection(String name) {
    TomlTable table = toml.getTableOrEmpty(name);
    return new TomlBackedConfiguration(table, schema.getSubSection(name));
  }

  @Override
  public Set<String> sections(String prefix) {
    Set<String> keys = new HashSet<>();
    TomlTable table = toml.getTable(prefix);
    if (table != null) {
      keys.addAll(table.keyPathSet().stream().map((key) -> key.get(0)).collect(Collectors.toSet()));
    }
    return keys;
  }

  @Override
  public boolean contains(String key) {
    return toml.contains(key) || schema.hasDefault(key);
  }

  @Nullable
  @Override
  public Object get(String key) {
    Object obj = toml.get(key);
    if (obj != null) {
      if (obj instanceof TomlArray) {
        return deepToList((TomlArray) obj);
      }
      if (obj instanceof TomlTable) {
        return deepToMap((TomlTable) obj);
      }
      return obj;
    }
    return schema.getDefault(key);
  }

  @Nullable
  @Override
  public DocumentPosition inputPositionOf(String key) {
    TomlPosition position = toml.inputPositionOf(key);
    if (position == null) {
      return null;
    }
    return documentPosition(position);
  }

  private DocumentPosition inputPositionOf(List<String> keyPath) {
    TomlPosition position = toml.inputPositionOf(keyPath);
    if (position == null) {
      return null;
    }
    return documentPosition(position);
  }

  @Override
  public String getString(String key) {
    String sectionKey = schema.getSubSectionPrefix(key);
    if (sectionKey == null) {
      return getValue(key, toml::getString, schema::getDefaultString);
    }
    Schema section = schema.getSubSection(sectionKey);
    String subSectionKey = key.substring(sectionKey.length() + 1);
    TomlTable table = toml.getTableOrEmpty(sectionKey);

    return getValue(subSectionKey, table::getString, section::getDefaultString);
  }

  @Override
  public int getInteger(String key) {
    String sectionKey = schema.getSubSectionPrefix(key);
    Schema section;
    TomlTable table;
    if (sectionKey != null) {
      section = schema.getSubSection(sectionKey);
      key = key.substring(sectionKey.length() + 1);
      table = toml.getTableOrEmpty(sectionKey);
    } else {
      section = schema;
      table = toml;
    }

    return getValue(key, keyPath -> {
      Long longValue = table.getLong(keyPath);
      if (longValue != null && (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE)) {
        throw new InvalidConfigurationPropertyTypeException(
            inputPositionOf(keyPath),
            "Value of property '" + joinKeyPath(keyPath) + "' is too large for an integer");
      }
      return (longValue != null) ? longValue.intValue() : null;
    }, section::getDefaultInteger);
  }

  @Override
  public long getLong(String key) {
    String sectionKey = schema.getSubSectionPrefix(key);
    if (sectionKey == null) {
      return getValue(key, toml::getLong, schema::getDefaultLong);
    }
    Schema section = schema.getSubSection(sectionKey);
    String subSectionKey = key.substring(sectionKey.length() + 1);
    TomlTable table = toml.getTableOrEmpty(sectionKey);

    return getValue(subSectionKey, table::getLong, section::getDefaultLong);
  }

  @Override
  public double getDouble(String key) {
    String sectionKey = schema.getSubSectionPrefix(key);
    if (sectionKey == null) {
      return getValue(key, toml::getDouble, schema::getDefaultDouble);
    }
    Schema section = schema.getSubSection(sectionKey);
    String subSectionKey = key.substring(sectionKey.length() + 1);
    TomlTable table = toml.getTableOrEmpty(sectionKey);

    return getValue(subSectionKey, table::getDouble, section::getDefaultDouble);
  }

  @Override
  public boolean getBoolean(String key) {
    String sectionKey = schema.getSubSectionPrefix(key);
    if (sectionKey == null) {
      return getValue(key, toml::getBoolean, schema::getDefaultBoolean);
    }
    Schema section = schema.getSubSection(sectionKey);
    String subSectionKey = key.substring(sectionKey.length() + 1);
    TomlTable table = toml.getTableOrEmpty(sectionKey);

    return getValue(subSectionKey, table::getBoolean, section::getDefaultBoolean);
  }

  @Override
  public Map<String, Object> getMap(String key) {
    String sectionKey = schema.getSubSectionPrefix(key);
    Schema section;
    TomlTable table;
    if (sectionKey != null) {
      section = schema.getSubSection(sectionKey);
      key = key.substring(sectionKey.length() + 1);
      table = toml.getTableOrEmpty(sectionKey);
    } else {
      section = schema;
      table = toml;
    }

    return getValue(key, keyPath -> {
      TomlTable map = table.getTable(keyPath);
      if (map == null) {
        return null;
      }
      return deepToMap(map);
    }, section::getDefaultMap);
  }

  @Override
  public List<Object> getList(String key) {
    String sectionKey = schema.getSubSectionPrefix(key);
    Schema section;
    TomlTable table;
    if (sectionKey != null) {
      section = schema.getSubSection(sectionKey);
      key = key.substring(sectionKey.length() + 1);
      table = toml.getTableOrEmpty(sectionKey);
    } else {
      section = schema;
      table = toml;
    }

    return getValue(key, keyPath -> {
      TomlArray array = table.getArray(keyPath);
      if (array == null) {
        return null;
      }
      return deepToList(array);
    }, section::getDefaultList);
  }

  @Override
  public List<String> getListOfString(String key) {
    return getList(key, "strings", TomlArray::containsStrings, (schema) -> schema::getDefaultListOfString);
  }

  @Override
  public List<Integer> getListOfInteger(String key) {
    return getValue(key, keyPath -> {
      TomlArray array = toml.getArray(keyPath);
      if (array == null) {
        return null;
      }
      if (!array.containsLongs()) {
        throw new InvalidConfigurationPropertyTypeException(
            inputPositionOf(keyPath),
            "List property '" + joinKeyPath(keyPath) + "' does not contain integers");
      }
      @SuppressWarnings("unchecked")
      List<Long> longList = (List<Long>) (List) array.toList();
      return IntStream.range(0, longList.size()).mapToObj(i -> {
        Long value = longList.get(i);
        if (value > Integer.MAX_VALUE) {
          throw new InvalidConfigurationPropertyTypeException(
              inputPositionOf(keyPath),
              "Value of property '" + joinKeyPath(keyPath) + "', index " + i + ", is too large for an integer");
        }
        return value.intValue();
      }).collect(Collectors.toList());
    }, schema::getDefaultListOfInteger);
  }

  @Override
  public List<Long> getListOfLong(String key) {
    return getList(key, "longs", TomlArray::containsLongs, schema -> schema::getDefaultListOfLong);
  }

  @Override
  public List<Double> getListOfDouble(String key) {
    return getList(key, "doubles", TomlArray::containsDoubles, schema -> schema::getDefaultListOfDouble);
  }

  @Override
  public List<Boolean> getListOfBoolean(String key) {
    return getList(key, "booleans", TomlArray::containsBooleans, schema -> schema::getDefaultListOfBoolean);
  }

  @Override
  public List<Map<String, Object>> getListOfMap(String key) {
    String sectionKey = schema.getSubSectionPrefix(key);
    Schema section;
    TomlTable table;
    if (sectionKey != null) {
      section = schema.getSubSection(sectionKey);
      key = key.substring(sectionKey.length() + 1);
      table = toml.getTableOrEmpty(sectionKey);
    } else {
      section = schema;
      table = toml;
    }

    return getValue(key, keyPath -> {
      TomlArray array = table.getArray(keyPath);
      if (array == null) {
        return null;
      }
      if (!array.containsTables()) {
        throw new InvalidConfigurationPropertyTypeException(
            inputPositionOf(keyPath),
            "List property '" + joinKeyPath(keyPath) + "' does not contain maps");
      }
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> typedList = (List<Map<String, Object>>) (List) deepToList(array);
      return typedList;
    }, section::getDefaultListOfMap);
  }

  private DocumentPosition documentPosition(TomlPosition position) {
    return DocumentPosition.positionAt(position.line(), position.column());
  }

  private <T> T getValue(String key, Function<List<String>, T> tomlGet, Function<String, T> defaultGet) {
    List<String> keyPath = parseDottedKey(key);
    T value;
    try {
      value = tomlGet.apply(keyPath);
    } catch (TomlInvalidTypeException e) {
      throw new InvalidConfigurationPropertyTypeException(inputPositionOf(keyPath), e.getMessage(), e);
    }
    if (value != null) {
      return value;
    }
    String canonicalKey = joinKeyPath(keyPath);
    value = defaultGet.apply(canonicalKey);
    if (value != null) {
      return value;
    }
    throw new NoConfigurationPropertyException("No value for property '" + canonicalKey + "'");
  }

  private <T> List<T> getList(
      String key,
      String typeName,
      Predicate<TomlArray> tomlCheck,
      Function<Schema, Function<String, List<T>>> defaultGet) {
    String sectionKey = schema.getSubSectionPrefix(key);
    Schema section;
    TomlTable table;
    if (sectionKey != null) {
      section = schema.getSubSection(sectionKey);
      key = key.substring(sectionKey.length() + 1);
      table = toml.getTableOrEmpty(sectionKey);
    } else {
      section = schema;
      table = toml;
    }
    Function<String, List<T>> defaultGetFn = defaultGet.apply(section);
    return getValue(key, keyPath -> {
      TomlArray array = table.getArray(keyPath);
      if (array == null) {
        return null;
      }
      if (!tomlCheck.test(array)) {
        throw new InvalidConfigurationPropertyTypeException(
            inputPositionOf(keyPath),
            "List property '" + joinKeyPath(keyPath) + "' does not contain " + typeName);
      }
      @SuppressWarnings("unchecked")
      List<T> typedList = (List<T>) array.toList();
      return typedList;
    }, defaultGetFn);
  }

  private static List<Object> deepToList(TomlArray array) {
    return array.toList().stream().map(o -> {
      if (o instanceof TomlArray) {
        return deepToList((TomlArray) o);
      }
      if (o instanceof TomlTable) {
        return deepToMap((TomlTable) o);
      }
      return o;
    }).collect(Collectors.toList());
  }

  private static Map<String, Object> deepToMap(TomlTable table) {
    return table.toMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
      Object o = e.getValue();
      if (o instanceof TomlArray) {
        return deepToList((TomlArray) o);
      }
      if (o instanceof TomlTable) {
        return deepToMap((TomlTable) o);
      }
      return o;
    }));
  }
}
