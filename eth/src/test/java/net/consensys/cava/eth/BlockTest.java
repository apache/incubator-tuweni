/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.eth;

import static net.consensys.cava.eth.BlockHeaderTest.generateBlockHeader;
import static net.consensys.cava.eth.TransactionTest.generateTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.junit.BouncyCastleExtension;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class BlockTest {

  @Test
  void testRoundtripRLP() {
    Block block = new Block(
        generateBlockHeader(),
        new BlockBody(
            Arrays.asList(generateTransaction(), generateTransaction(), generateTransaction(), generateTransaction()),
            Arrays.asList(
                generateBlockHeader(),
                generateBlockHeader(),
                generateBlockHeader(),
                generateBlockHeader(),
                generateBlockHeader(),
                generateBlockHeader())));
    Bytes encoded = block.toBytes();
    Block read = Block.fromBytes(encoded);
    assertEquals(block, read);
  }
}
