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
