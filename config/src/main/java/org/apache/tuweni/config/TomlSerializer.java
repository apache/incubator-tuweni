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

import org.apache.tuweni.toml.Toml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class TomlSerializer {

  private static final String LINE_SPLITTER = "\\r?\\n";

  private final Configuration configuration;
  private final Schema schema;
  // tableKey -> configKey
  private final Map<String, List<String>> tableMap;
  // configKey -> leafName
  private final Map<String, String> keyMap;

  TomlSerializer(Configuration configuration, Schema schema) {
    Map<String, List<String>> tableMap = new HashMap<>();
    Map<String, String> keyMap = new HashMap<>();

    configuration.keySet().forEach(configKey -> {
      List<String> keyPath;
      try {
        keyPath = Toml.parseDottedKey(configKey);
      } catch (IllegalArgumentException e) {
        throw new IllegalStateException("Configuration key '" + configKey + "' is not valid in TOML");
      }
      String tableKey = Toml.joinKeyPath(keyPath.subList(0, keyPath.size() - 1));
      List<String> keys = tableMap.get(tableKey);
      if (keys == null) {
        keys = new ArrayList<>();
        tableMap.put(tableKey, keys);
      }
      keys.add(configKey);
      keyMap.put(configKey, keyPath.get(keyPath.size() - 1));
    });

    this.configuration = configuration;
    this.schema = schema;
    this.tableMap = tableMap;
    this.keyMap = keyMap;
  }

  void writeTo(Appendable appendable) throws IOException {
    List<String> tableKeys = tableMap.keySet().stream().sorted().collect(Collectors.toList());
    for (Iterator<String> iterator = tableKeys.iterator(); iterator.hasNext();) {
      String tableKey = iterator.next();
      if (!tableKey.isEmpty()) {
        writeDocumentation(tableKey, appendable);
      }
      if (!tableKey.isEmpty()) {
        appendable.append('[');
        appendable.append(tableKey);
        appendable.append(']');
        appendable.append(System.lineSeparator());
      }

      List<String> configKeys = tableMap.get(tableKey);
      configKeys.sort(Comparator.naturalOrder());
      for (String configKey : configKeys) {
        String leafName = keyMap.get(configKey);
        Object obj = configuration.get(configKey);
        if (obj == null) {
          throw new IllegalStateException("Configuration key '" + configKey + "' was unexpectedly null");
        }
        Object defaultValue = schema.getDefault(configKey);
        if (obj instanceof Integer) {
          obj = ((Integer) obj).longValue();
        }
        if (defaultValue instanceof Integer) {
          defaultValue = ((Integer) defaultValue).longValue();
        }

        writeDocumentation(configKey, appendable);

        String leafKey = Toml.joinKeyPath(Collections.singletonList(leafName));
        if (defaultValue != null) {
          appendable.append('#');
          appendable.append(leafKey);
          appendable.append(" = ");
          writeValue(defaultValue, appendable);
          appendable.append(System.lineSeparator());
        }
        if (!obj.equals(defaultValue)) {
          appendable.append(leafKey);
          appendable.append(" = ");
          writeValue(obj, appendable);
          appendable.append(System.lineSeparator());
        }
      }

      if (iterator.hasNext()) {
        appendable.append(System.lineSeparator());
      }
    }
  }

  private void writeValue(Object obj, Appendable appendable) throws IOException {
    if (obj instanceof String) {
      appendable.append('\"');
      appendable.append(Toml.tomlEscape((String) obj));
      appendable.append('\"');
    } else if (obj instanceof List) {
      @SuppressWarnings("unchecked")
      List<Object> list = (List<Object>) obj;
      writeList(list, appendable);
    } else if (obj instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) obj;
      writeMap(map, appendable);
    } else {
      appendable.append(obj.toString());
    }
  }

  private void writeList(List<Object> list, Appendable appendable) throws IOException {
    appendable.append('[');
    for (Iterator<Object> iterator = list.iterator(); iterator.hasNext();) {
      Object obj = iterator.next();
      if (obj == null) {
        throw new IllegalStateException("Unexpected null in list property");
      }
      writeValue(obj, appendable);
      if (iterator.hasNext()) {
        appendable.append(", ");
      }
    }
    appendable.append(']');
  }

  private void writeMap(Map<String, Object> map, Appendable appendable) throws IOException {
    appendable.append('{');
    for (Iterator<String> iterator = map.keySet().stream().sorted().iterator(); iterator.hasNext();) {
      String key = iterator.next();
      Object obj = map.get(key);
      if (obj == null) {
        throw new IllegalStateException("Unexpected null in map property");
      }

      appendable.append(Toml.joinKeyPath(Collections.singletonList(key)));
      appendable.append(" = ");
      writeValue(obj, appendable);
      if (iterator.hasNext()) {
        appendable.append(", ");
      }
    }
    appendable.append('}');
  }

  private void writeDocumentation(String key, Appendable appendable) throws IOException {
    String description = schema.description(canonicalKey(key));
    if (description == null) {
      return;
    }

    for (String line : description.split(LINE_SPLITTER, -1)) {
      appendable.append("## ");
      appendable.append(line);
      appendable.append(System.lineSeparator());
    }
  }
}
