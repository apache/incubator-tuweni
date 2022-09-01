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

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

/**
 * A schema for a configuration, providing default values and validation rules.
 */
public final class Schema {

  static final Schema EMPTY = new Schema(
      Collections.emptyMap(),
      Collections.emptyMap(),
      Collections.emptyMap(),
      Collections.emptyList(),
      Collections.emptyMap());

  private final Map<String, String> propertyDescriptions;
  private final Map<String, Object> propertyDefaults;
  private final Map<String, PropertyValidator<Object>> propertyValidators;
  private final List<ConfigurationValidator> configurationValidators;
  private final Map<String, Schema> subSections;

  Schema(
      Map<String, String> propertyDescriptions,
      Map<String, Object> propertyDefaults,
      Map<String, PropertyValidator<Object>> propertyValidators,
      List<ConfigurationValidator> configurationValidators,
      Map<String, Schema> subSections) {
    this.propertyDescriptions = propertyDescriptions;
    this.propertyDefaults = propertyDefaults;
    this.propertyValidators = propertyValidators;
    this.configurationValidators = configurationValidators;
    this.subSections = subSections;
  }

  /**
   * The keys of all defaults provided by this schema.
   *
   * @return The keys for all defaults provided by this schema.
   */
  public Set<String> defaultsKeySet() {
    return propertyDefaults.keySet();
  }

  /**
   * The keys of all defaults provided by this schema.
   *
   * @param prefix the prefix the keys must match.
   * @return The keys for all defaults provided by this schema.
   */
  public Set<String> defaultsKeySet(String prefix) {
    return propertyDefaults.keySet().stream().filter((key) -> key.startsWith(prefix)).collect(Collectors.toSet());
  }

  /**
   * Get the description for a key.
   *
   * @param key A configuration key (e.g. {@code "server.address.hostname"}).
   * @return A description associated with the key, or null if no description is available.
   */
  @Nullable
  public String description(String key) {
    requireNonNull(key);
    return propertyDescriptions.get(key);
  }

  /**
   * Check if a key has a default provided by this schema.
   *
   * @param key A configuration key (e.g. {@code "server.address.hostname"}).
   * @return {@code true} if this schema provides a default value for the key.
   */
  public boolean hasDefault(String key) {
    requireNonNull(key);
    return propertyDefaults.containsKey(key);
  }

  /**
   * Get a default value from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.address.hostname"}).
   * @return The value, or null if no default was available.
   */
  public Object getDefault(String key) {
    requireNonNull(key);
    return propertyDefaults.get(key);
  }

  /**
   * Get a default value from this configuration as a string.
   *
   * @param key A configuration key (e.g. {@code "server.address.hostname"}).
   * @return The value, or null if no default was available.
   * @throws InvalidConfigurationPropertyTypeException If the default value is not a string.
   */
  @Nullable
  public String getDefaultString(String key) {
    requireNonNull(key);
    return getTypedDefault(key, String.class, "string");
  }

  /**
   * Get a default value from this configuration as a integer.
   *
   * @param key A configuration key (e.g. {@code "server.address.port"}).
   * @return The value, or null if no default was available.
   * @throws InvalidConfigurationPropertyTypeException If the default value is not an integer.
   */
  @Nullable
  public Integer getDefaultInteger(String key) {
    requireNonNull(key);
    Object obj = propertyDefaults.get(key);
    if (obj == null) {
      return null;
    }
    if (obj instanceof Integer) {
      return (Integer) obj;
    }
    if (obj instanceof Long) {
      Long longValue = (Long) obj;
      if (longValue > Integer.MAX_VALUE) {
        throw new InvalidConfigurationPropertyTypeException(
            null,
            "Value of property '" + key + "' is too large for an integer");
      }
      return longValue.intValue();
    }
    throw new InvalidConfigurationPropertyTypeException(null, "Property at '" + key + "' was not an integer");
  }

  /**
   * Get a default value from this configuration as a long.
   *
   * @param key A configuration key (e.g. {@code "server.address.port"}).
   * @return The value, or null if no default was available.
   * @throws InvalidConfigurationPropertyTypeException If the default value is not a long.
   */
  @Nullable
  public Long getDefaultLong(String key) {
    requireNonNull(key);
    Object obj = propertyDefaults.get(key);
    if (obj == null) {
      return null;
    }
    if (obj instanceof Long) {
      return (Long) obj;
    }
    if (obj instanceof Integer) {
      return ((Integer) obj).longValue();
    }
    throw new InvalidConfigurationPropertyTypeException(null, "Property at '" + key + "' was not a long");
  }

  /**
   * Get a default value from this configuration as a double.
   *
   * @param key A configuration key (e.g. {@code "server.priority"}).
   * @return The value, or null if no default was available.
   * @throws InvalidConfigurationPropertyTypeException If the default value is not a double.
   */
  @Nullable
  public Double getDefaultDouble(String key) {
    requireNonNull(key);
    return getTypedDefault(key, Double.class, "double");
  }

  /**
   * Get a default value from this configuration as a boolean.
   *
   * @param key A configuration key (e.g. {@code "server.active"}).
   * @return The value, or null if no default was available.
   * @throws InvalidConfigurationPropertyTypeException If the default value is not a boolean.
   */
  @Nullable
  public Boolean getDefaultBoolean(String key) {
    requireNonNull(key);
    return getTypedDefault(key, Boolean.class, "boolean");
  }

  /**
   * Get a default value from this configuration as a map.
   *
   * @param key A configuration key (e.g. {@code "server.active"}).
   * @return The value, or null if no default was available.
   * @throws InvalidConfigurationPropertyTypeException If the default value is not a map.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public Map<String, Object> getDefaultMap(String key) {
    requireNonNull(key);
    return getTypedDefault(key, Map.class, "map");
  }

  /**
   * Get a default value from this configuration as a list.
   *
   * @param key A configuration key (e.g. {@code "server.common_names"}).
   * @return The value, or null if no default was available.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the default value is not a list.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public List<Object> getDefaultList(String key) {
    requireNonNull(key);
    Object obj = propertyDefaults.get(key);
    if (obj == null) {
      return null;
    }
    if (obj instanceof List) {
      return (List<Object>) obj;
    }
    throw new InvalidConfigurationPropertyTypeException(null, "Property at '" + key + "' is not a list");
  }

  /**
   * Get a default value from this configuration as a list of strings.
   *
   * @param key A configuration key (e.g. {@code "server.common_names"}).
   * @return The value, or null if no default was available.
   * @throws InvalidConfigurationPropertyTypeException If the default value is not a list of strings.
   */
  @Nullable
  public List<String> getDefaultListOfString(String key) {
    requireNonNull(key);
    return getListDefault(key, String.class, "strings");
  }

  /**
   * Get a default value from this configuration as a list of integers.
   *
   * @param key A configuration key (e.g. {@code "server.address.ports"}).
   * @return The value, or null if no default was available.
   * @throws InvalidConfigurationPropertyTypeException If the default value is not a list of integers.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public List<Integer> getDefaultListOfInteger(String key) {
    requireNonNull(key);
    Object obj = propertyDefaults.get(key);
    if (obj == null) {
      return null;
    }
    if (obj instanceof List) {
      List<?> list = (List<?>) obj;
      if (list.isEmpty() || list.get(0) instanceof Integer) {
        return (List<Integer>) list;
      }
      if (list.get(0) instanceof Long) {
        return IntStream.range(0, list.size()).mapToObj(i -> {
          Long longValue = (Long) list.get(i);
          if (longValue == null) {
            return null;
          }
          if (longValue > Integer.MAX_VALUE) {
            throw new InvalidConfigurationPropertyTypeException(
                null,
                "Value of property '" + key + "', index " + i + ", is too large for an integer");
          }
          return longValue.intValue();
        }).collect(Collectors.toList());
      }
    }
    throw new InvalidConfigurationPropertyTypeException(null, "Property at '" + key + "' was not a list of integers");
  }

  /**
   * Get a default value from this configuration as a list of longs.
   *
   * @param key A configuration key (e.g. {@code "server.address.ports"}).
   * @return The value, or null if no default was available.
   * @throws InvalidConfigurationPropertyTypeException If the default value is not a list of longs.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public List<Long> getDefaultListOfLong(String key) {
    requireNonNull(key);
    Object obj = propertyDefaults.get(key);
    if (obj == null) {
      return null;
    }
    if (obj instanceof List) {
      List<?> list = (List<?>) obj;
      if (list.isEmpty() || list.get(0) instanceof Long) {
        return (List<Long>) list;
      }
      if (list.get(0) instanceof Integer) {
        return ((List<Integer>) list).stream().map(i -> {
          if (i == null) {
            return null;
          }
          return i.longValue();
        }).collect(Collectors.toList());
      }
    }
    throw new InvalidConfigurationPropertyTypeException(null, "Property at '" + key + "' was not a list of longs");
  }

  /**
   * Get a default value from this configuration as a list of doubles.
   *
   * @param key A configuration key (e.g. {@code "server.priorities"}).
   * @return The value, or null if no default was available.
   * @throws InvalidConfigurationPropertyTypeException If the default value is not a list of doubles.
   */
  @Nullable
  public List<Double> getDefaultListOfDouble(String key) {
    requireNonNull(key);
    return getListDefault(key, Double.class, "doubles");
  }

  /**
   * Get a default value from this configuration as a list of booleans.
   *
   * @param key A configuration key (e.g. {@code "server.flags"}).
   * @return The value, or null if no default was available.
   * @throws InvalidConfigurationPropertyTypeException If the default value is not a list of booleans.
   */
  @Nullable
  public List<Boolean> getDefaultListOfBoolean(String key) {
    requireNonNull(key);
    return getListDefault(key, Boolean.class, "booleans");
  }

  /**
   * Get a default value from this configuration as a list of maps.
   *
   * @param key A configuration key (e.g. {@code "mainnet.servers"}).
   * @return The value, or null if no default was available.
   * @throws InvalidConfigurationPropertyTypeException If the default value is not a list of maps.
   */
  @Nullable
  public List<Map<String, Object>> getDefaultListOfMap(String key) {
    requireNonNull(key);
    return getListDefault(key, Map.class, "maps");
  }

  @Nullable
  private <T> T getTypedDefault(String key, Class<T> clazz, String typeName) {
    Object obj = propertyDefaults.get(key);
    if (obj == null) {
      return null;
    }
    if (clazz.isInstance(obj)) {
      return clazz.cast(obj);
    }
    throw new InvalidConfigurationPropertyTypeException(null, "Property at '" + key + "' was not a " + typeName);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private <T> List<T> getListDefault(String key, Class<?> listType, String typeName) {
    Object obj = propertyDefaults.get(key);
    if (obj == null) {
      return null;
    }
    if (obj instanceof List) {
      List<?> list = (List<?>) obj;
      if (list.isEmpty() || listType.isInstance(list.get(0))) {
        return (List<T>) list;
      }
    }
    throw new InvalidConfigurationPropertyTypeException(
        null,
        "Property at '" + key + "' was not a list of " + typeName);
  }

  /**
   * Validate a configuration against this schema.
   *
   * <p>
   * The validations are done incrementally as the stream is consumed. Use {@code .limit(...)} on the stream to control
   * the maximum number of validation errors to receive.
   *
   * @param configuration The configuration to validate.
   * @return A stream containing any errors encountered during validation.
   */
  public Stream<ConfigurationError> validate(Configuration configuration) {
    requireNonNull(configuration);

    Stream<ConfigurationError> propertyErrors = propertyValidators.entrySet().stream().flatMap(e -> {
      String key = e.getKey();
      PropertyValidator<Object> validator = e.getValue();
      Object value = configuration.get(key);
      DocumentPosition position = configuration.inputPositionOf(key);
      List<ConfigurationError> errors = validator.validate(key, position, value);
      if (errors == null) {
        return Stream.empty();
      }
      return errors.stream();
    });

    Stream<ConfigurationError> configErrors = configurationValidators.stream().flatMap(v -> {
      List<ConfigurationError> errors = v.validate(configuration);
      if (errors == null) {
        return Stream.empty();
      }
      return errors.stream();
    });

    return Stream.concat(propertyErrors, configErrors);
  }

  public String getSubSectionPrefix(String key) {
    Schema schema = subSections.get(key);
    if (schema != null) {
      return key;
    }
    if (key.contains(".")) {
      return getSubSectionPrefix(key.substring(0, key.lastIndexOf(".")));
    }
    return null;
  }

  public Schema getSubSection(String name) {
    Schema schema = subSections.get(name);
    if (schema != null) {
      return schema;
    }
    if (name.contains(".")) {
      return getSubSection(name.substring(0, name.lastIndexOf(".")));
    }
    return this;
  }
}
