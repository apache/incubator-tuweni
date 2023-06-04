// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth;

import static org.apache.tuweni.crypto.Hash.keccak256;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.ethereum.Gas;
import org.apache.tuweni.units.ethereum.Wei;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class TransactionTest {

  static Transaction generateTransaction() {
    return generateTransaction(SECP256K1.KeyPair.random());
  }

  static Transaction generateTransaction(SECP256K1.KeyPair keyPair) {
    return new Transaction(
        UInt256.valueOf(0),
        Wei.valueOf(BigInteger.valueOf(5L)),
        Gas.valueOf(10L),
        Address.fromBytes(Bytes.fromHexString("0x0102030405060708091011121314151617181920")),
        Wei.valueOf(10L),
        Bytes.of(1, 2, 3, 4),
        keyPair);
  }

  @Test
  void testRLPRoundTrip() {
    Transaction tx = generateTransaction();
    Bytes encoded = tx.toBytes();
    Transaction read = Transaction.fromBytes(encoded);
    assertEquals(tx, read);
  }

  @Test
  void shouldGetSenderFromSignature() {
    SECP256K1.KeyPair keyPair = SECP256K1.KeyPair.random();
    Address sender =
        Address.fromBytes(Bytes.wrap(keccak256(keyPair.publicKey().bytesArray()), 12, 20));
    Transaction tx = generateTransaction(keyPair);
    assertEquals(sender, tx.getSender());
  }

  @Test
  void supportVMoreThanOneByte() {
    Transaction tx =
        new Transaction(
            UInt256.valueOf(0),
            Wei.valueOf(BigInteger.valueOf(5L)),
            Gas.valueOf(10L),
            Address.fromBytes(Bytes.fromHexString("0x0102030405060708091011121314151617181920")),
            Wei.valueOf(10L),
            Bytes.of(1, 2, 3, 4),
            SECP256K1.KeyPair.random(),
            16 * 16 * 3);
    Bytes bytes = tx.toBytes();
    Transaction read = Transaction.fromBytes(bytes);
    assertEquals(16 * 16 * 3, (int) read.getChainId());
  }
}
