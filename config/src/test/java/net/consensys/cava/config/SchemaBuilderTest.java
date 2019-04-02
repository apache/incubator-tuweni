/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.config;

import static net.consensys.cava.config.ConfigurationErrors.noErrors;
import static net.consensys.cava.config.ConfigurationErrors.singleError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class SchemaBuilderTest {

  @Test
  void shouldThrowForDefaultListContainingNulls() {
    SchemaBuilder schemaBuilder = new SchemaBuilder();
    assertThrows(
        IllegalArgumentException.class,
        () -> schemaBuilder.addListOfString("strings", Arrays.asList("a", null, "b"), null, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> schemaBuilder.addListOfInteger("ints", Arrays.asList(null, 1, 2), null, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> schemaBuilder.addListOfLong("longs", Arrays.asList(1L, 2L, null), null, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> schemaBuilder.addListOfDouble("doubles", Arrays.asList(1.0, 2.0, 3.0, null), null, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> schemaBuilder.addListOfBoolean("bools", Arrays.asList(true, null, false), null, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> schemaBuilder
            .addListOfMap("maps", Arrays.asList(Collections.emptyMap(), null, Collections.emptyMap()), null, null));
  }

  @Test
  void validateListOfStrings() {
    SchemaBuilder schemaBuilder = new SchemaBuilder();
    schemaBuilder.addListOfString("key", Collections.emptyList(), "Some description here", (key, pos, value) -> {
      if (value.size() == 2 && value.get(0).startsWith("no")) {
        return singleError("This won't work out");
      }
      return noErrors();
    });
    Configuration config = Configuration.fromToml("key=[\"no\",\"yes\"]", schemaBuilder.toSchema());

    assertEquals(1, config.errors().size());
  }

  @Test
  void validateListOfMaps() {
    SchemaBuilder schemaBuilder = new SchemaBuilder();
    schemaBuilder.addListOfMap("key", Collections.emptyList(), "Some description here", (key, pos, value) -> {
      if (value.size() == 2 && value.get(0).size() == 1 && (Long) value.get(0).get("a") == 1L) {
        return singleError("This won't work out");
      }
      return noErrors();
    });
    Configuration config = Configuration.fromToml("key=[{a = 1},{a = 2}]", schemaBuilder.toSchema());

    assertEquals(1, config.errors().size());
  }
}
