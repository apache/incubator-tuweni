// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
    GenericHash.Hash output =
        GenericHash.hash(
            64,
            GenericHash.Input.fromBytes(Bytes.random(384)),
            GenericHash.Key.fromBytes(Bytes.random(32)));
    assertNotNull(output);
    assertEquals(64, output.bytes().size());
  }
}
