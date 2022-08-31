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

import static org.apache.tuweni.config.Configuration.canonicalKey;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

final class EmptyConfiguration implements Configuration {

  static final Configuration EMPTY = new EmptyConfiguration(null);

  private final Schema schema;
  private final List<ConfigurationError> errors;

  EmptyConfiguration(@Nullable Schema schema) {
    if (schema != null) {
      this.schema = schema;
      this.errors = schema.validate(EMPTY).collect(Collectors.toList());
    } else {
      this.schema = Schema.EMPTY;
      this.errors = Collections.emptyList();
    }
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
    return schema.defaultsKeySet();
  }

  @Override
  public Set<String> keySet(String prefix) {
    return schema.defaultsKeySet(prefix);
  }

  @Override
  public Set<String> sections(String prefix) {
    return Collections.emptySet();
  }

  @Override
  public Configuration getConfigurationSection(String name) {
    return Configuration.empty();
  }

  @Override
  public boolean contains(String key) {
    return schema.hasDefault(key);
  }

  @Nullable
  @Override
  public Object get(String key) {
    return schema.getDefault(canonicalKey(key));
  }

  @Nullable
  @Override
  public DocumentPosition inputPositionOf(String key) {
    return null;
  }

  @Override
  public String getString(String key) {
    return getValue(key, schema::getDefaultString);
  }

  @Override
  public int getInteger(String key) {
    return getValue(key, schema::getDefaultInteger);
  }

  @Override
  public long getLong(String key) {
    return getValue(key, schema::getDefaultLong);
  }

  @Override
  public double getDouble(String key) {
    return getValue(key, schema::getDefaultDouble);
  }

  @Override
  public boolean getBoolean(String key) {
    return getValue(key, schema::getDefaultBoolean);
  }

  @Override
  public Map<String, Object> getMap(String key) {
    return getValue(key, schema::getDefaultMap);
  }

  @Override
  public List<Object> getList(String key) {
    return getValue(key, schema::getDefaultList);
  }

  @Override
  public List<String> getListOfString(String key) {
    return getValue(key, schema::getDefaultListOfString);
  }

  @Override
  public List<Integer> getListOfInteger(String key) {
    return getValue(key, schema::getDefaultListOfInteger);
  }

  @Override
  public List<Long> getListOfLong(String key) {
    return getValue(key, schema::getDefaultListOfLong);
  }

  @Override
  public List<Double> getListOfDouble(String key) {
    return getValue(key, schema::getDefaultListOfDouble);
  }

  @Override
  public List<Boolean> getListOfBoolean(String key) {
    return getValue(key, schema::getDefaultListOfBoolean);
  }

  @Override
  public List<Map<String, Object>> getListOfMap(String key) {
    return getValue(key, schema::getDefaultListOfMap);
  }

  private <T> T getValue(String key, Function<String, T> defaultGet) {
    String canonicalKey = canonicalKey(key);
    T value = defaultGet.apply(canonicalKey);
    if (value != null) {
      return value;
    }
    throw new NoConfigurationPropertyException("No value for property '" + canonicalKey + "'");
  }
}
