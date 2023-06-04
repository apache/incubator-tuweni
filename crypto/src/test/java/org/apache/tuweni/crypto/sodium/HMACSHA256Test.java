// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.tuweni.bytes.Bytes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HMACSHA256Test {
  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void testHmacsha256() {
    HMACSHA256.Key key = HMACSHA256.Key.random();
    Bytes authenticator = HMACSHA256.authenticate(Bytes.fromHexString("deadbeef"), key);
    assertTrue(HMACSHA256.verify(authenticator, Bytes.fromHexString("deadbeef"), key));
  }

  @Test
  void testHmacsha256InvalidAuthenticator() {
    HMACSHA256.Key key = HMACSHA256.Key.random();
    Bytes authenticator = HMACSHA256.authenticate(Bytes.fromHexString("deadbeef"), key);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            HMACSHA256.verify(
                Bytes.concatenate(authenticator, Bytes.of(1, 2, 3)),
                Bytes.fromHexString("deadbeef"),
                key));
  }

  @Test
  void testHmacsha512NoMatch() {
    HMACSHA256.Key key = HMACSHA256.Key.random();
    Bytes authenticator = HMACSHA256.authenticate(Bytes.fromHexString("deadbeef"), key);
    assertFalse(HMACSHA256.verify(authenticator.reverse(), Bytes.fromHexString("deadbeef"), key));
  }
}
