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
import static org.apache.tuweni.toml.Toml.canonicalDottedKey;

import org.apache.tuweni.toml.Toml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

/**
 * Represents collection of configuration properties, optionally validated against a schema.
 */
public interface Configuration {

  /**
   * Read a configuration from a TOML-formatted string.
   *
   * @param toml A TOML-formatted string.
   * @return A Configuration loaded from the TOML file.
   */
  static Configuration fromToml(String toml) {
    return fromToml(toml, null);
  }

  /**
   * Read a configuration from a TOML-formatted string, associated with a validation schema.
   *
   * @param toml A TOML-formatted string.
   * @param schema The validation schema for the configuration.
   * @return A Configuration loaded from the TOML file.
   */
  static Configuration fromToml(String toml, @Nullable Schema schema) {
    requireNonNull(toml);
    return new TomlBackedConfiguration(Toml.parse(toml), schema);
  }

  /**
   * Loads a configuration from a TOML-formatted file.
   *
   * @param file The path of the TOML-formatted configuration file.
   * @return A Configuration loaded from the TOML file.
   * @throws NoSuchFileException If the file could not be found.
   * @throws IOException If an IO error occurs.
   */
  static Configuration fromToml(Path file) throws IOException {
    return fromToml(file, null);
  }

  /**
   * Loads a configuration from a file, associated with a validation schema.
   *
   * @param file The path of the TOML-formatted configuration file.
   * @param schema The validation schema for the configuration.
   * @return A Configuration loaded from the TOML file.
   * @throws NoSuchFileException If the file could not be found.
   * @throws IOException If an IO error occurs.
   */
  static Configuration fromToml(Path file, @Nullable Schema schema) throws IOException {
    requireNonNull(file);
    return new TomlBackedConfiguration(Toml.parse(file), schema);
  }

  /**
   * Loads a configuration from a TOML-formatted file.
   *
   * @param is An input stream providing TOML-formatted configuration.
   * @return A Configuration loaded from the TOML file.
   * @throws IOException If an IO error occurs.
   */
  static Configuration fromToml(InputStream is) throws IOException {
    return fromToml(is, null);
  }

  /**
   * Loads a configuration from a file, associated with a validation schema.
   *
   * @param is An input stream providing TOML-formatted configuration.
   * @param schema The validation schema for the configuration.
   * @return A Configuration loaded from the TOML file.
   * @throws IOException If an IO error occurs.
   */
  static Configuration fromToml(InputStream is, @Nullable Schema schema) throws IOException {
    requireNonNull(is);
    return new TomlBackedConfiguration(Toml.parse(is), schema);
  }

  /**
   * Get an empty configuration, with no values.
   *
   * @return An empty configuration, with no values.
   */
  static Configuration empty() {
    return EmptyConfiguration.EMPTY;
  }

  /**
   * Get an empty configuration, associated with a validation schema.
   *
   * @param schema The validation schema for the configuration.
   * @return An empty configuration.
   */
  static Configuration empty(@Nullable Schema schema) {
    return new EmptyConfiguration(schema);
  }

  /**
   * Returns true if the document has errors.
   * 
   * @return {@code true} if the TOML document contained errors.
   */
  default boolean hasErrors() {
    return !(errors().isEmpty());
  }

  /**
   * The errors that occurred during parsing.
   *
   * @return A list of errors.
   */
  List<ConfigurationError> errors();

  /**
   * Get a TOML-formatted representation of this configuration.
   *
   * @return A TOML-formatted representation.
   */
  default String toToml() {
    StringBuilder builder = new StringBuilder();
    try {
      toToml(builder);
    } catch (IOException e) {
      // Not reachable
      throw new UncheckedIOException(e);
    }
    return builder.toString();
  }

  /**
   * Save a configuration to a TOML-formatted file.
   *
   * <p>
   * If necessary, parent directories for the output file will be created.
   *
   * @param path The file path to write the TOML-formatted output to.
   * @throws IOException If the file cannot be written.
   */
  default void toToml(Path path) throws IOException {
    Files.createDirectories(path.getParent());
    try (Writer writer = Files.newBufferedWriter(path)) {
      toToml(writer);
    }
  }

  /**
   * Writes a configuration in TOML format.
   *
   * @param appendable The output to write to.
   * @throws IOException If the file cannot be written.
   */
  void toToml(Appendable appendable) throws IOException;

  /**
   * The keys of all entries present in this configuration.
   *
   * @return The keys of all entries in this configuration.
   */
  Set<String> keySet();

  /**
   * The keys of all entries present in this configuration under a given prefix.
   *
   * @param prefix The prefix for all the matching keys
   * @return The keys of all entries in this configuration under a given prefix.
   */
  Set<String> keySet(String prefix);

  /**
   * The names of the sections defined under a given prefix.
   *
   * @param prefix the key prefix
   * @return The names of the sections defined under a given prefix.
   */
  Set<String> sections(String prefix);

  /**
   * Provides a section of the configuration
   *
   * @param name the name of the section
   * @return the section of the configuration.
   */
  Configuration getConfigurationSection(String name);


  /**
   * Check if a key is set in this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.address.hostname"}).
   * @return {@code true} if the entry is present in this configuration.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  boolean contains(String key);

  /**
   * Get an object from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.address.hostname"}).
   * @return The value, or {@code null} if no value was set in the configuration.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  @Nullable
  Object get(String key);

  /**
   * Get the position where a key is defined in the TOML document.
   *
   * @param key A configuration key (e.g. {@code "server.address.port"}).
   * @return The input position, or {@code null} if the key was not set in this configuration.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  @Nullable
  DocumentPosition inputPositionOf(String key);

  /**
   * Get a string from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.address.hostname"}).
   * @return The value.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the value is not a string.
   * @throws NoConfigurationPropertyException If the key was not set in the configuration.
   */
  String getString(String key);

  /**
   * Get an integer from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.address.port"}).
   * @return The value.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the value is not an integer.
   * @throws NoConfigurationPropertyException If the key was not set in the configuration.
   */
  int getInteger(String key);

  /**
   * Get a long from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.address.port"}).
   * @return The value.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the value is not a long.
   * @throws NoConfigurationPropertyException If the key was not set in the configuration.
   */
  long getLong(String key);

  /**
   * Get a double from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.priority"}).
   * @return The value.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the value is not a double.
   * @throws NoConfigurationPropertyException If the key was not set in the configuration.
   */
  double getDouble(String key);

  /**
   * Get a boolean from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.active"}).
   * @return The value.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the value is not a boolean.
   * @throws NoConfigurationPropertyException If the key was not set in the configuration.
   */
  boolean getBoolean(String key);

  /**
   * Get a map from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.active"}).
   * @return The value.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the value is not a map.
   * @throws NoConfigurationPropertyException If the key was not set in the configuration.
   */
  Map<String, Object> getMap(String key);

  /**
   * Get a list from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.common_names"}).
   * @return The value.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the value is not a list.
   * @throws NoConfigurationPropertyException If the key was not set in the configuration.
   */
  List<Object> getList(String key);

  /**
   * Get a list of strings from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.common_names"}).
   * @return The value.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the value is not a list of strings.
   * @throws NoConfigurationPropertyException If the key was not set in the configuration.
   */
  List<String> getListOfString(String key);

  /**
   * Get a list of integers from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.address.ports"}).
   * @return The value.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the value is not a list of integers.
   * @throws NoConfigurationPropertyException If the key was not set in the configuration.
   */
  List<Integer> getListOfInteger(String key);

  /**
   * Get a list of longs from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.address.ports"}).
   * @return The value.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the value is not a list of longs.
   * @throws NoConfigurationPropertyException If the key was not set in the configuration.
   */
  List<Long> getListOfLong(String key);

  /**
   * Get a list of doubles from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.priorities"}).
   * @return The value.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the value is not a list of doubles.
   * @throws NoConfigurationPropertyException If the key was not set in the configuration.
   */
  List<Double> getListOfDouble(String key);

  /**
   * Get a list of booleans from this configuration.
   *
   * @param key A configuration key (e.g. {@code "server.flags"}).
   * @return The value.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the value is not a list of booleans.
   * @throws NoConfigurationPropertyException If the key was not set in the configuration.
   */
  List<Boolean> getListOfBoolean(String key);

  /**
   * Get a list of maps from this configuration.
   *
   * @param key A configuration key (e.g. {@code "mainnet.servers"}).
   * @return The value.
   * @throws IllegalArgumentException If the key cannot be parsed.
   * @throws InvalidConfigurationPropertyTypeException If the value is not a list of maps.
   * @throws NoConfigurationPropertyException If the key was not set in the configuration.
   */
  List<Map<String, Object>> getListOfMap(String key);

  /**
   * Get the canonical form of a configuration key.
   *
   * @param key A configuration key (e.g. {@code "server.flags"}).
   * @return The canonical form of the key.
   * @throws IllegalArgumentException If the key cannot be parsed.
   */
  static String canonicalKey(String key) {
    return canonicalDottedKey(key);
  }
}
