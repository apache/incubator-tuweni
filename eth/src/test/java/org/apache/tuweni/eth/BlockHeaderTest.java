// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.bigints.UInt64;
import org.apache.tuweni.units.ethereum.Gas;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class BlockHeaderTest {

  static BlockHeader generateBlockHeader() {
    return new BlockHeader(
        Hash.fromBytes(Bytes.random(32)),
        Hash.fromBytes(Bytes.random(32)),
        Address.fromBytes(Bytes.fromHexString("0x0102030405060708091011121314151617181920")),
        Hash.fromBytes(Bytes.random(32)),
        Hash.fromBytes(Bytes.random(32)),
        Hash.fromBytes(Bytes.random(32)),
        Bytes.random(8),
        UInt256.fromBytes(Bytes.random(32)),
        UInt256.fromBytes(Bytes.random(32)),
        Gas.valueOf(UInt256.fromBytes(Bytes.random(6))),
        Gas.valueOf(UInt256.fromBytes(Bytes.random(6))),
        Instant.now().truncatedTo(ChronoUnit.SECONDS),
        Bytes.random(22),
        Hash.fromBytes(Bytes.random(32)),
        UInt64.ONE);
  }

  @Test
  void rlpRoundtrip() {
    BlockHeader blockHeader = generateBlockHeader();
    BlockHeader read = BlockHeader.fromBytes(blockHeader.toBytes());
    assertEquals(blockHeader, read);
  }
}
