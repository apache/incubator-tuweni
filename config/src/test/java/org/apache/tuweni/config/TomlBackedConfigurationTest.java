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

import static org.apache.tuweni.config.ConfigurationErrors.noErrors;
import static org.apache.tuweni.config.ConfigurationErrors.singleError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class TomlBackedConfigurationTest {

  @Test
  void loadSimpleConfigFile() throws Exception {
    // @formatter:off
    Configuration config = Configuration.fromToml(
        "foo = \"12\"\n"
      + "bar = 13\n"
      + "foobar = 156.34\n"
      + "amaps = [{a = 1, b = 2}, {a = 'hello'}]\n"
      + "\n"
      + "[boo]\n"
      + "baz=[1,2,3]\n"
      + "\n"
      + "[amap]\n"
      + "a=1\n"
      + "b=2\n"
      + "[deepmap]\n"
      + "a=4\n"
      + "[deepmap.deeper]\n"
      + "b=5\n"
    );
    // @formatter:on
    assertFalse(config.hasErrors());
    assertEquals("12", config.getString("foo"));
    assertEquals(13, config.getInteger("bar"));
    assertEquals(13L, config.getLong("bar"));
    assertEquals(156.34d, config.getDouble("foobar"));
    assertEquals(Arrays.asList(1, 2, 3), config.getListOfInteger("boo.baz"));
    assertEquals(Arrays.asList(1L, 2L, 3L), config.getListOfLong("boo.baz"));
    assertEquals(Arrays.asList(1L, 2L, 3L), config.getList("boo.baz"));

    Map<String, Object> expectedMap = new HashMap<>();
    expectedMap.put("a", 1L);
    expectedMap.put("b", 2L);
    assertEquals(expectedMap, config.getMap("amap"));

    assertEquals(Collections.singletonMap("baz", Arrays.asList(1L, 2L, 3L)), config.getMap("boo"));

    Map<String, Object> expectedDeepMap = new HashMap<>();
    expectedDeepMap.put("a", 4L);
    expectedDeepMap.put("deeper", Collections.singletonMap("b", 5L));
    assertEquals(expectedDeepMap, config.getMap("deepmap"));

    List<Map<String, Object>> expectedList = Arrays.asList(expectedMap, Collections.singletonMap("a", "hello"));
    assertEquals(expectedList, config.getListOfMap("amaps"));
  }

  @Test
  void testKeyPresent() throws Exception {
    Configuration config = Configuration.fromToml("foo=\"12\"\nbar=\"13\"\n[baz]\nfoobar = 156.34");
    assertFalse(config.hasErrors());
    assertEquals(new HashSet<>(Arrays.asList("foo", "bar", "baz.foobar")), config.keySet());
    assertTrue(config.contains("foo"));
    assertTrue(config.contains("bar"));
    assertTrue(config.contains("baz.foobar"));
    assertFalse(config.contains("example"));
  }

  @Test
  void testFindsValuesBasedOnCanonicalKey() throws Exception {
    Configuration config = Configuration.fromToml("foo=12\nbar=\"13\"\n[baz]\nfoobar = 156.34");
    assertFalse(config.hasErrors());
    assertEquals(new HashSet<>(Arrays.asList("foo", "bar", "baz.foobar")), config.keySet());
    assertEquals(12, config.getInteger("foo"));
    assertEquals(12, config.getInteger("   foo    "));
    assertEquals(12, config.getInteger("   'foo'    "));
    assertEquals(12, config.getInteger("   \"foo\"    "));
    assertTrue(config.contains("baz.foobar"));
    assertTrue(config.contains("baz .   foobar"));
    assertTrue(config.contains("baz .   'foobar'"));
    assertTrue(config.contains("\"baz\" .   'foobar'"));
  }

  @Test
  void throwsForMissingValue() throws Exception {
    Configuration config = Configuration.fromToml("foo=12\nbar=\"13\"\n[baz]\nfoobar = 156.34");
    assertFalse(config.hasErrors());
    Exception e = assertThrows(NoConfigurationPropertyException.class, () -> config.getString("foo.blah"));
    assertEquals("No value for property 'foo.blah'", e.getMessage());
    e = assertThrows(NoConfigurationPropertyException.class, () -> config.getInteger("  foobaz  "));
    assertEquals("No value for property 'foobaz'", e.getMessage());
  }

  @Test
  void throwsForInvalidType() throws Exception {
    Configuration config = Configuration
        .fromToml(
            "[foo]\n" + "bar=99\n" + "baz='buz'\n" + "biz=false\n" + "buz = [1,2,3]\n" + "sbuz = ['1', '2', '3']\n");
    assertFalse(config.hasErrors());

    assertEquals(99, config.getInteger("foo.bar"));
    InvalidConfigurationPropertyTypeException e =
        assertThrows(InvalidConfigurationPropertyTypeException.class, () -> config.getString("foo.bar"));
    assertEquals("Value of 'foo.bar' is a integer", e.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(2, 1), e.position());

    assertEquals("buz", config.getString("foo.baz"));
    e = assertThrows(InvalidConfigurationPropertyTypeException.class, () -> config.getDouble("foo.baz"));
    assertEquals("Value of 'foo.baz' is a string", e.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(3, 1), e.position());

    assertFalse(config.getBoolean("foo.biz"));
    e = assertThrows(InvalidConfigurationPropertyTypeException.class, () -> config.getLong("foo.biz"));
    assertEquals("Value of 'foo.biz' is a boolean", e.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(4, 1), e.position());

    e = assertThrows(InvalidConfigurationPropertyTypeException.class, () -> config.getLong("foo"));
    assertEquals("Value of 'foo' is a table", e.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(1, 1), e.position());

    e = assertThrows(InvalidConfigurationPropertyTypeException.class, () -> config.getListOfString("foo.buz"));
    assertEquals("List property 'foo.buz' does not contain strings", e.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(5, 1), e.position());

    e = assertThrows(InvalidConfigurationPropertyTypeException.class, () -> config.getListOfInteger("foo.sbuz"));
    assertEquals("List property 'foo.sbuz' does not contain integers", e.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(6, 1), e.position());

    e = assertThrows(InvalidConfigurationPropertyTypeException.class, () -> config.getListOfMap("foo.sbuz"));
    assertEquals("List property 'foo.sbuz' does not contain maps", e.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(6, 1), e.position());
  }

  @Test
  void throwsForImpossibleIntegerConversion() throws Exception {
    // @formatter:off
    Configuration config = Configuration.fromToml(
        "[foo]\n"
      + "\" bar\" = "
      + (1L + Integer.MAX_VALUE)
      + "\n"
      + "buz = [1, 2, "
      + (1L + Integer.MAX_VALUE)
      + ", 4]\n"
    );
    // @formatter:on
    assertFalse(config.hasErrors());
    InvalidConfigurationPropertyTypeException e =
        assertThrows(InvalidConfigurationPropertyTypeException.class, () -> config.getInteger("foo.' bar'"));
    assertEquals("Value of property 'foo.\" bar\"' is too large for an integer", e.getMessageWithoutPosition());

    e = assertThrows(InvalidConfigurationPropertyTypeException.class, () -> config.getListOfInteger("foo.buz"));
    assertEquals("Value of property 'foo.buz', index 2, is too large for an integer", e.getMessageWithoutPosition());
  }

  @Test
  void loadMissingFile() {
    assertThrows(NoSuchFileException.class, () -> {
      Configuration.fromToml(Paths.get("FileThatDoesntExist"));
    });
  }

  @Test
  void invalidTOMLFile() throws Exception {
    Configuration config = Configuration.fromToml("foo=\"12\"\nfoobar = \"156.34");
    assertTrue(config.hasErrors());
    assertTrue(
        config
            .errors()
            .get(0)
            .getMessageWithoutPosition()
            .startsWith("Unexpected end of input, expected \" or a character"));
    assertEquals(DocumentPosition.positionAt(2, 17), config.errors().get(0).position());
    assertEquals("12", config.getString("foo"));
  }

  @Test
  void getDefaultValue() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addString("foo", "goodbye", null, null);
    Schema schema = builder.toSchema();
    Configuration config = Configuration.fromToml("foobar = 'hello'", schema);
    assertEquals("hello", config.getString("foobar"));
    assertTrue(config.contains("foo"));
    assertEquals("goodbye", config.getString("foo"));
  }

  @Test
  void keysContainSchemaKeys() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addString("foo", "bar", null, null);
    builder.addString("foo.bar", "buz", null, null);
    Schema schema = builder.toSchema();
    Configuration config = Configuration.fromToml("foobar = 'hello'", schema);
    assertEquals(new HashSet<>(Arrays.asList("foo", "foo.bar", "foobar")), config.keySet());
  }

  @Test
  void validateConfiguration() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.validateConfiguration(config -> {
      if (config.getInteger("expenses") > config.getInteger("revenue")) {
        return singleError("Expenses cannot be larger than revenue");
      }
      return noErrors();
    });

    Schema schema = builder.toSchema();
    Configuration config = Configuration.fromToml("expenses = 2000\nrevenue = 1500\n", schema);
    assertTrue(config.hasErrors());
    assertEquals("Expenses cannot be larger than revenue", config.errors().get(0).getMessage());
  }

  @Test
  void validateConfigurationProperty() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addString("foo", "foobar", null, (key, position, value) -> {
      if ("bar".equals(value)) {
        return singleError(position, "No bar allowed");
      }
      return noErrors();
    });

    Schema schema = builder.toSchema();
    Configuration config = Configuration.fromToml(" foo = \"bar\"", schema);
    assertTrue(config.hasErrors());
    ConfigurationError error = config.errors().get(0);
    assertEquals("No bar allowed", error.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(1, 2), error.position());
  }

  @Test
  void validateIntegerProperty() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addInteger("foo", null, null, (key, position, value) -> {
      throw new AssertionFailedError("should not be reached");
    });

    Schema schema = builder.toSchema();
    Configuration config = Configuration.fromToml(" foo = " + (1L + Integer.MAX_VALUE) + "\n", schema);
    assertTrue(config.hasErrors());
    ConfigurationError error = config.errors().get(0);
    assertEquals("Value of property 'foo' is too large for an integer", error.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(1, 2), error.position());
  }

  @Test
  void shouldValidateStringList() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addListOfString("foo", null, null, (key, position, value) -> {
      throw new AssertionFailedError("should not be reached");
    });

    Schema schema = builder.toSchema();
    Configuration config = Configuration.fromToml(" foo = [1, 2]\n", schema);
    assertTrue(config.hasErrors());
    ConfigurationError error = config.errors().get(0);
    assertEquals("Value of property 'foo', index 0, is not a string", error.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(1, 2), error.position());
  }

  @Test
  void shouldValidateIntegerList() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addListOfInteger("foo", null, null, (key, position, value) -> {
      throw new AssertionFailedError("should not be reached");
    });

    Schema schema = builder.toSchema();
    Configuration config = Configuration.fromToml(" foo = ['a', 'b']\n", schema);
    assertTrue(config.hasErrors());
    ConfigurationError error = config.errors().get(0);
    assertEquals("Value of property 'foo', index 0, is not an integer", error.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(1, 2), error.position());
  }

  @Test
  void shouldValidateLongListAsIntegers() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addListOfInteger("foo", null, null, (key, position, value) -> {
      assertEquals(Arrays.asList(1, 2, 3), value);
      return singleError("should reach here");
    });

    Schema schema = builder.toSchema();
    Configuration config = Configuration.fromToml(" foo = [1, 2, 3]\n", schema);
    assertTrue(config.hasErrors());
    ConfigurationError error = config.errors().get(0);
    assertEquals("should reach here", error.getMessage());
    assertNull(error.position());
  }

  @Test
  void shouldValidateWithinIntegerList() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addListOfInteger("foo", null, null, (key, position, value) -> {
      throw new AssertionFailedError("should not be reached");
    });

    Schema schema = builder.toSchema();
    Configuration config = Configuration.fromToml(" foo = [1, 2, " + (1L + Integer.MAX_VALUE) + ", 3]\n", schema);
    assertTrue(config.hasErrors());
    ConfigurationError error = config.errors().get(0);
    assertEquals("Value of property 'foo', index 2, is too large for an integer", error.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(1, 2), error.position());
  }

  @Test
  void shouldValidateLongList() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addListOfLong("foo", null, null, (key, position, value) -> {
      throw new AssertionFailedError("should not be reached");
    });

    Schema schema = builder.toSchema();
    Configuration config = Configuration.fromToml(" foo = ['a', 'b']\n", schema);
    assertTrue(config.hasErrors());
    ConfigurationError error = config.errors().get(0);
    assertEquals("Value of property 'foo', index 0, is not a long", error.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(1, 2), error.position());
  }

  @Test
  void shouldValidateWithIsPresent() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addString("foo.bar", null, null, PropertyValidator.isPresent());

    Schema schema = builder.toSchema();
    Configuration config = Configuration.fromToml(" foo = ['a', 'b']\n", schema);
    assertTrue(config.hasErrors());
    ConfigurationError error = config.errors().get(0);
    assertEquals("Required property 'foo.bar' is missing", error.getMessage());
    assertNull(error.position());
  }

  @Test
  void shouldValidateWithInRange() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addInteger("foo", null, null, PropertyValidator.inRange(0, 10));

    Schema schema = builder.toSchema();
    Configuration config = Configuration.fromToml(" foo = 15\n", schema);
    assertTrue(config.hasErrors());
    ConfigurationError error = config.errors().get(0);
    assertEquals("Value of property 'foo' is outside range [0,10)", error.getMessageWithoutPosition());
    assertEquals(DocumentPosition.positionAt(1, 2), error.position());

    Configuration config2 = Configuration.fromToml(" foo = 9\n", schema);
    assertFalse(config2.hasErrors());
  }

  @Test
  void validatorNotCalledWhenDefaultUsed() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addString("fooS", "hello", null, (key, position, value) -> {
      throw new AssertionFailedError("should not be reached");
    });
    builder.addInteger("fooI", 2, null, (key, position, value) -> {
      throw new AssertionFailedError("should not be reached");
    });
    builder.addLong("fooL", 2L, null, (key, position, value) -> {
      throw new AssertionFailedError("should not be reached");
    });
    builder.addListOfString("fooLS", Collections.emptyList(), null, (key, position, value) -> {
      throw new AssertionFailedError("should not be reached");
    });
    builder.addListOfInteger("fooLI", Collections.emptyList(), null, (key, position, value) -> {
      throw new AssertionFailedError("should not be reached");
    });
    builder.addListOfLong("fooLL", Collections.emptyList(), null, (key, position, value) -> {
      throw new AssertionFailedError("should not be reached");
    });

    Schema schema = builder.toSchema();
    Configuration config = Configuration.fromToml("\n", schema);
    assertFalse(config.hasErrors());
  }

  @Test
  void writeConfigurationToToml() throws Exception {
    // @formatter:off
    Configuration config = Configuration.fromToml(
      ("foo = \"12\"\n"
      + "bar = 13\n"
      + "foobar = 156.34\n"
      + "amaps = [{a = 1, b = 2}, {a = 'hello'}]\n"
      + "\n"
      + "[boo]\n"
      + "baz=[1,2,3]\n"
      + "\n"
      + "[amap]\n"
      + "a=1\n"
      + "b=2\n"
      + "[deepmap]\n"
      + "' a' = 4\n"
      + "[deepmap.' deep']\n"
      + "'' = 'emptykey'\n"
      + "[deepmap.deeper]\n"
      + "farewell='goodbye'\n").replace("\n", System.lineSeparator())
    );
    // @formatter:on

    // @formatter:off
    String expected =
      ("amaps = [{a = 1, b = 2}, {a = \"hello\"}]\n"
      + "bar = 13\n"
      + "foo = \"12\"\n"
      + "foobar = 156.34\n"
      + "\n"
      + "[amap]\n"
      + "a = 1\n"
      + "b = 2\n"
      + "\n"
      + "[boo]\n"
      + "baz = [1, 2, 3]\n"
      + "\n"
      + "[deepmap]\n"
      + "\" a\" = 4\n"
      + "\n"
      + "[deepmap.\" deep\"]\n"
      + "\"\" = \"emptykey\"\n"
      + "\n"
      + "[deepmap.deeper]\n"
      + "farewell = \"goodbye\"\n").replace("\n", System.lineSeparator());
    // @formatter:on

    assertEquals(expected, config.toToml());
  }


  @Test
  void writeConfigurationWithDocumentationToToml() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.documentProperty("deepmap", "This table will have sub-tables");
    builder.documentProperty("boo.baz", "A list of longs");
    builder.documentProperty("bar", "Lucky number 13");
    Schema schema = builder.toSchema();

    // @formatter:off
    Configuration config = Configuration.fromToml(
      ("foo = \"12\"\n"
      + "bar = 13\n"
      + "foobar = 156.34\n"
      + "amaps = [{a = 1, b = 2}, {a = 'hello'}]\n"
      + "\n"
      + "[boo]\n"
      + "baz=[1,2,3]\n"
      + "\n"
      + "[amap]\n"
      + "a=1\n"
      + "b=2\n"
      + "[deepmap]\n"
      + "' a' = 4\n"
      + "[deepmap.' deep']\n"
      + "'' = 'emptykey'\n"
      + "[deepmap.deeper]\n"
      + "farewell='goodbye'\n").replace("\n", System.lineSeparator()), schema);
    // @formatter:on

    // @formatter:off
    String expected =
      ("amaps = [{a = 1, b = 2}, {a = \"hello\"}]\n"
      + "## Lucky number 13\n"
      + "bar = 13\n"
      + "foo = \"12\"\n"
      + "foobar = 156.34\n"
      + "\n"
      + "[amap]\n"
      + "a = 1\n"
      + "b = 2\n"
      + "\n"
      + "[boo]\n"
      + "## A list of longs\n"
      + "baz = [1, 2, 3]\n"
      + "\n"
      + "## This table will have sub-tables\n"
      + "[deepmap]\n"
      + "\" a\" = 4\n"
      + "\n"
      + "[deepmap.\" deep\"]\n"
      + "\"\" = \"emptykey\"\n"
      + "\n"
      + "[deepmap.deeper]\n"
      + "farewell = \"goodbye\"\n").replace("\n", System.lineSeparator());
    // @formatter:on
    assertEquals(expected, config.toToml());
  }

  @Test
  void writeConfigurationToTomlIncludesDefaults() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addString("farewell", "goodbye", "a farewell", null);
    builder.addInteger("xxx", 10, "documentation\n  over multiple lines!", null);
    builder.addInteger("zzz", 10, null, null);
    Schema schema = builder.toSchema();

    // @formatter:off
    Configuration config = Configuration.fromToml(
        "foo = \"12\"\n"
      + "foobar = 156.34\n"
      + "xxx = 10\n"
      + "zzz = 5\n"
      + "\n"
      + "[boo]\n"
      + "baz=[1,2,3]\n"
      + "\n", schema);
    // @formatter:on

    // @formatter:off
    String expected =
        "## a farewell\n"
      + "#farewell = \"goodbye\"\n"
      + "foo = \"12\"\n"
      + "foobar = 156.34\n"
      + "## documentation\n"
      + "##   over multiple lines!\n"
      + "#xxx = 10\n"
      + "#zzz = 10\n"
      + "zzz = 5\n"
      + "\n"
      + "[boo]\n"
      + "baz = [1, 2, 3]\n";
    // @formatter:on
    assertEquals(expected.replace("\n", System.lineSeparator()), config.toToml());
  }

  @Test
  void buildSchemaAndDumpToToml() throws Exception {
    SchemaBuilder builder = SchemaBuilder.create();
    builder.addString("somekey", "somevalue", "Got milk", null);
    builder.addBoolean("foo", false, "Toggle switch", null);
    builder.addDouble("bar", 1.0, "Value of currency", null);
    builder.addLong("'Here now'", 123L, "One two three", null);
    builder.addString("'No defaults'", null, null, null);
    Configuration config = Configuration.fromToml("", builder.toSchema());
    // @formatter:off
    String expected =
        "## One two three\n"
      + "#\"Here now\" = 123\n"
      + "## Value of currency\n"
      + "#bar = 1.0\n"
      + "## Toggle switch\n"
      + "#foo = false\n"
      + "## Got milk\n"
      + "#somekey = \"somevalue\"\n";
    // @formatter:on
    assertEquals(expected.replace("\n", System.lineSeparator()), config.toToml());
  }
}
