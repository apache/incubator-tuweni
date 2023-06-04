// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.tuweni.bytes.Bytes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AllocatedTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void destroyedValue() {
    Allocated allocated = Allocated.allocate(32);
    assertEquals(32, allocated.length());
    allocated.destroy();
    assertTrue(allocated.isDestroyed());
    assertThrows(IllegalStateException.class, () -> allocated.equals(Allocated.allocate(3)));
    assertThrows(IllegalStateException.class, () -> allocated.hashCode());
  }

  @Test
  void allocateBytes() {
    Allocated value = Allocated.fromBytes(Bytes.fromHexString("deadbeef"));
    assertEquals(Bytes.fromHexString("deadbeef"), value.bytes());
  }
}
