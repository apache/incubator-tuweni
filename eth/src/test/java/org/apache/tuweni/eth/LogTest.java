// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.rlp.RLP;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class LogTest {

  @Test
  void testRLProundtrip() {
    Log log =
        new Log(
            Address.fromBytes(Bytes.random(20)),
            Bytes.of(1, 2, 3),
            Arrays.asList(Bytes32.random(), Bytes32.random()));
    Bytes rlp = RLP.encode(log::writeTo);
    Log read = RLP.decode(rlp, Log::readFrom);
    assertEquals(log, read);
  }
}
