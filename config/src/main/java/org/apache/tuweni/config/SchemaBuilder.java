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
import static org.apache.tuweni.config.Configuration.canonicalKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

/**
 * This interface allows customers to determine a schema to associate with a configuration to validate the entries read
 * from configuration files, and provide default values if no value is present in the configuration file.
 */
public final class SchemaBuilder {

  private final Map<String, String> propertyDescriptions = new HashMap<>();
  private final Map<String, Object> propertyDefaults = new HashMap<>();
  private final Map<String, PropertyValidator<Object>> propertyValidators = new HashMap<>();
  private final List<ConfigurationValidator> configurationValidators = new ArrayList<>();
  private final Map<String, Schema> subSections = new HashMap<>();

  /**
   * Get a new builder for a schema.
   *
   * @return A new {@link SchemaBuilder}.
   */
  public static SchemaBuilder create() {
    return new SchemaBuilder();
  }

  SchemaBuilder() {}

  /**
   * Provide documentation for a property.
   *
   * <p>
   * Invoking this method with the same key as a previous invocation will replace the description for that key.
   *
   * @param key The configuration property key.
   * @param description The description to associate with the property.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  public SchemaBuilder documentProperty(String key, String description) {
    requireNonNull(key);
    requireNonNull(description);
    propertyDescriptions.put(canonicalKey(key), description);
    return this;
  }

  /**
   * Provide a default value for a property.
   *
   * <p>
   * Invoking this method with the same key as a previous invocation will replace the default value for that key.
   *
   * @param key The configuration property key.
   * @param value The default value for the property.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  public SchemaBuilder addDefault(String key, Object value) {
    requireNonNull(key);
    requireNonNull(value);
    propertyDefaults.put(canonicalKey(key), value);
    return this;
  }

  /**
   * Add a property validation to this schema.
   *
   * <p>
   * Multiple validators can be provided for the same key by invoking this method multiple times.
   *
   * @param key The configuration property key.
   * @param validator A validator for the property.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  public SchemaBuilder validateProperty(String key, PropertyValidator<Object> validator) {
    requireNonNull(key);
    requireNonNull(validator);
    propertyValidators.put(canonicalKey(key), validator);
    return this;
  }

  /**
   * Add a configuration validator to the schema.
   *
   * <p>
   * Multiple validators can be provided by invoking this method multiple times.
   *
   * @param validator A configuration validator.
   * @return This builder.
   */
  public SchemaBuilder validateConfiguration(ConfigurationValidator validator) {
    requireNonNull(validator);
    configurationValidators.add(validator);
    return this;
  }

  /**
   * Add a string property to the schema.
   *
   * <p>
   * Even if no {@code validator} is provided, the schema will validate that the configuration property, if present,
   * contains a string.
   *
   * <p>
   * If a {@code defaultValue} is provided, then the provided validator, if any, will only be invoked if the value is
   * present (i.e. it will not be provided a {@code null} value to validate).
   *
   * @param key The configuration property key.
   * @param defaultValue A default value for the property or null if no default is provided.
   * @param description The description to associate with this property, or null if no documentation is provided.
   * @param validator A validator for the property, or null if no validator is provided.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  public SchemaBuilder addString(
      String key,
      @Nullable String defaultValue,
      @Nullable String description,
      @Nullable PropertyValidator<? super String> validator) {
    requireNonNull(key);
    return addScalar(String.class, "string", key, defaultValue, description, validator);
  }

  /**
   * Add an integer property to the schema.
   *
   * <p>
   * Even if no {@code validator} is provided, the schema will validate that the configuration property, if present,
   * contains an integer.
   *
   * <p>
   * If a {@code defaultValue} is provided, then the provided validator, if any, will only be invoked if the value is
   * present (i.e. it will not be provided a {@code null} value to validate).
   *
   * @param key The configuration property key.
   * @param defaultValue A default value for the property or null if no default is provided.
   * @param description The description to associate with this property, or null if no documentation is provided.
   * @param validator A validator for the property, or null if no validator is provided.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  public SchemaBuilder addInteger(
      String key,
      @Nullable Integer defaultValue,
      @Nullable String description,
      @Nullable PropertyValidator<? super Integer> validator) {
    requireNonNull(key);
    if (defaultValue != null) {
      addDefault(key, defaultValue);
    }
    if (description != null) {
      documentProperty(key, description);
    }
    validateProperty(key, (canonicalKey, position, value) -> {
      if (!(value == null || value instanceof Integer || value instanceof Long)) {
        return Collections
            .singletonList(new ConfigurationError(position, "Property at '" + canonicalKey + "' requires an integer"));
      }
      if (validator == null || (defaultValue != null && value == null)) {
        return Collections.emptyList();
      }
      Integer intValue;
      if (value instanceof Long) {
        if (((Long) value) > Integer.MAX_VALUE) {
          return Collections
              .singletonList(
                  new ConfigurationError(
                      position,
                      "Value of property '" + canonicalKey + "' is too large for an integer"));
        }
        intValue = ((Long) value).intValue();
      } else {
        intValue = (Integer) value;
      }
      return validator.validate(canonicalKey, position, intValue);
    });
    return this;
  }

  /**
   * Add a long property to the schema.
   *
   * <p>
   * Even if no {@code validator} is provided, the schema will validate that the configuration property, if present,
   * contains a long.
   *
   * <p>
   * If a {@code defaultValue} is provided, then the provided validator, if any, will only be invoked if the value is
   * present (i.e. it will not be provided a {@code null} value to validate).
   *
   * @param key The configuration property key.
   * @param defaultValue A default value for the property or null if no default is provided.
   * @param description The description to associate with this property, or null if no documentation is provided.
   * @param validator A validator for the property, or null if no validator is provided.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  public SchemaBuilder addLong(
      String key,
      @Nullable Long defaultValue,
      @Nullable String description,
      @Nullable PropertyValidator<? super Long> validator) {
    requireNonNull(key);
    if (defaultValue != null) {
      addDefault(key, defaultValue);
    }
    if (description != null) {
      documentProperty(key, description);
    }
    validateProperty(key, (vkey, position, value) -> {
      if (!(value == null || value instanceof Long || value instanceof Integer)) {
        return Collections
            .singletonList(new ConfigurationError(position, "Property at '" + vkey + "' requires a long"));
      }
      if (validator == null || (defaultValue != null && value == null)) {
        return Collections.emptyList();
      }
      Long longValue;
      if (value instanceof Integer) {
        longValue = ((Integer) value).longValue();
      } else {
        longValue = (Long) value;
      }
      return validator.validate(vkey, position, longValue);
    });
    return this;
  }

  /**
   * Add a double property to the schema.
   *
   * <p>
   * Even if no {@code validator} is provided, the schema will validate that the configuration property, if present,
   * contains a double.
   *
   * <p>
   * If a {@code defaultValue} is provided, then the provided validator, if any, will only be invoked if the value is
   * present (i.e. it will not be provided a {@code null} value to validate).
   *
   * @param key The configuration property key.
   * @param defaultValue A default value for the property or null if no default is provided.
   * @param description The description to associate with this property, or null if no documentation is provided.
   * @param validator A validator for the property, or null if no validator is provided.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  public SchemaBuilder addDouble(
      String key,
      @Nullable Double defaultValue,
      @Nullable String description,
      @Nullable PropertyValidator<? super Double> validator) {
    requireNonNull(key);
    return addScalar(Double.class, "double", key, defaultValue, description, validator);
  }

  /**
   * Add a boolean property to the schema.
   *
   * <p>
   * Even if no {@code validator} is provided, the schema will validate that the configuration property, if present,
   * contains a boolean.
   *
   * <p>
   * If a {@code defaultValue} is provided, then the provided validator, if any, will only be invoked if the value is
   * present (i.e. it will not be provided a {@code null} value to validate).
   *
   * @param key The configuration property key.
   * @param defaultValue A default value for the property or null if no default is provided.
   * @param description The description to associate with this property, or null if no documentation is provided.
   * @param validator A validator for the property, or null if no validator is provided.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  public SchemaBuilder addBoolean(
      String key,
      @Nullable Boolean defaultValue,
      @Nullable String description,
      @Nullable PropertyValidator<? super Boolean> validator) {
    requireNonNull(key);
    return addScalar(Boolean.class, "boolean", key, defaultValue, description, validator);
  }

  /**
   * Add a list-of-strings property to the schema.
   *
   * <p>
   * Even if no {@code validator} is provided, the schema will validate that the configuration property, if present,
   * contains a list of strings without any null values.
   *
   * <p>
   * If a {@code defaultValue} is provided, then the provided validator, if any, will only be invoked if the value is
   * present (i.e. it will not be provided a {@code null} value to validate).
   *
   * @param key The configuration property key.
   * @param defaultValue A default value for the property or null if no default is provided.
   * @param description The description to associate with this property, or null if no documentation is provided.
   * @param validator A validator for the property, or null if no validator is provided.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  public SchemaBuilder addListOfString(
      String key,
      @Nullable List<String> defaultValue,
      @Nullable String description,
      @Nullable PropertyValidator<? super List<String>> validator) {
    requireNonNull(key);
    return addList(String.class, "string", key, defaultValue, description, validator);
  }

  /**
   * Add a list-of-integers property to the schema.
   *
   * <p>
   * Even if no {@code validator} is provided, the schema will validate that the configuration property, if present,
   * contains a list of integers without any null values.
   *
   * <p>
   * If a {@code defaultValue} is provided, then the provided validator, if any, will only be invoked if the value is
   * present (i.e. it will not be provided a {@code null} value to validate).
   *
   * @param key The configuration property key.
   * @param defaultValue A default value for the property or null if no default is provided.
   * @param description The description to associate with this property, or null if no documentation is provided.
   * @param validator A validator for the property, or null if no validator is provided.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  @SuppressWarnings("unchecked")
  public SchemaBuilder addListOfInteger(
      String key,
      @Nullable List<Integer> defaultValue,
      @Nullable String description,
      @Nullable PropertyValidator<? super List<Integer>> validator) {
    requireNonNull(key);

    if (defaultValue != null) {
      if (defaultValue.stream().anyMatch(Objects::isNull)) {
        throw new IllegalArgumentException("default value list contains null value(s)");
      }
      addDefault(key, defaultValue);
    }

    if (description != null) {
      documentProperty(key, description);
    }

    validateProperty(key, (vkey, position, value) -> {
      if (value != null && !(value instanceof List)) {
        return Collections
            .singletonList(new ConfigurationError(position, "Property at '" + vkey + "' requires a list of integers"));
      }

      boolean containsLong = false;
      if (value != null) {
        List<Object> objs = (List<Object>) value;
        for (int i = 0; i < objs.size(); ++i) {
          Object obj = objs.get(i);
          if (obj == null) {
            return Collections
                .singletonList(
                    new ConfigurationError(position, "Value of property '" + vkey + "', index " + i + ", is null"));
          }
          if (!(obj instanceof Integer) && !(obj instanceof Long)) {
            return Collections
                .singletonList(
                    new ConfigurationError(
                        position,
                        "Value of property '" + vkey + "', index " + i + ", is not an integer"));
          }
          if (obj instanceof Long) {
            containsLong = true;
            if (((Long) obj) > Integer.MAX_VALUE) {
              return Collections
                  .singletonList(
                      new ConfigurationError(
                          position,
                          "Value of property '" + vkey + "', index " + i + ", is too large for an integer"));
            }
          }
        }
      }

      if (validator == null || (defaultValue != null && value == null)) {
        return Collections.emptyList();
      }

      if (!containsLong) {
        return validator.validate(vkey, position, (List<Integer>) value);
      } else {
        return validator.validate(vkey, position, ((List<Object>) value).stream().map(o -> {
          if (o instanceof Integer) {
            return (Integer) o;
          }
          assert (o instanceof Long);
          return ((Long) o).intValue();
        }).collect(Collectors.toList()));
      }
    });

    return this;
  }

  /**
   * Add a list-of-longs property to the schema.
   *
   * <p>
   * Even if no {@code validator} is provided, the schema will validate that the configuration property, if present,
   * contains a list of longs without any null values.
   *
   * <p>
   * If a {@code defaultValue} is provided, then the provided validator, if any, will only be invoked if the value is
   * present (i.e. it will not be provided a {@code null} value to validate).
   *
   * @param key The configuration property key.
   * @param defaultValue A default value for the property or null if no default is provided.
   * @param description The description to associate with this property, or null if no documentation is provided.
   * @param validator A validator for the property, or null if no validator is provided.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  @SuppressWarnings("unchecked")
  public SchemaBuilder addListOfLong(
      String key,
      @Nullable List<Long> defaultValue,
      @Nullable String description,
      @Nullable PropertyValidator<? super List<Long>> validator) {
    requireNonNull(key);

    if (defaultValue != null) {
      if (defaultValue.stream().anyMatch(Objects::isNull)) {
        throw new IllegalArgumentException("default value list contains null value(s)");
      }
      addDefault(key, defaultValue);
    }

    if (description != null) {
      documentProperty(key, description);
    }

    validateProperty(key, (canonicalKey, position, value) -> {
      if (value != null && !(value instanceof List)) {
        return Collections
            .singletonList(
                new ConfigurationError(position, "Property at '" + canonicalKey + "' requires a list of longs"));
      }

      boolean containsInteger = false;
      if (value != null) {
        List<Object> objs = (List<Object>) value;
        for (int i = 0; i < objs.size(); ++i) {
          Object obj = objs.get(i);
          if (obj == null) {
            return Collections
                .singletonList(
                    new ConfigurationError(
                        position,
                        "Value of property '" + canonicalKey + "', index " + i + ", is null"));
          }

          if (!(obj instanceof Long) && !(obj instanceof Integer)) {
            return Collections
                .singletonList(
                    new ConfigurationError(
                        position,
                        "Value of property '" + canonicalKey + "', index " + i + ", is not a long"));
          }
          containsInteger |= (obj instanceof Integer);
        }
      }

      if (validator == null || (defaultValue != null && value == null)) {
        return Collections.emptyList();
      }

      if (!containsInteger) {
        return validator.validate(canonicalKey, position, (List<Long>) value);
      } else {
        return validator.validate(canonicalKey, position, ((List<Object>) value).stream().map(o -> {
          if (o instanceof Long) {
            return (Long) o;
          }
          assert (o instanceof Integer);
          return ((Integer) o).longValue();
        }).collect(Collectors.toList()));
      }
    });

    return this;
  }

  /**
   * Add a list-of-doubles property to the schema.
   *
   * <p>
   * Even if no {@code validator} is provided, the schema will validate that the configuration property, if present,
   * contains a list of doubles without any null values.
   *
   * <p>
   * If a {@code defaultValue} is provided, then the provided validator, if any, will only be invoked if the value is
   * present (i.e. it will not be provided a {@code null} value to validate).
   *
   * @param key The configuration property key.
   * @param defaultValue A default value for the property or null if no default is provided.
   * @param description The description to associate with this property, or null if no documentation is provided.
   * @param validator A validator for the property, or null if no validator is provided.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  public SchemaBuilder addListOfDouble(
      String key,
      @Nullable List<Double> defaultValue,
      @Nullable String description,
      @Nullable PropertyValidator<? super List<Double>> validator) {
    requireNonNull(key);
    return addList(Double.class, "double", key, defaultValue, description, validator);
  }

  /**
   * Add a list-of-booleans property to the schema.
   *
   * <p>
   * Even if no {@code validator} is provided, the schema will validate that the configuration property, if present,
   * contains a list of booleans without any null values.
   *
   * <p>
   * If a {@code defaultValue} is provided, then the provided validator, if any, will only be invoked if the value is
   * present (i.e. it will not be provided a {@code null} value to validate).
   *
   * @param key The configuration property key.
   * @param defaultValue A default value for the property or null if no default is provided.
   * @param description The description to associate with this property, or null if no documentation is provided.
   * @param validator A validator for the property, or null if no validator is provided.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  public SchemaBuilder addListOfBoolean(
      String key,
      @Nullable List<Boolean> defaultValue,
      @Nullable String description,
      @Nullable PropertyValidator<? super List<Boolean>> validator) {
    requireNonNull(key);
    return addList(Boolean.class, "boolean", key, defaultValue, description, validator);
  }

  /**
   * Add a list-of-maps property to the schema.
   *
   * <p>
   * Even if no {@code validator} is provided, the schema will validate that the configuration property, if present,
   * contains a list of maps without any null values.
   *
   * <p>
   * If a {@code defaultValue} is provided, then the provided validator, if any, will only be invoked if the value is
   * present (i.e. it will not be provided a {@code null} value to validate).
   *
   * @param key The configuration property key.
   * @param defaultValue A default value for the property or null if no default is provided.
   * @param description The description to associate with this property, or null if no documentation is provided.
   * @param validator A validator for the property, or null if no validator is provided.
   * @return This builder.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  public SchemaBuilder addListOfMap(
      String key,
      @Nullable List<Map<String, Object>> defaultValue,
      @Nullable String description,
      @Nullable PropertyValidator<? super List<Map<String, Object>>> validator) {
    requireNonNull(key);
    return addList(Map.class, "map", key, defaultValue, description, validator);
  }

  /**
   * Return the {@link Schema} constructed by this builder.
   *
   * @return A {@link Schema}.
   */
  public Schema toSchema() {
    return new Schema(propertyDescriptions, propertyDefaults, propertyValidators, configurationValidators, subSections);
  }

  private <T> SchemaBuilder addScalar(
      Class<T> clazz,
      String typeName,
      String key,
      @Nullable T defaultValue,
      @Nullable String description,
      @Nullable PropertyValidator<? super T> validator) {
    if (defaultValue != null) {
      addDefault(key, defaultValue);
    }
    if (description != null) {
      documentProperty(key, description);
    }
    validateProperty(key, (canonicalKey, position, value) -> {
      if (value != null && !clazz.isInstance(value)) {
        return Collections
            .singletonList(
                new ConfigurationError(position, "Property at '" + canonicalKey + "' requires a " + typeName));
      }
      if (validator == null || (defaultValue != null && value == null)) {
        return Collections.emptyList();
      }
      return validator.validate(canonicalKey, position, clazz.cast(value));
    });
    return this;
  }

  @SuppressWarnings("unchecked")
  private <T, U> SchemaBuilder addList(
      Class<U> innerClass,
      String typeName,
      String key,
      @Nullable List<T> defaultValue,
      @Nullable String description,
      @Nullable PropertyValidator<? super List<T>> validator) {

    if (defaultValue != null) {
      if (defaultValue.stream().anyMatch(Objects::isNull)) {
        throw new IllegalArgumentException("default value list contains null value(s)");
      }
      addDefault(key, defaultValue);
    }

    if (description != null) {
      documentProperty(key, description);
    }

    validateProperty(key, (canonicalKey, position, value) -> {
      if (value != null && !(value instanceof List)) {
        return Collections
            .singletonList(
                new ConfigurationError(
                    position,
                    "Property at '" + canonicalKey + "' requires a list of " + typeName + "s"));
      }

      if (value != null) {
        List<Object> objs = (List<Object>) value;
        for (int i = 0; i < objs.size(); ++i) {
          Object obj = objs.get(i);
          if (obj == null) {
            return Collections
                .singletonList(
                    new ConfigurationError(
                        position,
                        "Value of property '" + canonicalKey + "', index " + i + ", is null"));
          }
          if (!innerClass.isInstance(obj)) {
            return Collections
                .singletonList(
                    new ConfigurationError(
                        position,
                        "Value of property '" + canonicalKey + "', index " + i + ", is not a " + typeName));
          }
        }
      }

      if (validator == null || (defaultValue != null && value == null)) {
        return Collections.emptyList();
      }
      return validator.validate(canonicalKey, position, (List<T>) value);
    });

    return this;
  }

  public SchemaBuilder addSection(String key, Schema section) {
    subSections.put(key, section);
    return this;
  }
}
