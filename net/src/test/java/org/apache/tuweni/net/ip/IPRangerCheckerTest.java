// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.net.ip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

class IPRangerCheckerTest {

  @Test
  void testAllowedAll() {
    IPRangeChecker checker = IPRangeChecker.allowAll();
    assertTrue(checker.check("127.0.0.1"));
    assertTrue(checker.check("192.168.0.1"));
    assertTrue(checker.check("10.0.10.1"));
  }

  @Test
  void testRejectRange() {
    IPRangeChecker checker =
        IPRangeChecker.create(
            Collections.singletonList("0.0.0.0/0"), Collections.singletonList("10.0.0.0/24"));
    assertTrue(checker.check("127.0.0.1"));
    assertTrue(checker.check("192.168.0.1"));
    assertFalse(checker.check("10.0.0.2"));
  }
}
