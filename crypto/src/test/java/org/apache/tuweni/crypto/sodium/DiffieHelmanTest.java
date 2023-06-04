// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

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

    DiffieHelman.Secret scalar1 =
        DiffieHelman.Secret.forKeys(keyPair.secretKey(), secondKeyPair.publicKey());
    DiffieHelman.Secret scalar2 =
        DiffieHelman.Secret.forKeys(secondKeyPair.secretKey(), keyPair.publicKey());

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

  @Test
  void testDestroy() {
    DiffieHelman.KeyPair keyPair = DiffieHelman.KeyPair.random();
    keyPair.secretKey().destroy();
    assertTrue(keyPair.secretKey().isDestroyed());
  }

  @Test
  void testFromBoxPubKey() {
    Bytes bytes = Bytes32.random();
    Box.PublicKey pkey = Box.PublicKey.fromBytes(bytes);
    DiffieHelman.PublicKey dpk = DiffieHelman.PublicKey.forBoxPublicKey(pkey);
    assertEquals(bytes, dpk.bytes());
    assertArrayEquals(bytes.toArrayUnsafe(), dpk.bytesArray());
  }

  @Test
  void testEqualsPublicKeyFromBytes() {
    Bytes bytes = Bytes32.random();
    DiffieHelman.PublicKey pkey = DiffieHelman.PublicKey.fromBytes(bytes);
    DiffieHelman.PublicKey pkey2 = DiffieHelman.PublicKey.fromBytes(bytes);
    assertEquals(pkey, pkey2);
    assertEquals(pkey.hashCode(), pkey2.hashCode());
  }

  @Test
  void testInvalidBytes() {
    Bytes bytes = Bytes.random(20);
    assertThrows(IllegalArgumentException.class, () -> DiffieHelman.PublicKey.fromBytes(bytes));
  }

  @Test
  void testInvalidBytesSecretKey() {
    Bytes bytes = Bytes.random(20);
    assertThrows(IllegalArgumentException.class, () -> DiffieHelman.SecretKey.fromBytes(bytes));
  }
}
