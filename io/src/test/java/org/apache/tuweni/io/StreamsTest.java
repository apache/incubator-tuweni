// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.io;

import static org.apache.tuweni.io.Streams.enumerationStream;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class StreamsTest {

  @Test
  void shouldStreamAnEnumeration() {
    Enumeration<String> enumeration = Collections.enumeration(Arrays.asList("RED", "BLUE", "GREEN"));
    List<String> result = enumerationStream(enumeration).map(String::toLowerCase).collect(Collectors.toList());
    assertEquals(Arrays.asList("red", "blue", "green"), result);
  }
}
