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
package org.apache.tuweni.crypto.mikuli;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;


class SignatureTest {

  @Test
  void testNoSigs() {
    assertThrows(IllegalArgumentException.class, () -> {
      Signature.aggregate(Collections.emptyList());
    });
  }

  @Test
  void testNoSigsAndPubKeys() {
    assertThrows(IllegalArgumentException.class, () -> {
      SignatureAndPublicKey.aggregate(Collections.emptyList());
    });
  }

  @Test
  void testSimpleSignature() {
    KeyPair keyPair = KeyPair.random();
    byte[] message = "Hello".getBytes(UTF_8);
    SignatureAndPublicKey sigAndPubKey = BLS12381.sign(keyPair, message, 48);

    Boolean isValid = BLS12381.verify(sigAndPubKey.publicKey(), sigAndPubKey.signature(), message, 48);
    assertTrue(isValid);
  }

  @Test
  void testAggregatedSignature() {
    byte[] message = "Hello".getBytes(UTF_8);
    List<SignatureAndPublicKey> sigs = getSignaturesAndPublicKeys(message);
    SignatureAndPublicKey sigAndPubKey = SignatureAndPublicKey.aggregate(sigs);

    Boolean isValid = BLS12381.verify(sigAndPubKey, message, 48);
    assertTrue(isValid);
  }

  @Test
  void testCorruptedMessage() {
    byte[] message = "Hello".getBytes(UTF_8);
    List<SignatureAndPublicKey> sigs = getSignaturesAndPublicKeys(message);
    SignatureAndPublicKey sigAndPubKey = SignatureAndPublicKey.aggregate(sigs);
    byte[] corruptedMessage = "Not Hello".getBytes(UTF_8);

    Boolean isValid = BLS12381.verify(sigAndPubKey, corruptedMessage, 48);
    assertFalse(isValid);
  }

  @Test
  void testCorruptedSignature() {
    byte[] message = "Hello".getBytes(UTF_8);
    List<SignatureAndPublicKey> sigs = getSignaturesAndPublicKeys(message);
    KeyPair keyPair = KeyPair.random();
    byte[] notHello = "Not Hello".getBytes(UTF_8);

    SignatureAndPublicKey additionalSignature = BLS12381.sign(keyPair, notHello, 48);
    sigs.add(additionalSignature);

    SignatureAndPublicKey sigAndPubKey = SignatureAndPublicKey.aggregate(sigs);

    Boolean isValid = BLS12381.verify(sigAndPubKey, message, 48);
    assertFalse(isValid);
  }

  @Test
  void testCorruptedSignatureWithoutPubKeys() {
    byte[] message = "Hello".getBytes(UTF_8);
    List<Signature> sigs = getSignatures(message);
    KeyPair keyPair = KeyPair.random();
    byte[] notHello = "Not Hello".getBytes(UTF_8);

    SignatureAndPublicKey additionalSignature = BLS12381.sign(keyPair, notHello, 48);
    sigs.add(additionalSignature.signature());

    Signature sig = Signature.aggregate(sigs);

    Boolean isValid = BLS12381.verify(additionalSignature.publicKey(), sig, message, 48);
    assertFalse(isValid);
  }

  @Test
  void testSerialization() {
    KeyPair keyPair = KeyPair.random();
    byte[] message = "Hello".getBytes(UTF_8);
    Signature signature = BLS12381.sign(keyPair, message, 48).signature();

    Bytes sigTobytes = signature.encode();
    Signature sigFromBytes = Signature.decode(sigTobytes);

    assertEquals(signature, sigFromBytes);
    assertEquals(signature.hashCode(), sigFromBytes.hashCode());

    PublicKey pubKey = keyPair.publicKey();
    byte[] pubKeyTobytes = pubKey.toByteArray();
    PublicKey pubKeyFromBytes = PublicKey.fromBytes(pubKeyTobytes);

    assertEquals(pubKey, pubKeyFromBytes);
    assertEquals(pubKey.hashCode(), pubKeyFromBytes.hashCode());
  }

  List<Signature> getSignatures(byte[] message) {
    KeyPair keyPair1 = KeyPair.random();
    KeyPair keyPair2 = KeyPair.random();
    KeyPair keyPair3 = KeyPair.random();

    Signature sigAndPubKey1 = BLS12381.sign(keyPair1, message, 48).signature();
    Signature sigAndPubKey2 = BLS12381.sign(keyPair2, message, 48).signature();
    Signature sigAndPubKey3 = BLS12381.sign(keyPair3, message, 48).signature();

    List<Signature> sigs = new ArrayList<>();
    sigs.add(sigAndPubKey1);
    sigs.add(sigAndPubKey2);
    sigs.add(sigAndPubKey3);

    return sigs;
  }

  List<SignatureAndPublicKey> getSignaturesAndPublicKeys(byte[] message) {
    KeyPair keyPair1 = KeyPair.random();
    KeyPair keyPair2 = KeyPair.random();
    KeyPair keyPair3 = KeyPair.random();

    SignatureAndPublicKey sigAndPubKey1 = BLS12381.sign(keyPair1, message, 48);
    SignatureAndPublicKey sigAndPubKey2 = BLS12381.sign(keyPair2, message, 48);
    SignatureAndPublicKey sigAndPubKey3 = BLS12381.sign(keyPair3, message, 48);

    List<SignatureAndPublicKey> sigs = new ArrayList<SignatureAndPublicKey>();
    sigs.add(sigAndPubKey1);
    sigs.add(sigAndPubKey2);
    sigs.add(sigAndPubKey3);

    return sigs;
  }

  @Test
  void secretKeyRoundtrip() {
    KeyPair kp = KeyPair.random();
    SecretKey key = kp.secretKey();
    Bytes bytes = key.toBytes();
    assertEquals(key, SecretKey.fromBytes(bytes));
  }
}
