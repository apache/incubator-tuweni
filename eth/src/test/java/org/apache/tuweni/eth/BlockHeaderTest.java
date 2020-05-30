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
