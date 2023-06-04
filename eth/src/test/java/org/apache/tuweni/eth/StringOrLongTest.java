// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StringOrLongTest {

  @Test
  void testEquals() {
    assertTrue(new StringOrLong("3").equals(new StringOrLong("3")));
    assertTrue(new StringOrLong(3L).equals(new StringOrLong(3L)));
    assertFalse(new StringOrLong(3L).equals(new StringOrLong("3")));
  }

  @Test
  void testToStringLong() {
    assertEquals("3", new StringOrLong(3L).toString());
  }

  @Test
  void testToStringString() {
    assertEquals("3", new StringOrLong("3").toString());
  }
}
