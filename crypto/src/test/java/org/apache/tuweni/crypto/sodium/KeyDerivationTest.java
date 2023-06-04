// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.sodium.KeyDerivation.MasterKey;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class KeyDerivationTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
    assumeTrue(
        KeyDerivation.isAvailable(), "KeyDerivation support is not available (requires >= 10.0.12");
  }

  @Test
  void differentIdsShouldGenerateDifferentKeys() {
    MasterKey masterKey = MasterKey.random();

    Bytes subKey1 = masterKey.deriveKey(40, 1, "abcdefg");
    assertEquals(subKey1, masterKey.deriveKey(40, 1, "abcdefg"));

    assertNotEquals(subKey1, masterKey.deriveKey(40, 2, "abcdefg"));
    assertNotEquals(subKey1, masterKey.deriveKey(40, 1, new byte[KeyDerivation.contextLength()]));
  }
}
