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
package org.apache.tuweni.crypto.sodium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.tuweni.bytes.Bytes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GenericHashTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void hashValue() {
    GenericHash.Hash output = GenericHash.hash(64, GenericHash.Input.fromBytes(Bytes.random(384)));
    assertNotNull(output);
    assertEquals(64, output.bytes().size());
  }

  @Test
  void hashWithKeyValue() {
    GenericHash.Hash output = GenericHash
        .hash(64, GenericHash.Input.fromBytes(Bytes.random(384)), GenericHash.Key.fromBytes(Bytes.random(32)));
    assertNotNull(output);
    assertEquals(64, output.bytes().size());
  }
}
