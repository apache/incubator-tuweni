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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.sodium.PasswordHash.Algorithm;
import org.apache.tuweni.crypto.sodium.PasswordHash.Salt;
import org.apache.tuweni.crypto.sodium.PasswordHash.VerificationResult;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PasswordHashTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void shouldGenerateSameKeyForSameParameters() {
    String password = "A very insecure password";
    Salt salt = Salt.random();

    Bytes hash = PasswordHash
        .hash(
            password,
            20,
            salt,
            PasswordHash.interactiveOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            Algorithm.recommended());
    assertEquals(20, hash.size());

    Bytes generated = PasswordHash
        .hash(
            password,
            20,
            salt,
            PasswordHash.interactiveOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            Algorithm.recommended());
    assertEquals(hash, generated);

    generated = PasswordHash
        .hash(
            password,
            20,
            Salt.random(),
            PasswordHash.interactiveOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            Algorithm.recommended());
    assertNotEquals(hash, generated);

    generated = PasswordHash
        .hash(
            password,
            20,
            salt,
            PasswordHash.moderateOpsLimit(),
            PasswordHash.interactiveMemLimit(),
            Algorithm.recommended());
    assertNotEquals(hash, generated);
  }

  @Test
  void shouldThrowForLowOpsLimitWithArgon2i() {
    assertThrows(IllegalArgumentException.class, () -> {
      PasswordHash
          .hash(
              "A very insecure password",
              20,
              Salt.random(),
              1,
              PasswordHash.moderateMemLimit(),
              Algorithm.argon2i13());
    });
  }

  @Test
  void checkHashAndVerify() {
    assumeTrue(Sodium.supportsVersion(Sodium.VERSION_10_0_14), "Requires sodium native library >= 10.0.14");
    String password = "A very insecure password";

    String hash = PasswordHash.hashInteractive(password);
    assertTrue(PasswordHash.verify(hash, password));
    VerificationResult result = PasswordHash.checkHashForInteractive(hash, password);
    assertEquals(VerificationResult.PASSED, result);
    assertTrue(result.passed());

    assertFalse(PasswordHash.verify(hash, "Bad password"));
    result = PasswordHash.checkHashForInteractive(hash, "Bad password");
    assertEquals(VerificationResult.FAILED, result);
    assertFalse(result.passed());
  }

  @Test
  void checkHashAndVerifyNeedingRehash() {
    assumeTrue(Sodium.supportsVersion(Sodium.VERSION_10_0_14), "Requires sodium native library >= 10.0.14");
    String password = "A very insecure password";
    String hash = PasswordHash.hashInteractive(password);
    assertTrue(PasswordHash.needsRehash(hash));
    VerificationResult result = PasswordHash.checkHash(hash, password);
    assertEquals(VerificationResult.NEEDS_REHASH, result);
    assertTrue(result.passed());
  }
}
