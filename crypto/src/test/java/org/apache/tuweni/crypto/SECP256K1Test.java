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
package org.apache.tuweni.crypto;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.tuweni.bytes.Bytes.fromHexString;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.SECP256K1.KeyPair;
import org.apache.tuweni.crypto.SECP256K1.PublicKey;
import org.apache.tuweni.crypto.SECP256K1.SecretKey;
import org.apache.tuweni.crypto.SECP256K1.Signature;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
@ExtendWith(BouncyCastleExtension.class)
class SECP256K1Test {

  @Test
  void testCreatePrivateKey_NullEncoding() {
    assertThrows(NullPointerException.class, () -> SecretKey.fromBytes(null));
  }

  @Test
  void testPrivateKeyEquals() {
    SecretKey secretKey1 = SecretKey.fromInteger(BigInteger.TEN);
    SecretKey secretKey2 = SecretKey.fromInteger(BigInteger.TEN);
    assertEquals(secretKey1, secretKey2);
  }

  @Test
  void testPrivateHashCode() {
    SecretKey secretKey = SecretKey.fromInteger(BigInteger.TEN);
    assertNotEquals(0, secretKey.hashCode());
  }

  @Test
  void testCreatePublicKey_NullEncoding() {
    assertThrows(NullPointerException.class, () -> SECP256K1.PublicKey.fromBytes(null));
  }

  @Test
  void testCreatePublicKey_EncodingTooShort() {
    assertThrows(IllegalArgumentException.class, () -> SECP256K1.PublicKey.fromBytes(Bytes.wrap(new byte[63])));
  }

  @Test
  void testCreatePublicKey_EncodingTooLong() {
    assertThrows(IllegalArgumentException.class, () -> SECP256K1.PublicKey.fromBytes(Bytes.wrap(new byte[65])));
  }

  @Test
  void testPublicKeyEquals() {
    SECP256K1.PublicKey publicKey1 = SECP256K1.PublicKey
        .fromBytes(
            fromHexString(
                "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7"));
    SECP256K1.PublicKey publicKey2 = SECP256K1.PublicKey
        .fromBytes(
            fromHexString(
                "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7"));
    assertEquals(publicKey1, publicKey2);
  }

  @Test
  void testPublicHashCode() {
    SECP256K1.PublicKey publicKey = SECP256K1.PublicKey
        .fromBytes(
            fromHexString(
                "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7"));

    assertNotEquals(0, publicKey.hashCode());
  }

  @Test
  void testCreateKeyPair_PublicKeyNull() {
    assertThrows(
        NullPointerException.class,
        () -> SECP256K1.KeyPair.create(null, SECP256K1.PublicKey.fromBytes(Bytes.wrap(new byte[64]))));
  }

  @Test
  void testCreateKeyPair_PrivateKeyNull() {
    assertThrows(
        NullPointerException.class,
        () -> SECP256K1.KeyPair.create(SecretKey.fromBytes(Bytes32.wrap(new byte[32])), null));
  }

  @Test
  void testKeyPairGeneration() {
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.random();
    assertNotNull(keyPair);
    assertNotNull(keyPair.secretKey());
    assertNotNull(keyPair.publicKey());
  }

  @Test
  void testKeyPairEquals() {
    SecretKey secretKey1 = SecretKey.fromInteger(BigInteger.TEN);
    SecretKey secretKey2 = SecretKey.fromInteger(BigInteger.TEN);
    SECP256K1.PublicKey publicKey1 = SECP256K1.PublicKey
        .fromBytes(
            fromHexString(
                "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7"));
    SECP256K1.PublicKey publicKey2 = SECP256K1.PublicKey
        .fromBytes(
            fromHexString(
                "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7"));

    SECP256K1.KeyPair keyPair1 = SECP256K1.KeyPair.create(secretKey1, publicKey1);
    SECP256K1.KeyPair keyPair2 = SECP256K1.KeyPair.create(secretKey2, publicKey2);

    assertEquals(keyPair1, keyPair2);
  }

  @Test
  void testKeyPairHashCode() {
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.random();
    assertNotEquals(0, keyPair.hashCode());
  }

  @Test
  void testKeyPairGeneration_PublicKeyRecovery() {
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.random();
    assertEquals(keyPair.publicKey(), SECP256K1.PublicKey.fromSecretKey(keyPair.secretKey()));
  }

  @Test
  void testPublicKeyRecovery() {
    SecretKey secretKey = SecretKey.fromInteger(BigInteger.TEN);
    SECP256K1.PublicKey expectedPublicKey = SECP256K1.PublicKey
        .fromBytes(
            fromHexString(
                "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7"));

    SECP256K1.PublicKey publicKey = SECP256K1.PublicKey.fromSecretKey(secretKey);
    assertEquals(expectedPublicKey, publicKey);
  }

  @Test
  void testCreateSignature() {
    SECP256K1.Signature signature = new SECP256K1.Signature((byte) 0, BigInteger.ONE, BigInteger.TEN);
    assertEquals(BigInteger.ONE, signature.r());
    assertEquals(BigInteger.TEN, signature.s());
    assertEquals((byte) 0, signature.v());
  }

  @Test
  void testEncodeSignature() {
    SECP256K1.Signature signature = new SECP256K1.Signature((byte) 0, BigInteger.ONE, BigInteger.TEN);
    assertEquals(
        "0x0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000a00",
        signature.bytes().toString());
  }

  @Test
  void testCreateSignatureFromEncoding() {
    SECP256K1.Signature signature = SECP256K1.Signature
        .fromBytes(
            fromHexString(
                "0x0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000A01"));
    assertEquals(BigInteger.ONE, signature.r());
    assertEquals(BigInteger.TEN, signature.s());
    assertEquals((byte) 1, signature.v());
  }

  @Test
  void testCreateSignatureWithNullR() {
    assertThrows(NullPointerException.class, () -> SECP256K1.Signature.create((byte) 1, null, BigInteger.ONE));
  }

  @Test
  void testCreateSignatureWithNullS() {
    assertThrows(NullPointerException.class, () -> SECP256K1.Signature.create((byte) 1, BigInteger.ONE, null));
  }

  @Test
  void testCreateSignatureWithZeroR() {
    Exception throwable = assertThrows(
        IllegalArgumentException.class,
        () -> SECP256K1.Signature.create((byte) 1, BigInteger.ZERO, BigInteger.ONE));
    assertEquals(
        "Invalid r-value, should be >= 1 and < " + SECP256K1.Parameters.CURVE.getN() + ", got 0",
        throwable.getMessage());
  }

  @Test
  void testCreateSignatureWithZeroS() {
    Exception throwable = assertThrows(
        IllegalArgumentException.class,
        () -> SECP256K1.Signature.create((byte) 1, BigInteger.ONE, BigInteger.ZERO));
    assertEquals(
        "Invalid s-value, should be >= 1 and < " + SECP256K1.Parameters.CURVE.getN() + ", got 0",
        throwable.getMessage());
  }

  @Test
  void testCreateSignatureWithRHigherThanCurve() {
    BigInteger curveN = SECP256K1.Parameters.CURVE.getN();
    Exception throwable = assertThrows(
        IllegalArgumentException.class,
        () -> SECP256K1.Signature.create((byte) 1, curveN.add(BigInteger.ONE), BigInteger.ONE));
    assertEquals(
        "Invalid r-value, should be >= 1 and < " + curveN + ", got " + curveN.add(BigInteger.ONE),
        throwable.getMessage());
  }

  @Test
  void testCreateSignatureWithSHigherThanCurve() {
    BigInteger curveN = SECP256K1.Parameters.CURVE.getN();
    Exception throwable = assertThrows(
        IllegalArgumentException.class,
        () -> SECP256K1.Signature.create((byte) 1, BigInteger.ONE, curveN.add(BigInteger.ONE)));
    assertEquals(
        "Invalid s-value, should be >= 1 and < " + curveN + ", got " + curveN.add(BigInteger.ONE),
        throwable.getMessage());
  }

  @Test
  void testRecoverPublicKeyFromSignature() {
    SecretKey secretKey =
        SecretKey.fromInteger(new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16));
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.fromSecretKey(secretKey);

    long seed = new Random().nextLong();
    Random random = new Random(seed);
    for (int i = 0; i < 100; ++i) {
      try {
        byte[] data = new byte[20];
        random.nextBytes(data);
        SECP256K1.Signature signature = SECP256K1.sign(data, keyPair);

        PublicKey recoveredPublicKey = SECP256K1.PublicKey.recoverFromSignature(data, signature);
        assertNotNull(recoveredPublicKey);
        assertEquals(keyPair.publicKey().toString(), recoveredPublicKey.toString());
        assertTrue(SECP256K1.verify(data, signature, recoveredPublicKey));
      } catch (AssertionError e) {
        System.err.println("Random seed: " + seed);
        throw e;
      }
    }

    Bytes32 hash = Bytes32.fromHexString("ACB1C19AC0832320815B5E886C6B73AD7D6177853D44B026F2A7A9E11BB899FC");
    SECP256K1.Signature signature = SECP256K1.Signature
        .create(
            (byte) 1,
            new BigInteger("62380806879052346173879701944100777919767605075819957043497305774369260714318"),
            new BigInteger("38020116821208196490118623452490256423459205241616519723877133146103446128360"));
    assertNull(SECP256K1.PublicKey.recoverFromHashAndSignature(hash, signature));
  }

  @Test
  void testCannotRecoverPublicKeyFromSignature() {
    SECP256K1.Signature signature =
        new Signature((byte) 0, SECP256K1.Parameters.CURVE_ORDER.subtract(BigInteger.ONE), BigInteger.valueOf(10));
    assertNull(SECP256K1.PublicKey.recoverFromSignature(Bytes.of("Random data".getBytes(UTF_8)), signature));
  }

  @Test
  void testSignatureGeneration() {
    SecretKey secretKey =
        SecretKey.fromInteger(new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16));
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.fromSecretKey(secretKey);

    Bytes data = Bytes.wrap("This is an example of a signed message.".getBytes(UTF_8));
    SECP256K1.Signature expectedSignature = new SECP256K1.Signature(
        (byte) 1,
        new BigInteger("d2ce488f4da29e68f22cb05cac1b19b75df170a12b4ad1bdd4531b8e9115c6fb", 16),
        new BigInteger("75c1fe50a95e8ccffcbb5482a1e42fbbdd6324131dfe75c3b3b7f9a7c721eccb", 16));

    SECP256K1.Signature actualSignature = SECP256K1.sign(data, keyPair);
    assertEquals(expectedSignature, actualSignature);
  }

  @Test
  void testSignatureVerification() {
    SecretKey secretKey =
        SecretKey.fromInteger(new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16));
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.fromSecretKey(secretKey);

    Bytes data = Bytes.wrap("This is an example of a signed message.".getBytes(UTF_8));

    SECP256K1.Signature signature = SECP256K1.sign(data, keyPair);
    assertTrue(SECP256K1.verify(data, signature, keyPair.publicKey()));
  }

  @Test
  void testFileContainsValidPrivateKey(@TempDirectory Path tempDir) throws Exception {
    Path tempFile = tempDir.resolve("tempId");
    Files.write(tempFile, "000000000000000000000000000000000000000000000000000000000000000A".getBytes(UTF_8));
    SecretKey secretKey = SecretKey.load(tempFile);
    assertEquals(fromHexString("000000000000000000000000000000000000000000000000000000000000000A"), secretKey.bytes());
  }

  @Test
  void testReadWritePrivateKeyString(@TempDirectory Path tempDir) throws Exception {
    SecretKey secretKey = SecretKey.fromInteger(BigInteger.TEN);
    SECP256K1.KeyPair keyPair1 = SECP256K1.KeyPair.fromSecretKey(secretKey);
    Path tempFile = tempDir.resolve("tempId");
    keyPair1.store(tempFile);
    SECP256K1.KeyPair keyPair2 = SECP256K1.KeyPair.load(tempFile);
    assertEquals(keyPair1, keyPair2);
  }

  @Test
  void testInvalidFileThrowsInvalidKeyPairException(@TempDirectory Path tempDir) throws Exception {
    Path tempFile = tempDir.resolve("tempId");
    Files.write(tempFile, "not valid".getBytes(UTF_8));
    assertThrows(InvalidSEC256K1SecretKeyStoreException.class, () -> SecretKey.load(tempFile));
  }

  @Test
  void testInvalidMultiLineFileThrowsInvalidIdException(@TempDirectory Path tempDir) throws Exception {
    Path tempFile = tempDir.resolve("tempId");
    Files.write(tempFile, "not\n\nvalid".getBytes(UTF_8));
    assertThrows(InvalidSEC256K1SecretKeyStoreException.class, () -> SecretKey.load(tempFile));
  }

  @Test
  void testEncodedBytes() {
    KeyPair kp = SECP256K1.KeyPair.random();
    Signature sig = SECP256K1.sign(Bytes.of(1, 2, 3), kp);
    assertEquals(65, sig.bytes().size());
    assertTrue(sig.bytes().get(64) <= 3 && sig.bytes().get(64) >= 0);
  }

  @Test
  void testSharedSecretBytes() {
    KeyPair kp = SECP256K1.KeyPair.random();
    KeyPair otherKP = SECP256K1.KeyPair.random();
    Bytes32 sharedSecret = SECP256K1.calculateKeyAgreement(kp.secretKey(), otherKP.publicKey());
    Bytes32 otherSharedSecret = SECP256K1.calculateKeyAgreement(otherKP.secretKey(), kp.publicKey());
    assertEquals(sharedSecret, otherSharedSecret);
  }

  @Test
  void encryptDecrypt() {
    KeyPair kp = SECP256K1.KeyPair.random();
    Bytes encrypted = SECP256K1.encrypt(kp.publicKey(), Bytes.fromHexString("0xdeadbeef"));
    Bytes decrypted = SECP256K1.decrypt(kp.secretKey(), encrypted);
    assertEquals(Bytes.fromHexString("0xdeadbeef"), decrypted);
  }
}
