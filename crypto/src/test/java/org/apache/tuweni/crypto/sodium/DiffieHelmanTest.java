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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DiffieHelmanTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void testScalarMultiplication() {
    DiffieHelman.KeyPair keyPair = DiffieHelman.KeyPair.random();
    DiffieHelman.KeyPair secondKeyPair = DiffieHelman.KeyPair.random();

    DiffieHelman.Secret scalar1 = DiffieHelman.Secret.forKeys(keyPair.secretKey(), secondKeyPair.publicKey());
    DiffieHelman.Secret scalar2 = DiffieHelman.Secret.forKeys(secondKeyPair.secretKey(), keyPair.publicKey());

    assertEquals(scalar1, scalar2);
  }

  @Test
  void testEquals() {
    DiffieHelman.KeyPair keyPair = DiffieHelman.KeyPair.random();
    DiffieHelman.KeyPair keyPair2 = DiffieHelman.KeyPair.forSecretKey(keyPair.secretKey());
    assertEquals(keyPair, keyPair2);
    assertEquals(keyPair.hashCode(), keyPair2.hashCode());
  }

  @Test
  void testEqualsSecretKey() {
    DiffieHelman.KeyPair keyPair = DiffieHelman.KeyPair.random();
    DiffieHelman.KeyPair keyPair2 = DiffieHelman.KeyPair.forSecretKey(keyPair.secretKey());
    assertEquals(keyPair.secretKey(), keyPair2.secretKey());
    assertEquals(keyPair.hashCode(), keyPair2.hashCode());
  }

  @Test
  void testEqualsPublicKey() {
    DiffieHelman.KeyPair keyPair = DiffieHelman.KeyPair.random();
    DiffieHelman.KeyPair keyPair2 = DiffieHelman.KeyPair.forSecretKey(keyPair.secretKey());
    assertEquals(keyPair.publicKey(), keyPair2.publicKey());
    assertEquals(keyPair.hashCode(), keyPair2.hashCode());
  }
}
