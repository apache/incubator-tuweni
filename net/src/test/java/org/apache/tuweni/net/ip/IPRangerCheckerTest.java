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
        IPRangeChecker.create(Collections.singletonList("0.0.0.0/0"), Collections.singletonList("10.0.0.0/24"));
    assertTrue(checker.check("127.0.0.1"));
    assertTrue(checker.check("192.168.0.1"));
    assertFalse(checker.check("10.0.0.2"));
  }
}
