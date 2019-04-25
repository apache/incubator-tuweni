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
package org.apache.tuweni.progpow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.units.bigints.UInt32;

import org.junit.jupiter.api.Test;

class KISS99RandomTest {

  @Test
  void testRounds() {
    KISS99Random random = new KISS99Random(
        UInt32.valueOf(362436069),
        UInt32.valueOf(521288629),
        UInt32.valueOf(123456789),
        UInt32.valueOf(380116160));
    assertEquals(UInt32.valueOf(769445856), random.generate());
    assertEquals(UInt32.valueOf(742012328), random.generate());
    assertEquals(UInt32.valueOf(2121196314), random.generate());
    assertEquals(UInt32.fromHexString("0xa73a60ce"), random.generate());
    for (int i = 0; i < 99995; i++) {
      random.generate();
    }
    assertEquals(UInt32.valueOf(941074834), random.generate());
  }
}
