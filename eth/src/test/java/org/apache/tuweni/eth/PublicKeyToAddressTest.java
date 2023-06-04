// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.junit.BouncyCastleExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class PublicKeyToAddressTest {

  @Test
  public void testTransformFromSecretKeyToAddress() {
    SECP256K1.SecretKey key = SECP256K1.SecretKey
        .fromBytes(Bytes32.fromHexString("4ee50b74f1f903a80df52c4f6b43a17bc1319636e203f3fe9c09294f74907849"));
    SECP256K1.PublicKey pk = SECP256K1.KeyPair.fromSecretKey(key).publicKey();
    Address addr = Address.fromPublicKeyBytes(pk.bytes());
    assertEquals(Address.fromHexString("0x25851ab5f8151a68d0014fd508609bbf6b4d6d1d"), addr);
  }
}
