// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth;

import static org.apache.tuweni.eth.BlockHeaderTest.generateBlockHeader;
import static org.apache.tuweni.eth.TransactionTest.generateTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.junit.BouncyCastleExtension;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class BlockBodyTest {

  @Test
  void testRLPRoundtrip() {
    BlockBody blockBody = new BlockBody(
        Arrays.asList(generateTransaction(), generateTransaction(), generateTransaction(), generateTransaction()),
        Arrays
            .asList(
                generateBlockHeader(),
                generateBlockHeader(),
                generateBlockHeader(),
                generateBlockHeader(),
                generateBlockHeader(),
                generateBlockHeader()));
    Bytes encoded = blockBody.toBytes();
    BlockBody read = BlockBody.fromBytes(encoded);
    assertEquals(blockBody, read);
  }

}
