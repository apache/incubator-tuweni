// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class KeyExchangeTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void testMatchingSession() {
    KeyExchange.KeyPair clientKeyPair = KeyExchange.KeyPair.random();
    KeyExchange.KeyPair serverKeyPair = KeyExchange.KeyPair.random();
    KeyExchange.SessionKeyPair clientSessionKeyPair =
        KeyExchange.client(clientKeyPair, serverKeyPair.publicKey());
    KeyExchange.SessionKeyPair serverSessionKeyPair =
        KeyExchange.server(serverKeyPair, clientKeyPair.publicKey());

    assertEquals(clientSessionKeyPair.rx().bytes(), serverSessionKeyPair.tx().bytes());
    assertEquals(clientSessionKeyPair.tx().bytes(), serverSessionKeyPair.rx().bytes());
  }

  @Test
  void testEquals() {
    KeyExchange.KeyPair keyPair = KeyExchange.KeyPair.random();
    KeyExchange.KeyPair keyPair2 = KeyExchange.KeyPair.forSecretKey(keyPair.secretKey());
    assertEquals(keyPair, keyPair2);
    assertEquals(keyPair.hashCode(), keyPair2.hashCode());
  }

  @Test
  void testEqualsSecretKey() {
    KeyExchange.KeyPair keyPair = KeyExchange.KeyPair.random();
    KeyExchange.KeyPair keyPair2 = KeyExchange.KeyPair.forSecretKey(keyPair.secretKey());
    assertEquals(keyPair.secretKey(), keyPair2.secretKey());
    assertEquals(keyPair.hashCode(), keyPair2.hashCode());
  }

  @Test
  void testEqualsPublicKey() {
    KeyExchange.KeyPair keyPair = KeyExchange.KeyPair.random();
    KeyExchange.KeyPair keyPair2 = KeyExchange.KeyPair.forSecretKey(keyPair.secretKey());
    assertEquals(keyPair.publicKey(), keyPair2.publicKey());
    assertEquals(keyPair.hashCode(), keyPair2.hashCode());
  }

  @Test
  void testDestroy() {
    KeyExchange.KeyPair keyPair = KeyExchange.KeyPair.random();
    keyPair.secretKey().destroy();
    assertTrue(keyPair.secretKey().isDestroyed());
  }
}
