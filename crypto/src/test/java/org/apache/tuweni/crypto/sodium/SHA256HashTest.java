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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.Hash;
import org.apache.tuweni.junit.BouncyCastleExtension;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class SHA256HashTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void hashValue() {
    SHA256Hash.Hash output = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes.random(384)));
    assertNotNull(output);
    assertEquals(32, output.bytes().size());
    assertFalse(output.isDestroyed());
    output.destroy();
    assertTrue(output.isDestroyed());
  }

  @Test
  void inputValueEquals() {
    SHA256Hash.Input input = SHA256Hash.Input.fromBytes(Bytes.random(384));
    assertEquals(input, input);
    assertEquals(input.hashCode(), input.hashCode());
    assertEquals(input, SHA256Hash.Input.fromBytes(input.bytes()));
    assertEquals(input.hashCode(), SHA256Hash.Input.fromBytes(input.bytes()).hashCode());
    assertFalse(input.isDestroyed());
    input.destroy();
    assertTrue(input.isDestroyed());
  }

  @Test
  void outputEquals() {
    SHA256Hash.Input input = SHA256Hash.Input.fromBytes(Bytes.random(384));
    SHA256Hash.Hash output = SHA256Hash.hash(input);
    assertEquals(output, output);
    assertEquals(output.hashCode(), output.hashCode());
    assertEquals(output, SHA256Hash.hash(input));
    assertEquals(output.hashCode(), SHA256Hash.hash(input).hashCode());
  }

  @Test
  void testCompat() {
    Bytes toHash = Bytes.random(384);
    SHA256Hash.Input input = SHA256Hash.Input.fromBytes(toHash);
    SHA256Hash.Hash output = SHA256Hash.hash(input);
    assertEquals(Hash.sha2_256(toHash), output.bytes());
  }
}
